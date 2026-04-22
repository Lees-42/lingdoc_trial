# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Configuration Module
=============================================================================
用途：集中管理 AI 服务的所有配置项，支持环境变量覆盖
设计原则：
  1. 敏感信息（API Key、Token）必须从环境变量读取，禁止硬编码
  2. 提供默认值，方便本地开发
  3. 服务启动时校验必填配置，缺失则报错

对接说明（给主项目工程师）：
  - 本配置决定 AI 服务监听端口、认证密钥、OCR 参数等
  - 主后端只需关注 AI_SERVICE_URL 和 AI_INTERNAL_TOKEN 两个值
  - 其余参数由 AI 工程师（用户）自行调整

环境变量清单（部署前必须设置）：
  - DASHSCOPE_API_KEY: 阿里云大模型 API Key（必填）
  - AI_INTERNAL_TOKEN: 主后端与 AI 服务之间的共享密钥（必填）
  - AI_SERVICE_PORT: AI 服务监听端口（可选，默认 8000）
=============================================================================
"""

import os
from dataclasses import dataclass, field
from typing import Optional

# 加载 .env 文件环境变量（开发环境用）
from dotenv import load_dotenv
load_dotenv()


@dataclass
class AIConfig:
    """
    AI 服务配置类
    
    使用方式：
        from app.config import config
        print(config.DASHSCOPE_API_KEY)
    
    说明：实例化时自动从环境变量加载，支持默认值
    """
    
    # -----------------------------------------------------------------------
    # 1. 服务基础配置
    # -----------------------------------------------------------------------
    # AI 服务监听端口，主后端通过此端口访问 AI 服务
    # 建议 Docker 部署时映射为 8000:8000
    SERVICE_PORT: int = field(default_factory=lambda: int(os.getenv("AI_SERVICE_PORT", "8000")))
    
    # 服务主机，0.0.0.0 允许外部访问（Docker 必须），127.0.0.1 仅本地
    SERVICE_HOST: str = field(default_factory=lambda: os.getenv("AI_SERVICE_HOST", "0.0.0.0"))
    
    # -----------------------------------------------------------------------
    # 2. 安全认证（⚠️ 必填，不能为空）
    # -----------------------------------------------------------------------
    # 主后端与 AI 服务之间的共享密钥，用于验证请求来源
    # 主后端每次请求必须携带 Header: X-Internal-Token: {此值}
    # 安全建议：生产环境使用 32 位以上随机字符串，定期轮换
    AI_INTERNAL_TOKEN: str = field(default_factory=lambda: os.getenv("AI_INTERNAL_TOKEN", ""))
    
    # -----------------------------------------------------------------------
    # 3. LLM 配置（阿里云 DashScope）
    # -----------------------------------------------------------------------
    # 阿里云 DashScope API Key，用于调用 Qwen3-Max 等大模型
    # 获取方式：https://dashscope.console.aliyun.com/apiKey
    # ⚠️ 必填，没有则 AI 分析功能无法工作
    DASHSCOPE_API_KEY: str = field(default_factory=lambda: os.getenv("DASHSCOPE_API_KEY", ""))
    
    # 默认使用的模型名称
    # 当前推荐：qwen3-max（性能最强）
    # 可选降级：qwen3-30b-a3b（性价比高）
    DEFAULT_MODEL: str = field(default_factory=lambda: os.getenv("AI_DEFAULT_MODEL", "qwen3-max"))
    
    # LLM 调用超时时间（秒），主后端应设置比此值稍大的超时
    LLM_TIMEOUT: int = field(default_factory=lambda: int(os.getenv("AI_LLM_TIMEOUT", "60")))
    
    # -----------------------------------------------------------------------
    # 4. OCR 配置（PaddleOCR）
    # -----------------------------------------------------------------------
    # PDF 转图片时的 DPI（每英寸像素数）
    # 值越大识别精度越高，但速度越慢；150 是速度与精度的平衡点
    PDF_DPI: int = field(default_factory=lambda: int(os.getenv("AI_PDF_DPI", "150")))
    
    # OCR 置信度阈值，低于此值的识别结果会被丢弃
    # 范围 0.0~1.0，默认 0.5（50% 置信度）
    OCR_THRESHOLD: float = field(default_factory=lambda: float(os.getenv("AI_OCR_THRESHOLD", "0.5")))
    
    # OCR 识别语言，"ch" 表示中文+英文混合
    OCR_LANGUAGE: str = field(default_factory=lambda: os.getenv("AI_OCR_LANGUAGE", "ch"))
    
    # -----------------------------------------------------------------------
    # 5. 文件存储配置
    # -----------------------------------------------------------------------
    # 文件上传临时目录，主后端保存文件后传此路径给 AI 服务
    # Docker 部署时必须和主后端挂载同一个 Volume
    # 建议：/uploads/lingdoc（和主后端 uploads 目录一致）
    UPLOAD_DIR: str = field(default_factory=lambda: os.getenv("AI_UPLOAD_DIR", "/tmp/lingdoc/uploads"))
    
    # 处理结果输出目录（OCR 文本、AI 分析结果等缓存）
    OUTPUT_DIR: str = field(default_factory=lambda: os.getenv("AI_OUTPUT_DIR", "/tmp/lingdoc/output"))
    
    # -----------------------------------------------------------------------
    # 6. 日志配置
    # -----------------------------------------------------------------------
    # 日志级别：DEBUG（开发）/ INFO（生产）/ WARNING
    LOG_LEVEL: str = field(default_factory=lambda: os.getenv("AI_LOG_LEVEL", "INFO"))


# =============================================================================
# 全局配置实例（单例模式）
# =============================================================================
# 说明：整个应用共享同一个 config 实例，避免重复读取环境变量
# 导入方式：from app.config import config
# =============================================================================
config = AIConfig()


def validate_config() -> None:
    """
    配置校验函数，服务启动时调用
    
    用途：检查必填配置是否缺失，缺失则抛出异常阻止启动
    
    对接说明（给主项目工程师）：
      - 如果启动报错 "DASHSCOPE_API_KEY 未配置"，说明阿里云 API Key 没设置
      - 如果报错 "AI_INTERNAL_TOKEN 未配置"，说明共享密钥没设置
    
    Raises:
        ValueError: 必填配置缺失时抛出
    """
    # 检查必填项
    required_fields = [
        ("DASHSCOPE_API_KEY", "阿里云大模型 API Key"),
        ("AI_INTERNAL_TOKEN", "主后端共享密钥"),
    ]
    
    missing = []
    for field_name, description in required_fields:
        value = getattr(config, field_name)
        if not value or value.strip() == "":
            missing.append(f"  - {field_name} ({description})")
    
    if missing:
        error_msg = (
            "\n"
            "╔═══════════════════════════════════════════════════════════════╗\n"
            "║  AI 服务启动失败：必填配置缺失                                ║\n"
            "╠═══════════════════════════════════════════════════════════════╣\n"
            + "\n".join(missing) + "\n"
            "╠═══════════════════════════════════════════════════════════════╣\n"
            "║  解决方法：                                                   ║\n"
            "║  1. 本地开发：在项目根目录创建 .env 文件，写入：               ║\n"
            "║     DASHSCOPE_API_KEY=sk-xxxxx                                ║\n"
            "║     AI_INTERNAL_TOKEN=lingdoc-ai-2026-xxxxx                   ║\n"
            "║  2. Docker 部署：在 docker-compose.yml 中设置环境变量          ║\n"
            "║  3. 生产环境：通过 Kubernetes Secret / 环境变量注入            ║\n"
            "╚═══════════════════════════════════════════════════════════════╝\n"
        )
        raise ValueError(error_msg)
    
    # 检查上传目录是否存在
    os.makedirs(config.UPLOAD_DIR, exist_ok=True)
    os.makedirs(config.OUTPUT_DIR, exist_ok=True)


# =============================================================================
# 配置使用示例（给 AI 工程师参考）
# =============================================================================
if __name__ == "__main__":
    # 本地测试时可以直接运行此文件查看当前配置
    print(f"AI 服务端口: {config.SERVICE_PORT}")
    print(f"默认模型: {config.DEFAULT_MODEL}")
    print(f"OCR 语言: {config.OCR_LANGUAGE}")
    print(f"上传目录: {config.UPLOAD_DIR}")
    
    # 校验配置
    try:
        validate_config()
        print("✅ 配置校验通过")
    except ValueError as e:
        print(e)
