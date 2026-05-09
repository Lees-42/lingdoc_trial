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
    
    # 1. 查找空白模板文件
    file_path = None
    # 尝试多个可能的上传路径
    possible_patterns = [
        f"/tmp/lingdoc/upload/lingdoc/form/**/{file_name}",
        f"/tmp/lingdoc/uploads/{file_name}",
        f"/tmp/lingdoc/{file_name}",
        f"/tmp/lingdoc/form/upload/{file_name}",
    ]
    import glob
    for pattern in possible_patterns:
        matches = glob.glob(pattern, recursive=True)
        if matches:
            file_path = matches[0]
            logger.info(f"[EXTRACT_COMPAT:{task_id}] 找到文件: {file_path}")
            break
    
    # 如果精确匹配失败，尝试根据文件名前缀模糊匹配
    if not file_path:
        base_name = os.path.splitext(file_name)[0]
        all_files = glob.glob("/tmp/lingdoc/upload/lingdoc/form/**/*", recursive=True)
        for f in all_files:
            if os.path.basename(f).startswith(base_name):
                file_path = f
                logger.info(f"[EXTRACT_COMPAT:{task_id}] 模糊匹配找到文件: {file_path}")
                break
    
    # 2. 从模板提取字段定义
    fields = []
    if file_path and os.path.exists(file_path):
        field_list = form_service._extract_fields_from_template(file_path)
        for i, field_name in enumerate(field_list):
            fields.append({
                "fieldName": field_name,
                "fieldType": "text",
                "fieldLabel": field_name,
                "suggestedValue": "",
                "confidence": 0.85,
                "sourceDocId": "",
                "sourceDocName": file_name,
                "sortOrder": i,
                "options": []
            })
        logger.info(f"[EXTRACT_COMPAT:{task_id}] 从模板提取 {len(fields)} 个字段")
    else:
        logger.warning(f"[EXTRACT_COMPAT:{task_id}] 未找到模板文件，返回空字段")
    
    # 3. 从 Vault 读取参考文档列表
    references = []
    try:
        vault_db = "/tmp/lingdoc/vault/default/.lingdoc/vault.db"
        if os.path.exists(vault_db):
            import sqlite3
            conn = sqlite3.connect(vault_db)
            cursor = conn.cursor()
            cursor.execute("SELECT file_id, file_name, abs_path, file_type FROM lingdoc_file_index WHERE source_type = '0'")
            for row in cursor.fetchall():
                # 排除空白申请表本身
                if file_name not in row[1]:
                    references.append({
                        "docId": row[0],
                        "docName": row[1],
                        "docPath": row[2],
                        "docType": row[3],
                        "relevance": 0.9
                    })
            conn.close()
            logger.info(f"[EXTRACT_COMPAT:{task_id}] 从 Vault 读取 {len(references)} 个参考文档")
    except Exception as e:
        logger.warning(f"[EXTRACT_COMPAT:{task_id}] 读取 Vault 参考文档失败: {e}")
    
    return {
        "fields": fields,
        "references": references,
        "tokenCost": e2e_result.get("token_usage", 0) if e2e_result.get("token_usage") else 0
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
    
    # 2. 从 Vault 获取参考文档路径
    reference_paths = []
    try:
        vault_db = "/tmp/lingdoc/vault/default/.lingdoc/vault.db"
        if os.path.exists(vault_db):
            import sqlite3
            conn = sqlite3.connect(vault_db)
            cursor = conn.cursor()
            cursor.execute("SELECT abs_path FROM lingdoc_file_index WHERE source_type = '0'")
            for row in cursor.fetchall():
                path = row[0]
                # 排除空白申请表本身
                if original_file_path not in path and os.path.exists(path):
                    reference_paths.append(path)
            conn.close()
            logger.info(f"[GENERATE_COMPAT:{task_id}] 参考文档: {reference_paths}")
    except Exception as e:
        logger.warning(f"[GENERATE_COMPAT:{task_id}] 读取 Vault 参考文档失败: {e}")
    
    # 生成输出路径
    output_path = original_file_path.replace("_空白_", "_已填写_") if "_空白_" in original_file_path else f"{original_file_path.rsplit('.', 1)[0]}_filled.{file_type}"
    os.makedirs(os.path.dirname(output_path) if os.path.dirname(output_path) else "/tmp/lingdoc/output", exist_ok=True)
    
    # 4. 如果有参考文档，调用端到端填表（提取+匹配+渲染）
    if reference_paths and os.path.exists(original_file_path):
        logger.info(f"[GENERATE_COMPAT:{task_id}] 调用端到端填表 | 参考文档={len(reference_paths)}")
        e2e_result = await form_service.fill_form_end_to_end(
            reference_paths=reference_paths,
            template_path=original_file_path,
            output_path=output_path,
            task_id=task_id
        )
        
        if e2e_result.get("success"):
            logger.info(f"[GENERATE_COMPAT:{task_id}] 端到端填表成功: {e2e_result.get('output_path')}")
            return {
                "filledFilePath": e2e_result.get("output_path", output_path),
                "filledValues": e2e_result.get("fill_values", {}),
                "tokenCost": e2e_result.get("token_usage", 0) if e2e_result.get("token_usage") else 0
            }
        else:
            logger.warning(f"[GENERATE_COMPAT:{task_id}] 端到端填表失败: {e2e_result.get('error')}，回退到直接渲染")
    
    # 5. 兜底：如果没有参考文档或端到端失败，用 confirmedFields 直接渲染
    fill_values = {}
    for field in confirmed_fields:
        name = field.get("fieldName", "")
        # 优先用 userValue，其次 aiValue，最后 suggestedValue
        value = field.get("userValue", "") or field.get("aiValue", "") or field.get("suggestedValue", "")
        if name:
            fill_values[name] = value


async def _handle_default(inputs: Dict, task_id: str) -> Dict:
    """兜底处理：尝试端到端"""
    logger.warning(f"[DEFAULT_COMPAT:{task_id}] 未知 workflow，返回空输出")
    return {"message": "未识别的 workflow，请使用 form-extract 或 form-generate"}
