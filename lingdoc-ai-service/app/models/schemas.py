# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - Pydantic Data Models (Schemas)
=============================================================================
用途：定义所有 API 接口的请求/响应数据模型，自动校验和文档生成

设计原则（参考飞书云空间元数据设计）：
  1. 全局唯一标识：所有对象使用 file_id（类似飞书 token），不依赖路径
  2. 层级关系：用 parent_id 表达父子关系，不存储完整路径
  3. 元数据分离：文件基础信息 + AI 分析结果 + 标签 分开存储
  4. 类型安全：所有字段都有明确类型和校验规则

对接说明（给主项目工程师）：
  - 本文件定义了主后端和 AI 服务之间的 JSON 数据结构
  - 主后端发送请求、接收响应时必须符合这些模型定义
  - FastAPI 会自动根据这些模型生成 Swagger 文档（/docs）
=============================================================================
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, field_validator
from datetime import datetime
from enum import Enum


# =============================================================================
# 1. 枚举类型定义（限制取值范围，避免非法输入）
# =============================================================================

class FileType(str, Enum):
    """文件类型枚举，对应飞书的 type 字段"""
    PDF = "pdf"
    DOCX = "docx"
    DOC = "doc"      # 旧版 Word
    XLSX = "xlsx"
    XLS = "xls"
    PPTX = "pptx"
    PNG = "png"
    JPG = "jpg"
    JPEG = "jpeg"
    BMP = "bmp"
    TXT = "txt"
    MD = "md"        # Markdown
    UNKNOWN = "unknown"


class SourceType(str, Enum):
    """文件来源类型，区分文件产生渠道"""
    MANUAL_UPLOAD = "0"      # 用户手动上传
    AUTO_ORGANIZE = "1"      # AI 自动规整生成
    FORM_GENERATED = "2"     # 表格助手生成
    OCR_EXTRACTED = "3"      # OCR 提取后保存
    BATCH_IMPORT = "4"       # 批量导入


class AiParseStatus(str, Enum):
    """AI 解析状态，借鉴飞书文档处理流程"""
    PENDING = "0"     # 待处理
    PROCESSING = "1"  # 处理中
    SUCCESS = "2"     # 完成
    FAILED = "3"      # 失败
    PARTIAL = "4"     # 部分成功（OCR 完成但 AI 分析失败）


# =============================================================================
# 2. 基础请求/响应模型（所有接口共用）
# =============================================================================

class BaseResponse(BaseModel):
    """
    统一响应基类，所有 AI 服务接口返回此结构
    
    对接说明（给主项目工程师）：
      - 主后端解析响应时，先检查 code，再读取 data
      - trace 字段包含 AI 处理成本（token 用量、耗时），可用于计费统计
    
    示例：
        {
          "code": 200,
          "msg": "处理成功",
          "data": { ... },
          "trace": {
            "token_usage": 1250,
            "process_time_ms": 320
          }
        }
    """
    code: int = Field(200, description="HTTP 状态码，200=成功")
    msg: str = Field("success", description="响应消息，失败时包含具体错误")
    data: Optional[Dict[str, Any]] = Field(None, description="业务数据，失败时可能为空")
    trace: Optional[Dict[str, Any]] = Field(
        None, 
        description="追踪信息：token_usage(大模型用量), process_time_ms(处理耗时), request_id(请求ID)"
    )


class TraceInfo(BaseModel):
    """追踪信息详情，记录 AI 处理成本"""
    request_id: str = Field(..., description="唯一请求 ID，用于排查重复请求/链路追踪")
    token_usage: int = Field(0, description="大模型 Token 消耗量")
    process_time_ms: int = Field(0, description="AI 处理耗时（毫秒）")
    ocr_time_ms: Optional[int] = Field(None, description="OCR 识别耗时（毫秒）")
    llm_time_ms: Optional[int] = Field(None, description="LLM 分析耗时（毫秒）")


# =============================================================================
# 3. 文档处理接口模型（核心 API：/api/ai/v1/doc/process）
# =============================================================================

class DocProcessRequest(BaseModel):
    """
    文档处理请求模型
    
    用途：主后端调用 AI 服务处理文档时发送的请求体
    
    对接说明（给主项目工程师）：
      - file_path: 主后端保存文件后，把绝对路径传给 AI 服务
      - task_id: 主后端生成的唯一任务 ID，AI 服务原样返回，用于状态追踪
      - options: 可选参数，控制 OCR 精度、AI 分析深度等
      
    重要：
      - 不直接传文件二进制，传文件路径（大文件走本地 IO，不走网络）
      - file_path 必须是 AI 服务能访问的路径（Docker 共享 Volume）
    """
    task_id: str = Field(..., description="任务唯一 ID，由主后端生成，格式建议：uuid")
    file_path: str = Field(..., description="文件绝对路径，如 /uploads/lingdoc/123/xxx.pdf")
    user_id: Optional[int] = Field(None, description="用户 ID，用于个性化分析（可选）")
    vault_id: Optional[str] = Field(None, description="所属 Vault ID，用于关联存储（可选）")
    
    # 处理选项
    options: Optional[Dict[str, Any]] = Field(
        default_factory=dict,
        description="""
        处理选项（可选）：
        - ocr_dpi: PDF 转图片 DPI（默认 150）
        - ocr_threshold: 置信度阈值（默认 0.5）
        - ocr_language: 识别语言（默认 ch）
        - enable_ai_analysis: 是否启用 AI 分析（默认 true）
        - enable_tagging: 是否自动生成标签（默认 true）
        - enable_summary: 是否生成摘要（默认 true）
        - model: 指定模型（默认 qwen3-max）
        """
    )
    
    @field_validator("file_path")
    @classmethod
    def validate_file_path(cls, v: str) -> str:
        """校验文件路径合法性"""
        if not v or not v.strip():
            raise ValueError("file_path 不能为空")
        if not v.startswith("/"):
            raise ValueError("file_path 必须是绝对路径（以 / 开头）")
        return v.strip()


