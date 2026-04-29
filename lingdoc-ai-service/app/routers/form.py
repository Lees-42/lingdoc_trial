# -*- coding: utf-8 -*-
"""
=============================================================================
Form Router - 智能填表 API 接口
=============================================================================
API 路径: /api/ai/v1/form/*

接口清单:
  POST /api/ai/v1/form/extract     - 从参考文档提取信息
  POST /api/ai/v1/form/generate    - 匹配模板字段生成填写值
  POST /api/ai/v1/form/render      - 渲染生成最终文件
  POST /api/ai/v1/form/fill        - 端到端：提取+生成+渲染（一键填表）

对接说明:
  - 所有接口需要 X-Internal-Token 认证
  - /api/ai/v1/form/fill 是核心接口，Java 后端直接调用
=============================================================================
"""

import os
import time
import uuid
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, status

from app.middleware.auth import internal_auth
from app.services.form_service import FormService
from app.config import config
from app.utils.logger import logger

router = APIRouter(
    prefix="/api/ai/v1",
    tags=["AI Form Fill"],
    responses={
        401: {"description": "认证失败"},
        500: {"description": "AI 服务内部错误"}
    }
)

form_service = FormService()


# =============================================================================
# 1. 信息提取接口
# =============================================================================

@router.post("/form/extract", summary="从参考文档提取信息")
async def form_extract(
    request: dict,
    token: str = Depends(internal_auth)
):
    """
    从参考文档（成绩单、证书、申请理由等）提取结构化信息
    
    Request:
    {
      "task_id": "uuid",
      "file_path": "/data/lingdoc/upload/xxx.docx"
    }
    
    Response:
    {
      "code": 200,
      "msg": "提取成功",
      "data": {
        "success": true,
        "extracted": {"姓名": "张三", "成绩": 95},
        "duration_ms": 3200,
        "token_usage": 1250
      }
    }
    """
    task_id = request.get("task_id", f"extract_{uuid.uuid4().hex[:8]}")
    file_path = request.get("file_path", "")
    
    if not file_path or not os.path.exists(file_path):
        return {"code": 400, "msg": "文件不存在", "data": None}
    
    result = await form_service.extract_from_document(file_path, task_id)
    
    return {
        "code": 200 if result["success"] else 500,
        "msg": "提取成功" if result["success"] else f"提取失败: {result.get('error')}",
        "data": {
            "success": result["success"],
            "extracted": result.get("data", {}),
            "duration_ms": result.get("duration_ms", 0),
            "token_usage": result.get("token_usage", 0)
        }
    }


# =============================================================================
# 2. 填写值生成接口
# =============================================================================

@router.post("/form/generate", summary="生成表格填写值")
async def form_generate(
    request: dict,
    token: str = Depends(internal_auth)
):
    """
    根据已提取信息和空白表格模板，生成字段填写值
    
    Request:
    {
      "task_id": "uuid",
      "extracted_data": {"姓名": "张三", "成绩": 95},
      "template_path": "/data/lingdoc/upload/form.docx"
    }
    
    Response:
    {
      "code": 200,
      "msg": "生成成功",
      "data": {
        "success": true,
        "fill_values": {"姓名": "张三", "学号": "[待补充]"},
        "fill_rate": 85.0,
        "duration_ms": 2500
      }
    }
    """
    task_id = request.get("task_id", f"gen_{uuid.uuid4().hex[:8]}")
    extracted_data = request.get("extracted_data", {})
    template_path = request.get("template_path", "")
    
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    
    result = await form_service.generate_fill_values(extracted_data, template_path, task_id)
    
    return {
        "code": 200 if result["success"] else 500,
        "msg": "生成成功" if result["success"] else f"生成失败: {result.get('error')}",
        "data": {
            "success": result["success"],
            "fill_values": result.get("fill_values", {}),
            "fill_rate": result.get("fill_rate", 0),
            "duration_ms": result.get("duration_ms", 0),
            "token_usage": result.get("token_usage", 0)
        }
    }


