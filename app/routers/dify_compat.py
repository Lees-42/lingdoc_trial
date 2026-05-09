# -*- coding: utf-8 -*-
"""
=============================================================================
Dify 兼容层 - 让 Java 后端以为在调 Dify，实际调本地 AI 服务
=============================================================================
后端 DifyWorkflowClient 调用格式：
  POST {base-url}/workflows/run
  Headers: Authorization: Bearer {api-key}
  Body: {"inputs": {...}, "response_mode": "blocking", "user": "user_xxx"}

本路由拦截该请求，根据 workflow 名称分发到本地处理。
=============================================================================
"""

import json
import os
import time
import uuid
from typing import Dict, List, Optional

from fastapi import APIRouter, Request, Header, HTTPException
from fastapi.responses import JSONResponse

from app.config import config
from app.utils.logger import logger
from app.services.form_service import FormService

router = APIRouter()
form_service = FormService()


@router.post("/workflows/run")
async def dify_workflows_run(
    request: Request,
    authorization: Optional[str] = Header(None)
):
    """
    兼容 Dify Workflow API
    
    Java 后端 DifyWorkflowClient 会调用此接口。
    根据 request body 中的 workflow 名称（通过 inputs 推断或 URL 推断），
    分发到本地 AI 服务处理。
    """
    body = await request.json()
    inputs = body.get("inputs", {})
    response_mode = body.get("response_mode", "blocking")
    user = body.get("user", "")
    
    # 从 Authorization Header 提取 API Key（格式: Bearer xxx）
    api_key = ""
    if authorization and authorization.startswith("Bearer "):
        api_key = authorization[7:]
    
    # 从 inputs 推断 workflow 名称
    # 后端 DifyAiFormServiceImpl 调用时传的是 workflow 名称作为 URL 的一部分
    # 但这里 URL 是统一的 /workflows/run，需要从 inputs 推断
    workflow_name = _infer_workflow_name(inputs)
    
    task_id = f"compat_{uuid.uuid4().hex[:8]}"
    start = time.time()
    
    logger.info(f"[DIFY_COMPAT:{task_id}] 收到请求 | workflow={workflow_name} | user={user}")
    
    try:
        if workflow_name == "form-extract":
            outputs = await _handle_form_extract(inputs, task_id)
        elif workflow_name == "form-generate":
            outputs = await _handle_form_generate(inputs, task_id)
        else:
            # 默认或未知 workflow，尝试端到端处理
            outputs = await _handle_default(inputs, task_id)
        
        elapsed = round(time.time() - start, 2)
        
        return JSONResponse({
            "data": {
                "id": task_id,
                "workflowId": workflow_name,
                "status": "succeeded",
                "outputs": outputs,
                "elapsedTime": elapsed
            }
        })
        
    except Exception as e:
        logger.error(f"[DIFY_COMPAT:{task_id}] 处理失败: {e}", exc_info=True)
        return JSONResponse({
            "data": {
                "id": task_id,
                "workflowId": workflow_name,
                "status": "failed",
                "outputs": {"error": str(e)},
                "elapsedTime": round(time.time() - start, 2)
            }
        })


def _infer_workflow_name(inputs: Dict) -> str:
    """从 inputs 推断 workflow 名称"""
    # form-extract 特有字段: fileName, fileContent, fileType
    if "fileName" in inputs and "fileType" in inputs:
        return "form-extract"
    # form-generate 特有字段: originalFilePath, confirmedFields
    if "originalFilePath" in inputs and "confirmedFields" in inputs:
        return "form-generate"
    # 兜底
    return "unknown"


