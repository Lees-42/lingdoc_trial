# -*- coding: utf-8 -*-
"""
=============================================================================
LingDoc AI Service - LLM Client (Qwen3-Max via DashScope)
=============================================================================
用途：调用阿里云 Qwen3-Max 大模型，实现文档分析、摘要生成、标签提取

设计原则：
  1. 统一封装：隐藏 DashScope SDK 细节，提供简洁接口
  2. 异步调用：HTTP 请求异步化，不阻塞主线程
  3. 容错处理：超时、重试、降级策略内置
  4. Token 统计：每次调用返回用量，用于成本追踪

模型说明：
  - qwen3-max：阿里最新旗舰模型，128K 上下文，支持多模态
  - 适用场景：长文档分析、复杂推理、代码生成
  - 计费：按输入+输出 Token 计费

对接说明（给主项目工程师）：
  - 主后端无需关心 LLM 细节，只需传 OCR 文本
  - AI 服务内部完成：构建 Prompt → 调用模型 → 解析结果
  - 建议主后端设置超时 ≥ 60 秒（长文档分析耗时）
=============================================================================
"""

import json
import time
import asyncio
from typing import Dict, List, Optional, Any
from dataclasses import dataclass

import httpx
from app.config import config
from app.utils.logger import logger


@dataclass
class LLMResult:
    """
    LLM 调用结果封装
    
    用途：标准化模型输出，无论底层是哪个模型，返回结构一致
    
    字段：
        success: 是否成功
        content: 模型生成的文本内容
        token_usage: Token 消耗总量
        input_tokens: 输入 Token 数
        output_tokens: 输出 Token 数
        model: 实际使用的模型名称
        duration_ms: 调用耗时（毫秒）
        error: 错误信息（失败时）
    """
    success: bool
    content: str = ""
    token_usage: int = 0
    input_tokens: int = 0
    output_tokens: int = 0
    model: str = ""
    duration_ms: int = 0
    error: Optional[str] = None