class DocProcessResult(BaseModel):
    """
    文档处理结果，嵌套在 BaseResponse.data 中
    
    用途：AI 服务完成文档处理后返回的完整元数据
    
    设计说明（参考飞书元数据设计）：
      - file_id: 全局唯一标识，类似飞书 token
      - parent_id: 父文件夹/目录标识，类似飞书 parent_token
      - 不返回完整路径，主后端通过 file_id 查询 Vault 获取路径
    """
    # --- 基础元数据（参考飞书 file_index 表） ---
    file_id: str = Field(..., description="文件全局唯一 ID（建议格式：{vault_id}_{uuid}）")
    file_name: str = Field(..., description="原始文件名（不含路径）")
    file_type: FileType = Field(..., description="文件类型枚举")
    file_size: int = Field(..., description="文件大小（字节）")
    checksum: str = Field(..., description="文件 MD5/SHA256 校验值，用于去重")
    
    # --- 层级关系（借鉴飞书 parent_token） ---
    parent_id: Optional[str] = Field(None, description="父文件夹 ID（类似飞书 parent_token），空表示根目录")
    vault_path: str = Field(..., description="Vault 内相对路径（纯文件名或短路径）")
    
    # --- 来源信息 ---
    source_type: SourceType = Field(SourceType.MANUAL_UPLOAD, description="文件来源")
    
    # --- OCR 结果 ---
    ocr_text: str = Field("", description="OCR 提取的完整文本内容")
    ocr_page_count: int = Field(0, description="文档总页数")
    ocr_total_lines: int = Field(0, description="识别到的文本行数")
    ocr_total_chars: int = Field(0, description="识别到的字符总数")
    ocr_language: str = Field("ch", description="OCR 识别语言")
    
    # --- AI 分析结果 ---
    ai_parse_status: AiParseStatus = Field(AiParseStatus.PENDING, description="AI 解析状态")
    ai_summary: Optional[str] = Field(None, description="AI 生成的文档摘要")
    ai_keywords: List[str] = Field(default_factory=list, description="AI 提取的关键词列表")
    ai_category: Optional[str] = Field(None, description="AI 判断的文档分类（如：实验报告、课程笔记）")
    
    # --- 自动标签（参考飞书标签体系） ---
    auto_tags: List[Dict[str, Any]] = Field(
        default_factory=list,
        description="""
        自动生成的标签列表，每项包含：
        - tag_name: 标签名称（如：电路、实验报告、2026春）
        - tag_color: 标签颜色（如 #409EFF）
        - confidence: AI 置信度（0.0~1.0）
        - source: 标签来源（ai_auto / rule_based）
        """
    )
    
    # --- 时间戳 ---
    create_time: str = Field(default_factory=lambda: datetime.now().isoformat(), description="创建时间 ISO8601")
    update_time: str = Field(default_factory=lambda: datetime.now().isoformat(), description="更新时间 ISO8601")
    
    # --- 处理统计 ---
    trace: TraceInfo = Field(..., description="处理追踪信息（耗时、Token 用量等）")


class DocProcessResponse(BaseResponse):
    """文档处理接口专用响应，data 字段固定为 DocProcessResult"""
    data: Optional[DocProcessResult] = Field(None, description="文档处理结果详情")


# =============================================================================
# 4. 文档问答接口模型（扩展预留：/api/ai/v1/doc/qa）
# =============================================================================

class DocQARequest(BaseModel):
    """文档问答请求"""
    file_id: str = Field(..., description="目标文件 ID")
    question: str = Field(..., description="用户问题")
    context_window: Optional[int] = Field(5, description="上下文窗口大小（取前后 N 个分块）")


class DocQAResponse(BaseResponse):
    """文档问答响应"""
    data: Optional[Dict[str, Any]] = Field(None, description="包含 answer, relevant_chunks, confidence")


# =============================================================================
# 5. 表格填写接口模型（扩展预留：/api/ai/v1/table/fill）
# =============================================================================

class TableFillRequest(BaseModel):
    """表格填写请求"""
    task_id: str = Field(..., description="任务 ID")
    template_path: str = Field(..., description="表格模板文件路径")
    field_hints: Optional[Dict[str, Any]] = Field(None, description="字段提示信息")
    reference_docs: Optional[List[str]] = Field(None, description="参考文档 file_id 列表")


class TableFillResponse(BaseResponse):
    """表格填写响应"""
    data: Optional[Dict[str, Any]] = Field(None, description="包含 filled_path, field_results, confidence")


# =============================================================================
# 6. 健康检查接口模型
# =============================================================================

class HealthResponse(BaseModel):
    """服务健康状态"""
    status: str = Field("ok", description="服务状态：ok / degraded / error")
    version: str = Field("1.0.0", description="AI 服务版本号")
    uptime_seconds: int = Field(0, description="服务运行时长（秒）")
    ocr_ready: bool = Field(False, description="OCR 引擎是否就绪")
    llm_ready: bool = Field(False, description="LLM 服务是否就绪")
    config: Optional[Dict[str, Any]] = Field(None, description="当前配置摘要（不含敏感信息）")