async def _handle_form_extract(inputs: Dict, task_id: str) -> Dict:
    """
    处理 form-extract workflow
    
    后端输入:
      fileName: 上传文件名
      fileContent: 文件文本内容（docx/pdf 时为空）
      fileType: 文件扩展名
    
    需要输出:
      fields: 字段列表
      references: 参考文档列表
      tokenCost: token 消耗
    """
    file_name = inputs.get("fileName", "")
    file_content = inputs.get("fileContent", "")
    file_type = inputs.get("fileType", "")
    
    logger.info(f"[EXTRACT_COMPAT:{task_id}] fileName={file_name}, fileType={file_type}")
    
    # 如果 fileContent 为空，尝试从上传目录找文件
    file_path = None
    if not file_content or len(file_content.strip()) < 10:
        # 尝试多个可能的上传路径
        possible_paths = [
            f"/tmp/lingdoc/uploads/{file_name}",
            f"/tmp/lingdoc/{file_name}",
            f"/tmp/lingdoc/form/upload/{file_name}",
        ]
        for p in possible_paths:
            if os.path.exists(p):
                file_path = p
                logger.info(f"[EXTRACT_COMPAT:{task_id}] 找到文件: {p}")
                break
    
    # 如果找到文件路径，用本地提取
    if file_path:
        result = await form_service.extract_from_document(file_path, task_id)
        if result.get("success"):
            extracted = result.get("data", {})
            fields = []
            for i, (key, value) in enumerate(extracted.items()):
                fields.append({
                    "fieldName": key,
                    "fieldType": "text",
                    "fieldLabel": key,
                    "suggestedValue": str(value) if not isinstance(value, list) else ", ".join(str(v) for v in value),
                    "confidence": 0.85,
                    "sourceDocId": "",
                    "sourceDocName": file_name,
                    "sortOrder": i,
                    "options": []
                })
            return {
                "fields": fields,
                "references": [],
                "tokenCost": result.get("token_usage", 0)
            }
    
    # 兜底：如果 fileContent 有值，直接用 LLM 提取
    if file_content and len(file_content.strip()) > 10:
        # 创建一个临时文件
        tmp_path = f"/tmp/lingdoc/compat_{task_id}.txt"
        os.makedirs("/tmp/lingdoc", exist_ok=True)
        with open(tmp_path, "w", encoding="utf-8") as f:
            f.write(file_content)
        
        result = await form_service.extract_from_document(tmp_path, task_id)
        if result.get("success"):
            extracted = result.get("data", {})
            fields = []
            for i, (key, value) in enumerate(extracted.items()):
                fields.append({
                    "fieldName": key,
                    "fieldType": "text",
                    "fieldLabel": key,
                    "suggestedValue": str(value) if not isinstance(value, list) else ", ".join(str(v) for v in value),
                    "confidence": 0.85,
                    "sourceDocId": "",
                    "sourceDocName": file_name,
                    "sortOrder": i,
                    "options": []
                })
            return {
                "fields": fields,
                "references": [],
                "tokenCost": result.get("token_usage", 0)
            }
    
    # 如果都失败了，返回空字段列表
    logger.warning(f"[EXTRACT_COMPAT:{task_id}] 无法提取内容，返回空字段")
    return {
        "fields": [],
        "references": [],
        "tokenCost": 0
    }


async def _handle_form_generate(inputs: Dict, task_id: str) -> Dict:
    """
    处理 form-generate workflow
    
    后端输入:
      originalFilePath: 模板文件绝对路径
      fileType: 文件扩展名
      confirmedFields: JSON 字符串（[{fieldName, fieldType, fieldValue}, ...]）
    
    需要输出:
      filledFilePath: 填写后的文件路径
      filledValues: 字段名→值映射
      tokenCost: token 消耗
    """
    original_file_path = inputs.get("originalFilePath", "")
    file_type = inputs.get("fileType", "")
    confirmed_fields_json = inputs.get("confirmedFields", "[]")
    
    logger.info(f"[GENERATE_COMPAT:{task_id}] originalFilePath={original_file_path}")
    
    # 解析 confirmedFields
    try:
        confirmed_fields = json.loads(confirmed_fields_json)
    except json.JSONDecodeError:
        confirmed_fields = []
    
    # 转为 fill_values 格式
    fill_values = {}
    for field in confirmed_fields:
        name = field.get("fieldName", "")
        value = field.get("fieldValue", "")
        if name:
            fill_values[name] = value
    
    # 生成输出路径
    output_path = original_file_path.replace("_空白_", "_已填写_") if "_空白_" in original_file_path else f"{original_file_path.rsplit('.', 1)[0]}_filled.{file_type}"
    os.makedirs(os.path.dirname(output_path) if os.path.dirname(output_path) else "/tmp/lingdoc/output", exist_ok=True)
    
    # 调用渲染器
    render_result = form_service.render_document(
        template_path=original_file_path,
        output_path=output_path,
        fill_values=fill_values
    )
    
    if render_result.get("success"):
        logger.info(f"[GENERATE_COMPAT:{task_id}] 渲染成功: {output_path}")
        return {
            "filledFilePath": output_path,
            "filledValues": fill_values,
            "tokenCost": 0  # 本地渲染不消耗 LLM token
        }
    else:
        logger.error(f"[GENERATE_COMPAT:{task_id}] 渲染失败: {render_result.get('error')}")
        raise RuntimeError(f"渲染失败: {render_result.get('error')}")


async def _handle_default(inputs: Dict, task_id: str) -> Dict:
    """兜底处理：尝试端到端"""
    logger.warning(f"[DEFAULT_COMPAT:{task_id}] 未知 workflow，返回空输出")
    return {"message": "未识别的 workflow，请使用 form-extract 或 form-generate"}