class LLMClient:
    """
    大模型客户端（单例）
    
    用途：封装阿里云 DashScope API 调用，提供文档分析能力
    
    支持功能：
      1. analyze_document: 文档综合分析（摘要 + 关键词 + 分类）
      2. generate_tags: 自动生成标签
      3. extract_fields: 表格字段提取（给表格填写用）
      4. qa_answer: 文档问答
    
    使用方式：
        ```python
        client = LLMClient()
        result = await client.analyze_document("这是一篇关于电路的实验报告...")
        print(result.content)  # JSON 格式的分析结果
        ```
    """
    
    _instance: Optional["LLMClient"] = None
    
    def __new__(cls) -> "LLMClient":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        # DashScope API 配置
        self.api_key = config.DASHSCOPE_API_KEY
        self.base_url = "https://dashscope.aliyuncs.com/api/v1"
        self.model = config.DEFAULT_MODEL
        
        # HTTP 客户端（异步）
        self.client = httpx.AsyncClient(
            timeout=httpx.Timeout(config.LLM_TIMEOUT),
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            }
        )
        
        self._initialized = True
        logger.info(f"LLM 客户端初始化完成 | 模型: {self.model}")
    
    # ========================================================================
    # 核心方法：文档综合分析（OCR 文本 → 结构化元数据）
    # ========================================================================
    
    async def analyze_document(
        self,
        ocr_text: str,
        file_name: str = "",
        file_type: str = "",
        task_id: str = ""
    ) -> LLMResult:
        """
        文档综合分析：生成摘要、关键词、分类
        
        参数：
            ocr_text: OCR 提取的完整文本
            file_name: 原始文件名（辅助判断文档类型）
            file_type: 文件类型（pdf/docx/...）
            task_id: 任务 ID，用于日志追踪
        
        返回：
            LLMResult: 分析结果（content 字段包含 JSON）
        
        示例输出（content 字段）：
            {
              "summary": "本文档是一份电路实验报告，涵盖了...",
              "keywords": ["电路", "实验报告", "基尔霍夫定律", "电压测量"],
              "category": "实验报告",
              "subject": "电路理论",
              "difficulty": "中等",
              "language": "中文"
            }
        """
        start_time = time.time()
        
        try:
            # 构建系统提示词（System Prompt）
            # 作用：告诉模型它的角色和输出格式要求
            system_prompt = """你是一位专业的文档分析助手。你的任务是分析用户提供的文档内容，提取关键信息并生成结构化元数据。

请严格按照以下 JSON 格式输出，不要包含任何其他内容：
{
  "summary": "文档摘要（100-300字）",
  "keywords": ["关键词1", "关键词2", ...],
  "category": "文档分类（如：实验报告、课程笔记、参考文档、作业答案）",
  "subject": "所属学科（如：电路理论、操作系统、数据结构）",
  "language": "文档语言（中文/英文/混合）",
  "entities": ["提取的实体名词（如：人名、定理名、公式名）"]
}

要求：
1. summary 必须准确概括文档核心内容
2. keywords 至少提取 5 个，按重要性排序
3. category 必须从预设分类中选择，不要自创
4. 只输出 JSON，不要 markdown 代码块标记"""

            # 构建用户提示词（User Prompt）
            # 包含文件名、类型和 OCR 文本
            user_prompt = f"""文件名：{file_name}
文件类型：{file_type}
文档内容：
---
{ocr_text[:8000]}  # 截断到 8K Token 以内（Qwen3-Max 上下文 128K，但控制成本）
---

请分析以上文档，按要求的 JSON 格式输出元数据。"""

            # 调用模型
            result = await self._call_model(
                system=system_prompt,
                user=user_prompt,
                task_id=task_id
            )
            
            # 解析 JSON 结果
            if result.success:
                try:
                    # 尝试提取 JSON（模型可能输出 markdown 代码块）
                    content = result.content
                    if "```json" in content:
                        content = content.split("```json")[1].split("```")[0]
                    elif "```" in content:
                        content = content.split("```")[1].split("```")[0]
                    
                    parsed = json.loads(content.strip())
                    
                    # 验证必要字段
                    required_fields = ["summary", "keywords", "category"]
                    for field in required_fields:
                        if field not in parsed:
                            parsed[field] = ""
                    
                    # 将解析后的 JSON 重新序列化为字符串
                    result.content = json.dumps(parsed, ensure_ascii=False)
                    
                except json.JSONDecodeError as e:
                    logger.warning(f"[LLM:{task_id}] JSON 解析失败，返回原始文本: {str(e)}")
                    # 失败时返回原始文本，让上层处理
            
            result.duration_ms = int((time.time() - start_time) * 1000)
            
            logger.info(
                f"[LLM:{task_id}] 文档分析完成 | tokens={result.token_usage} | "
                f"耗时={result.duration_ms}ms"
            )
            
            return result
            
        except Exception as e:
            logger.error(f"[LLM:{task_id}] 文档分析异常: {str(e)}", exc_info=True)
            return LLMResult(
                success=False,
                error=f"LLM 调用失败: {str(e)}",
                duration_ms=int((time.time() - start_time) * 1000)
            )
    
    # ========================================================================
    # 核心方法：自动生成标签
    # ========================================================================
    
    async def generate_tags(
        self,
        ocr_text: str,
        file_name: str = "",
        existing_tags: Optional[List[str]] = None,
        task_id: str = ""
    ) -> LLMResult:
        """
        自动生成文档标签
        
        参数：
            ocr_text: OCR 文本
            file_name: 文件名
            existing_tags: 已有标签列表（避免重复）
            task_id: 任务 ID
        
        返回：
            LLMResult: content 字段包含标签列表 JSON
        
        示例输出：
            {
              "tags": [
                {"name": "电路", "color": "#409EFF", "confidence": 0.95, "source": "ai_auto"},
                {"name": "实验报告", "color": "#67C23A", "confidence": 0.88, "source": "ai_auto"},
                {"name": "2026春", "color": "#E6A23C", "confidence": 0.72, "source": "rule_based"}
              ]
            }
        """
        start_time = time.time()
        
        try:
            system_prompt = """你是一位标签分类专家。请根据文档内容生成合适的标签。

标签生成规则：
1. 内容标签：从文档主题提取（如：电路、操作系统、数据结构）
2. 类型标签：从文档格式判断（如：实验报告、课程笔记、参考答案）
3. 时间标签：从文件名或内容推断（如：2026春、大三上）
4. 优先级标签：如果是作业/考试相关，标记紧急程度

输出格式（严格 JSON）：
{
  "tags": [
    {
      "name": "标签名称",
      "color": "颜色代码（从预设中选择）",
      "confidence": 0.95,
      "source": "ai_auto 或 rule_based",
      "category": "content/type/time/priority"
    }
  ]
}

颜色预设：
- 内容标签（蓝）：#409EFF
- 类型标签（绿）：#67C23A
- 时间标签（橙）：#E6A23C
- 优先级（红）：#F56C6C
- 其他（灰）：#909399"""

            user_prompt = f"""文件名：{file_name}
已有标签：{', '.join(existing_tags or [])}
文档内容：
---
{ocr_text[:5000]}
---

请生成标签，不要和已有标签重复。"""

            result = await self._call_model(
                system=system_prompt,
                user=user_prompt,
                task_id=task_id
            )
            
            if result.success:
                try:
                    content = result.content
                    if "```json" in content:
                        content = content.split("```json")[1].split("```")[0]
                    elif "```" in content:
                        content = content.split("```")[1].split("```")[0]
                    
                    parsed = json.loads(content.strip())
                    
                    # 确保 tags 字段存在
                    if "tags" not in parsed:
                        parsed = {"tags": []}
                    
                    result.content = json.dumps(parsed, ensure_ascii=False)
                    
                except json.JSONDecodeError:
                    logger.warning(f"[LLM:{task_id}] 标签 JSON 解析失败")
            
            result.duration_ms = int((time.time() - start_time) * 1000)
            
            logger.info(
                f"[LLM:{task_id}] 标签生成完成 | tags={len(json.loads(result.content).get('tags', []))} | "
                f"耗时={result.duration_ms}ms"
            )
            
            return result
            
        except Exception as e:
            logger.error(f"[LLM:{task_id}] 标签生成异常: {str(e)}", exc_info=True)
            return LLMResult(
                success=False,
                error=f"标签生成失败: {str(e)}",
                duration_ms=int((time.time() - start_time) * 1000)
            )
    
    # ========================================================================
    # 私有方法：底层模型调用
    # ========================================================================
    
    async def _call_model(
        self,
        system: str,
        user: str,
        task_id: str = "",
        max_retries: int = 2
    ) -> LLMResult:
        """
        底层模型调用（带重试）
        
        参数：
            system: 系统提示词
            user: 用户提示词
            task_id: 任务 ID
            max_retries: 最大重试次数
        
        返回：
            LLMResult: 调用结果
        """
        start_time = time.time()
        
        # 构建请求体（兼容 Qwen3-Max 格式）
        payload = {
            "model": self.model,
            "input": {
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": user}
                ]
            },
            "parameters": {
                "result_format": "message",  # 返回消息格式
                "max_tokens": 4096,          # 最大输出 Token
                "temperature": 0.3,          # 低温度（更确定性的输出）
                "top_p": 0.9,
            }
        }
        
        last_error = None
        
        for attempt in range(max_retries + 1):
            try:
                logger.debug(f"[LLM:{task_id}] 调用尝试 {attempt+1}/{max_retries+1}")
                
                response = await self.client.post(
                    f"{self.base_url}/services/aigc/text-generation/generation",
                    json=payload
                )
                
                # 检查 HTTP 状态
                if response.status_code != 200:
                    error_text = response.text[:200]
                    logger.warning(
                        f"[LLM:{task_id}] HTTP {response.status_code}: {error_text}"
                    )
                    last_error = f"HTTP {response.status_code}"
                    
                    # 429（限流）或 5xx（服务端错误）时重试
                    if response.status_code in [429, 500, 502, 503, 504]:
                        await asyncio.sleep(2 ** attempt)  # 指数退避
                        continue
                    else:
                        break
                
                # 解析响应
                data = response.json()
                
                # 提取 Token 用量
                token_usage = 0
                input_tokens = 0
                output_tokens = 0
                
                if "usage" in data:
                    usage = data["usage"]
                    input_tokens = usage.get("input_tokens", 0)
                    output_tokens = usage.get("output_tokens", 0)
                    token_usage = input_tokens + output_tokens
                
                # 提取生成的内容
                content = ""
                if "output" in data and "choices" in data["output"]:
                    choices_data = data["output"]["choices"]
                    if choices_data:
                        content = choices_data[0].get("message", {}).get("content", "")
                
                duration_ms = int((time.time() - start_time) * 1000)
                
                return LLMResult(
                    success=True,
                    content=content,
                    token_usage=token_usage,
                    input_tokens=input_tokens,
                    output_tokens=output_tokens,
                    model=self.model,
                    duration_ms=duration_ms
                )
                
            except httpx.TimeoutException:
                last_error = "请求超时"
                logger.warning(f"[LLM:{task_id}] 超时，等待重试...")
                await asyncio.sleep(2 ** attempt)
                
            except Exception as e:
                last_error = str(e)
                logger.error(f"[LLM:{task_id}] 调用异常: {last_error}")
                await asyncio.sleep(2 ** attempt)
        
        # 所有重试失败
        return LLMResult(
            success=False,
            error=f"模型调用失败（重试 {max_retries+1} 次）: {last_error}",
            duration_ms=int((time.time() - start_time) * 1000)
        )


# =============================================================================
# 快捷函数
# =============================================================================

async def analyze_document(
    ocr_text: str,
    file_name: str = "",
    file_type: str = "",
    task_id: str = ""
) -> LLMResult:
    """快捷函数：文档分析入口"""
    client = LLMClient()
    return await client.analyze_document(ocr_text, file_name, file_type, task_id)


async def generate_tags(
    ocr_text: str,
    file_name: str = "",
    existing_tags: Optional[List[str]] = None,
    task_id: str = ""
) -> LLMResult:
    """快捷函数：标签生成入口"""
    client = LLMClient()
    return await client.generate_tags(ocr_text, file_name, existing_tags, task_id)
