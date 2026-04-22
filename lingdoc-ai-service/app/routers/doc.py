# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Document Processing Router
=============================================================================
API 路径: /api/ai/v1/doc/*
用途：提供文档上传后处理的完整链路（OCR → AI分析 → 标签生成）

接口清单：
  POST /api/ai/v1/doc/process     - 文档处理（核心接口）
  GET  /api/ai/v1/doc/status/{task_id} - 查询处理状态
  POST /api/ai/v1/doc/qa        - 文档问答（预留）
  POST /api/ai/v1/table/fill    - 表格填写（预留）

对接说明（给主项目工程师）：
  - 所有 /api/ai/v1/* 接口都需要 X-Internal-Token 认证
  - /health 健康检查不需要认证（方便负载均衡/监控）
  - 请求体使用 JSON，响应体使用统一格式 {code, msg, data, trace}

处理流程：
  主后端上传文件 → 保存到共享目录 → 调用 /api/ai/v1/doc/process
                                            ↓
                                   1. OCR 提取文本
                                   2. LLM 分析内容（摘要/关键词/分类）
                                   3. 自动生成标签
                                   4. 返回完整元数据
=============================================================================
"""

import os
import hashlib
import time
import uuid
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import JSONResponse

from app.middleware.auth import internal_auth
from app.models.schemas import (
    DocProcessRequest, DocProcessResponse, DocProcessResult,
    DocQARequest, DocQAResponse,
    TableFillRequest, TableFillResponse,
    AiParseStatus, SourceType, FileType, TraceInfo,
    BaseResponse
)
from app.services.ocr_engine import ocr_process
from app.services.llm_client import analyze_document
from app.services.tag_generator import generate_tags
from app.config import config
from app.utils.logger import logger, log_request, log_response

# 创建路由实例
router = APIRouter(
    prefix="/api/ai/v1",
    tags=["AI Document Processing"],
    responses={
        401: {"description": "认证失败，缺少或错误的 X-Internal-Token"},
        403: {"description": "权限不足"},
        500: {"description": "AI 服务内部错误"}
    }
)


# =============================================================================
# 1. 核心接口：文档处理
# =============================================================================

@router.post(
    "/doc/process",
    response_model=DocProcessResponse,
    summary="文档智能处理",
    description="""
    处理上传的文档，完成 OCR 识别、AI 分析、标签生成。
    
    请求体示例：
    ```json
    {
      "task_id": "t_123e4567-e89b-12d3-a456-426614174000",
      "file_path": "/uploads/lingdoc/1/1745376123_report.pdf",
      "user_id": 1,
      "vault_id": "vault_default",
      "options": {
        "enable_ai_analysis": true,
        "enable_tagging": true,
        "model": "qwen3-max"
      }
    }
    ```
    
    响应体示例：
    ```json
    {
      "code": 200,
      "msg": "处理成功",
      "data": {
        "file_id": "vault_default_abc123",
        "file_name": "report.pdf",
        "file_type": "pdf",
        "file_size": 1024000,
        "checksum": "md5:abc123...",
        "ocr_text": "这是提取的完整文本...",
        "ocr_page_count": 5,
        "ai_summary": "这是一份电路实验报告，涵盖了...",
        "ai_keywords": ["电路", "实验报告", "基尔霍夫定律"],
        "auto_tags": [
          {"name": "电路", "color": "#409EFF", "confidence": 0.95, "source": "ai_auto"}
        ],
        "trace": {
          "request_id": "req_xxx",
          "token_usage": 1250,
          "process_time_ms": 3200
        }
      }
    }
    ```
    """
)
async def process_document(
    request: DocProcessRequest,
    token: str = Depends(internal_auth)
) -> DocProcessResponse:
    """
    文档处理主入口
    
    说明：
      - 这是 AI 服务最核心的接口，主后端上传文件后调用此接口
      - 处理流程：OCR 识别 → AI 分析 → 标签生成 → 返回元数据
      - 整个流程是同步的（因为主后端需要立即获得结果）
      - 大文件可能耗时较长，主后端应设置足够的 HTTP 超时
    
    性能参考：
      - 单页 PDF：约 2-5 秒
      - 10 页 PDF：约 10-20 秒
      - Word 文档（直接提取）：约 1-3 秒
    
    对接说明（给主项目工程师）：
      - 调用前确保文件已保存到 AI_UPLOAD_DIR 目录
      - 建议主后端设置 HTTP 超时 ≥ 60 秒
      - 失败时检查 response.trace 获取详细错误信息
    """
    request_id = f"req_{uuid.uuid4().hex[:12]}"
    start_time = time.time()
    
    # 记录请求日志
    log_request(request_id, "POST", "/api/ai/v1/doc/process", "internal")
    logger.info(
        f"[DOC:{request_id}] 开始处理 | task={request.task_id} | "
        f"file={request.file_path}"
    )
    
    try:
        # -------------------------------------------------------------------
        # Step 1: 文件校验
        # -------------------------------------------------------------------
        if not os.path.exists(request.file_path):
            logger.error(f"[DOC:{request_id}] 文件不存在: {request.file_path}")
            return DocProcessResponse(
                code=400,
                msg="文件不存在",
                data=None,
                trace={"request_id": request_id, "error": "file_not_found"}
            )
        
        # 计算文件大小和 checksum
        file_size = os.path.getsize(request.file_path)
        file_name = os.path.basename(request.file_path)
        file_ext = os.path.splitext(file_name)[1].lower()
        
        # MD5 校验（快速）
        md5_hash = hashlib.md5()
        with open(request.file_path, "rb") as f:
            for chunk in iter(lambda: f.read(8192), b""):
                md5_hash.update(chunk)
        checksum = f"md5:{md5_hash.hexdigest()}"
        
        # 映射文件类型
        file_type_map = {
            ".pdf": FileType.PDF,
            ".docx": FileType.DOCX,
            ".doc": FileType.DOC,
            ".png": FileType.PNG,
            ".jpg": FileType.JPG,
            ".jpeg": FileType.JPEG,
        }
        file_type = file_type_map.get(file_ext, FileType.UNKNOWN)
        
        # -------------------------------------------------------------------
        # Step 2: OCR 识别
        # -------------------------------------------------------------------
        logger.info(f"[DOC:{request_id}] Step 2: OCR 识别")
        ocr_start = time.time()
        
        ocr_result = await ocr_process(request.file_path, request.task_id)
        ocr_time_ms = int((time.time() - ocr_start) * 1000)
        
        if not ocr_result["success"]:
            logger.error(f"[DOC:{request_id}] OCR 失败: {ocr_result.get('error')}")
            return DocProcessResponse(
                code=500,
                msg=f"OCR 识别失败: {ocr_result.get('error')}",
                data=None,
                trace={"request_id": request_id, "error": ocr_result.get('error')}
            )
        
        logger.info(
            f"[DOC:{request_id}] OCR 完成 | 页数={ocr_result['page_count']} | "
            f"字符={ocr_result['total_chars']} | 耗时={ocr_time_ms}ms"
        )
        
        # -------------------------------------------------------------------
        # Step 3: AI 分析（可选）
        # -------------------------------------------------------------------
        ai_summary = ""
        ai_keywords = []
        ai_category = ""
        llm_time_ms = 0
        total_token_usage = 0
        
        options = request.options or {}
        enable_ai = options.get("enable_ai_analysis", True)
        
        if enable_ai and ocr_result["total_chars"] > 10:
            logger.info(f"[DOC:{request_id}] Step 3: AI 分析")
            llm_start = time.time()
            
            llm_result = await analyze_document(
                ocr_text=ocr_result["ocr_text"],
                file_name=file_name,
                file_type=file_type.value,
                task_id=request.task_id
            )
            
            llm_time_ms = llm_result.duration_ms
            total_token_usage += llm_result.token_usage
            
            if llm_result.success:
                # 解析 JSON 结果
                import json
                try:
                    ai_data = json.loads(llm_result.content)
                    ai_summary = ai_data.get("summary", "")
                    ai_keywords = ai_data.get("keywords", [])
                    ai_category = ai_data.get("category", "")
                except json.JSONDecodeError:
                    logger.warning(f"[DOC:{request_id}] AI 结果 JSON 解析失败，使用原始文本")
                    ai_summary = llm_result.content[:500]  # 截断到 500 字
            else:
                logger.warning(f"[DOC:{request_id}] AI 分析失败: {llm_result.error}")
        else:
            logger.info(f"[DOC:{request_id}] 跳过 AI 分析（禁用或文本过短）")
        
        # -------------------------------------------------------------------
        # Step 4: 标签生成
        # -------------------------------------------------------------------
        auto_tags = []
        enable_tagging = options.get("enable_tagging", True)
        
        if enable_tagging:
            logger.info(f"[DOC:{request_id}] Step 4: 标签生成")
            
            tags = await generate_tags(
                file_name=file_name,
                ocr_text=ocr_result["ocr_text"],
                source_type="0",  # 手动上传
                task_id=request.task_id
            )
            auto_tags = tags
            
            logger.info(f"[DOC:{request_id}] 标签生成完成 | 数量={len(auto_tags)}")
        
        # -------------------------------------------------------------------
        # Step 5: 组装结果
        # -------------------------------------------------------------------
        total_time_ms = int((time.time() - start_time) * 1000)
        
        # 构建 file_id（全局唯一，格式：{vault_id}_{uuid}）
        file_id = f"{request.vault_id or 'default'}_{uuid.uuid4().hex[:16]}"
        
        result = DocProcessResult(
            file_id=file_id,
            file_name=file_name,
            file_type=file_type,
            file_size=file_size,
            checksum=checksum,
            parent_id=None,  # 根目录
            vault_path=file_name,
            source_type=SourceType.MANUAL_UPLOAD,
            ocr_text=ocr_result["ocr_text"],
            ocr_page_count=ocr_result.get("page_count", 0),
            ocr_total_lines=ocr_result.get("total_lines", 0),
            ocr_total_chars=ocr_result.get("total_chars", 0),
            ocr_language="ch",
            ai_parse_status=AiParseStatus.SUCCESS if ai_summary else AiParseStatus.PARTIAL,
            ai_summary=ai_summary or None,
            ai_keywords=ai_keywords,
            ai_category=ai_category or None,
            auto_tags=auto_tags,
            trace=TraceInfo(
                request_id=request_id,
                token_usage=total_token_usage,
                process_time_ms=total_time_ms,
                ocr_time_ms=ocr_time_ms,
                llm_time_ms=llm_time_ms
            )
        )
        
        log_response(request_id, 200, total_time_ms, total_token_usage)
        
        logger.info(
            f"[DOC:{request_id}] 处理完成 | 总耗时={total_time_ms}ms | "
            f"tokens={total_token_usage}"
        )
        
        return DocProcessResponse(
            code=200,
            msg="处理成功",
            data=result,
            trace={
                "request_id": request_id,
                "token_usage": total_token_usage,
                "process_time_ms": total_time_ms
            }
        )
        
    except Exception as e:
        logger.error(f"[DOC:{request_id}] 处理异常: {str(e)}", exc_info=True)
        log_response(request_id, 500, int((time.time() - start_time) * 1000))
        
        return DocProcessResponse(
            code=500,
            msg=f"处理失败: {str(e)}",
            data=None,
            trace={"request_id": request_id, "error": str(e)}
        )


# =============================================================================
# 2. 预留接口：文档问答
# =============================================================================

@router.post(
    "/doc/qa",
    response_model=DocQAResponse,
    summary="文档问答（预留）",
    description="基于文档内容回答用户提问（需要 RAG 向量检索支持，暂未实现）"
)
async def document_qa(
    request: DocQARequest,
    token: str = Depends(internal_auth)
) -> DocQAResponse:
    """文档问答接口（预留）"""
    return DocQAResponse(
        code=501,
        msg="文档问答功能暂未实现，将在 RAG 模块完成后开放",
        data=None
    )


# =============================================================================
# 3. 预留接口：表格填写
# =============================================================================

@router.post(
    "/table/fill",
    response_model=TableFillResponse,
    summary="表格智能填写（预留）",
    description="根据文档内容自动填写表格模板（需要 Qwen3-Coder 支持，暂未实现）"
)
async def table_fill(
    request: TableFillRequest,
    token: str = Depends(internal_auth)
) -> TableFillResponse:
    """表格填写接口（预留）"""
    return TableFillResponse(
        code=501,
        msg="表格填写功能暂未实现，将在表格助手模块迁移后开放",
        data=None
    )


# =============================================================================
# 4. 健康检查（无需认证）
# =============================================================================

@router.get(
    "/health",
    summary="服务健康检查",
    description="检查 AI 服务各组件状态，用于负载均衡和监控"
)
async def health_check() -> BaseResponse:
    """
    健康检查接口
    
    说明：
      - 此接口不需要 X-Internal-Token 认证
      - 返回服务状态、版本、各组件就绪情况
      - 建议监控工具每 30 秒调用一次
    
    对接说明（给主项目工程师）：
      - 主后端启动时可调用此接口确认 AI 服务就绪
      - 如果 ocr_ready=false，说明 PaddleOCR 模型还在加载
      - 如果 llm_ready=false，说明 DashScope API Key 未配置或网络不通
    """
    from app.models.schemas import HealthResponse
    
    # 检查各组件状态
    ocr_ready = False
    llm_ready = False
    
    try:
        from app.services.ocr_engine import OcrEngine
        engine = OcrEngine()
        ocr_ready = engine._initialized
    except Exception:
        pass
    
    try:
        if config.DASHSCOPE_API_KEY and len(config.DASHSCOPE_API_KEY) > 10:
            llm_ready = True
    except Exception:
        pass
    
    # 计算运行时间（简化处理）
    uptime = 0  # TODO: 从服务启动时间计算
    
    health_data = HealthResponse(
        status="ok" if (ocr_ready and llm_ready) else "degraded",
        version="1.0.0",
        uptime_seconds=uptime,
        ocr_ready=ocr_ready,
        llm_ready=llm_ready,
        config={
            "model": config.DEFAULT_MODEL,
            "ocr_language": config.OCR_LANGUAGE,
            "ocr_threshold": config.OCR_THRESHOLD,
        }
    )
    
    return BaseResponse(
        code=200,
        msg="服务运行中",
        data=health_data.model_dump()
    )
