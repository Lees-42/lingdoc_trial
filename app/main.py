# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - FastAPI Application Entry
=============================================================================
用途：FastAPI 应用主入口，注册路由、中间件、异常处理

启动方式：
  开发模式：uvicorn app.main:app --reload --port 8000
  生产模式：uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4

文档地址：
  Swagger UI: http://localhost:8000/docs
  ReDoc:      http://localhost:8000/redoc

服务依赖：
  - PaddleOCR 模型（首次启动自动下载，约 100MB）
  - DashScope API Key（必须配置）
  - 共享上传目录（和主后端挂载同一个 Volume）

对接说明（给主项目工程师）：
  - 主后端通过 HTTP 调用 AI 服务，Base URL 为 http://lingdoc-ai-service:8000
  - 所有业务接口在 /api/ai/v1/ 前缀下
  - 认证通过 X-Internal-Token Header 实现
=============================================================================
"""

import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import config, validate_config
from app.utils.logger import logger, setup_logger
from app.routers import doc as doc_router
from app.routers import form as form_router


# =============================================================================
# 生命周期管理（启动/关闭）
# =============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理
    
    启动时：
      1. 校验配置（检查必填项是否缺失）
      2. 初始化日志级别
      3. 预热 OCR 引擎（可选）
    
    关闭时：
      1. 清理资源
      2. 关闭 HTTP 客户端
    """
    # --- 启动阶段 ---
    logger.info("=" * 60)
    logger.info("LingDoc AI Service 启动中...")
    logger.info("=" * 60)
    
    # 校验配置
    try:
        validate_config()
        logger.info("✅ 配置校验通过")
    except ValueError as e:
        logger.error(f"❌ 配置校验失败: {e}")
        raise  # 配置错误时阻止启动
    
    # 设置日志级别
    log_level = config.LOG_LEVEL
    setup_logger(level=log_level)
    logger.info(f"日志级别: {log_level}")
    
    # 预热 OCR 引擎（可选，注释掉以加快启动）
    # 说明：预热可以在启动时加载模型，避免第一次请求卡顿 3-5 秒
    # 代价：启动时间增加 3-5 秒，内存占用增加约 500MB
    # try:
    #     from app.services.ocr_engine import OcrEngine
    #     engine = OcrEngine()
    #     logger.info("✅ OCR 引擎预热完成")
    # except Exception as e:
    #     logger.warning(f"⚠️ OCR 引擎预热失败（不影响启动）: {e}")
    
    logger.info(f"🚀 服务启动完成 | 监听: {config.SERVICE_HOST}:{config.SERVICE_PORT}")
    logger.info(f"📖 API 文档: http://{config.SERVICE_HOST}:{config.SERVICE_PORT}/docs")
    
    yield  # 应用运行期间
    
    # --- 关闭阶段 ---
    logger.info("🛑 服务关闭中...")
    # 清理 HTTP 客户端
    try:
        from app.services.llm_client import LLMClient
        client = LLMClient()
        await client.client.aclose()
        logger.info("✅ HTTP 客户端已关闭")
    except Exception:
        pass
    
    logger.info("👋 服务已安全关闭")


# =============================================================================
# 创建 FastAPI 应用实例
# =============================================================================

app = FastAPI(
    title="LingDoc AI Service",
    description="""
    灵档 AI 服务 - 文档智能处理 API
    
    提供文档 OCR 识别、AI 分析、自动标签生成等能力。
    主后端通过 HTTP + X-Internal-Token 调用此服务。
    
    核心接口：
    - POST /api/ai/v1/doc/process - 文档处理（OCR + AI分析 + 标签）
    - GET  /api/ai/v1/health      - 健康检查
    
    认证方式：
    - Header: X-Internal-Token: {AI_INTERNAL_TOKEN}
    - 所有 /api/ai/v1/* 接口都需要此 Header
    - /health 健康检查不需要认证
    """,
    version="1.0.0",
    docs_url="/docs",       # Swagger UI 路径
    redoc_url="/redoc",     # ReDoc 路径
    openapi_url="/openapi.json",
    lifespan=lifespan,
)

# =============================================================================
# 中间件注册
# =============================================================================

# 1. CORS（跨域支持，开发环境需要）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制为具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2. 请求日志中间件
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """
    记录所有 HTTP 请求的耗时和状态
    
    说明：
      - 跳过 /health 健康检查（避免日志噪音）
      - 记录请求方法、路径、耗时、状态码
    """
    if request.url.path == "/health":
        return await call_next(request)
    
    start = time.time()
    response = await call_next(request)
    duration = time.time() - start
    
    logger.info(
        f"{request.method} {request.url.path} | "
        f"status={response.status_code} | duration={duration*1000:.1f}ms"
    )
    
    return response

# 3. 异常处理中间件
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """
    全局异常捕获
    
    说明：
      - 捕获所有未处理的异常，返回统一格式的错误响应
      - 避免暴露内部堆栈给客户端（生产环境）
      - 详细错误记录到日志
    """
    logger.error(
        f"未处理异常: {request.method} {request.url.path} | {type(exc).__name__}: {exc}",
        exc_info=True
    )
    
    return JSONResponse(
        status_code=500,
        content={
            "code": 500,
            "msg": "AI 服务内部错误",
            "data": None,
            "trace": {
                "error_type": type(exc).__name__,
                "error": str(exc) if config.LOG_LEVEL == "DEBUG" else "详见服务日志"
            }
        }
    )


# =============================================================================
# 路由注册
# =============================================================================

# 业务路由（所有 /api/ai/v1/* 接口）
app.include_router(doc_router.router)
app.include_router(form_router.router)


# =============================================================================
# 根路径（方便浏览器直接访问确认服务状态）
# =============================================================================

@app.get("/", tags=["Root"])
async def root():
    """
    根路径 - 返回服务基本信息
    
    用途：方便浏览器直接访问 http://localhost:8000 确认服务运行
    """
    return {
        "service": "LingDoc AI Service",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
        "health": "/api/ai/v1/health"
    }


# =============================================================================
# 启动脚本入口（python app/main.py）
# =============================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=config.SERVICE_HOST,
        port=config.SERVICE_PORT,
        reload=True,  # 开发模式自动重载
        log_level=config.LOG_LEVEL.lower(),
    )