# =============================================================================
# 3. 渲染接口
# =============================================================================

@router.post("/form/render", summary="渲染填写后的表格文件")
async def form_render(
    request: dict,
    token: str = Depends(internal_auth)
):
    """
    根据填写值渲染生成最终文件
    
    Request:
    {
      "task_id": "uuid",
      "template_path": "/data/lingdoc/upload/form.docx",
      "output_path": "/data/lingdoc/output/filled_form.docx",
      "fill_values": {"姓名": "张三", "学号": "20240001"}
    }
    
    Response:
    {
      "code": 200,
      "msg": "渲染成功",
      "data": {
        "success": true,
        "output_path": "/data/lingdoc/output/filled_form.docx",
        "filled_count": 8
      }
    }
    """
    template_path = request.get("template_path", "")
    output_path = request.get("output_path", "")
    fill_values = request.get("fill_values", {})
    
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    
    if not output_path:
        return {"code": 400, "msg": "输出路径不能为空", "data": None}
    
    # 确保输出目录存在
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    result = form_service.render_document(template_path, output_path, fill_values)
    
    return {
        "code": 200 if result.get("success") else 500,
        "msg": "渲染成功" if result.get("success") else f"渲染失败: {result.get('error')}",
        "data": {
            "success": result.get("success", False),
            "output_path": result.get("outputPath") or result.get("output_path"),
            "filled_count": result.get("filledCount", 0),
            "matched_fields": result.get("matchedFields", []),
            "unmatched_fields": result.get("unmatchedFields", [])
        }
    }


# =============================================================================
# 4. 端到端一键填表接口（核心）
# =============================================================================

@router.post("/form/fill", summary="端到端智能填表（核心接口）")
async def form_fill_end_to_end(
    request: dict,
    token: str = Depends(internal_auth)
):
    """
    一键完成：提取+生成+渲染
    
    Request:
    {
      "task_id": "uuid",
      "reference_paths": [
        "/data/lingdoc/upload/成绩单.docx",
        "/data/lingdoc/upload/证书.docx"
      ],
      "template_path": "/data/lingdoc/upload/申请表.docx",
      "output_path": "/data/lingdoc/output/申请表_已填写.docx"
    }
    
    Response:
    {
      "code": 200,
      "msg": "填表成功",
      "data": {
        "success": true,
        "output_path": "/data/lingdoc/output/申请表_已填写.docx",
        "fill_values": {"姓名": "张三", ...},
        "fill_rate": 85.0,
        "duration_ms": 8500
      }
    }
    """
    task_id = request.get("task_id", f"fill_{uuid.uuid4().hex[:8]}")
    reference_paths = request.get("reference_paths", [])
    template_path = request.get("template_path", "")
    output_path = request.get("output_path", "")
    
    logger.info(
        f"[FILL:{task_id}] 端到端填表 | 参考文档={len(reference_paths)} | "
        f"模板={template_path}"
    )
    
    # 参数校验
    if not reference_paths:
        return {"code": 400, "msg": "参考文档路径不能为空", "data": None}
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    if not output_path:
        return {"code": 400, "msg": "输出路径不能为空", "data": None}
    
    # 校验参考文件是否存在
    for path in reference_paths:
        if not os.path.exists(path):
            return {"code": 400, "msg": f"参考文件不存在: {path}", "data": None}
    
    # 执行端到端填表
    start = time.time()
    result = await form_service.fill_form_end_to_end(
        reference_paths=reference_paths,
        template_path=template_path,
        output_path=output_path,
        task_id=task_id
    )
    duration_ms = int((time.time() - start) * 1000)
    
    return {
        "code": 200 if result["success"] else 500,
        "msg": "填表成功" if result["success"] else f"填表失败: {result.get('error')}",
        "data": {
            "success": result["success"],
            "output_path": result.get("output_path"),
            "fill_values": result.get("fill_values", {}),
            "fill_rate": result.get("fill_rate", 0),
            "duration_ms": duration_ms,
            "task_id": task_id
        }
    }
