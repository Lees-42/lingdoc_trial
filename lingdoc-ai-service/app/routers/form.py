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
  POST /api/ai/v1/form/fill        - 端到端：提取+生成+渲染（同步）
  POST /api/ai/v1/form/fill/async  - 端到端：提交 Celery 异步任务
  GET  /api/ai/v1/form/status/{task_id} - 查询任务进度

对接说明:
  - 所有接口需要 X-Internal-Token 认证
  - /api/ai/v1/form/fill/async 是异步入口，Java 后端立即返回 task_id
  - /api/ai/v1/form/status/{task_id} 供轮询获取进度
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
from app.tasks import fill_form_task
import redis

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
    template_path = request.get("template_path", "")
    output_path = request.get("output_path", "")
    fill_values = request.get("fill_values", {})
    
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    if not output_path:
        return {"code": 400, "msg": "输出路径不能为空", "data": None}
    
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
# 4. 异步填表任务提交
# =============================================================================

@router.post("/form/fill/async", summary="提交异步填表任务")
async def form_fill_async(
    request: dict,
    token: str = Depends(internal_auth)
):
    task_id = request.get("task_id", f"fill_{uuid.uuid4().hex[:8]}")
    reference_paths = request.get("reference_paths", [])
    template_path = request.get("template_path", "")
    output_path = request.get("output_path", "")
    
    if not reference_paths:
        return {"code": 400, "msg": "参考文档路径不能为空", "data": None}
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    if not output_path:
        return {"code": 400, "msg": "输出路径不能为空", "data": None}
    
    celery_task = fill_form_task.delay(
        task_id=task_id,
        reference_paths=reference_paths,
        template_path=template_path,
        output_path=output_path
    )
    
    logger.info(f"[FILL:ASYNC] task_id={task_id}, celery_id={celery_task.id}")
    
    return {
        "code": 200,
        "msg": "任务已提交",
        "data": {
            "task_id": task_id,
            "celery_task_id": celery_task.id,
            "status": "queued"
        }
    }


@router.get("/form/status/{task_id}", summary="查询填表任务状态")
async def form_status(
    task_id: str,
    token: str = Depends(internal_auth)
):
    try:
        r = redis.Redis.from_url("redis://localhost:6379/0", decode_responses=True)
        status = r.hgetall(f"lingdoc:task:{task_id}:status")
        
        if not status:
            return {"code": 200, "msg": "任务状态未找到", "data": {"stage": "unknown", "progress": 0}}
        
        return {
            "code": 200,
            "msg": "查询成功",
            "data": {
                "stage": status.get("stage", "unknown"),
                "progress": int(status.get("progress", 0)),
                "msg": status.get("msg", ""),
                "success": status.get("success") == "1"
            }
        }
    except Exception as e:
        logger.error(f"查询任务状态失败: {e}")
        return {"code": 500, "msg": "查询失败", "data": None}


# =============================================================================
# 5. 端到端一键填表接口（同步版保留兼容）
# =============================================================================

@router.post("/form/fill", summary="端到端智能填表（同步版）")
async def form_fill_end_to_end(
    request: dict,
    token: str = Depends(internal_auth)
):
    task_id = request.get("task_id", f"fill_{uuid.uuid4().hex[:8]}")
    reference_paths = request.get("reference_paths", [])
    template_path = request.get("template_path", "")
    output_path = request.get("output_path", "")
    
    logger.info(
        f"[FILL:{task_id}] 端到端填表 | 参考文档={len(reference_paths)} | "
        f"模板={template_path}"
    )
    
    if not reference_paths:
        return {"code": 400, "msg": "参考文档路径不能为空", "data": None}
    if not template_path or not os.path.exists(template_path):
        return {"code": 400, "msg": "模板文件不存在", "data": None}
    if not output_path:
        return {"code": 400, "msg": "输出路径不能为空", "data": None}
    
    for path in reference_paths:
        if not os.path.exists(path):
            return {"code": 400, "msg": f"参考文件不存在: {path}", "data": None}
    
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
