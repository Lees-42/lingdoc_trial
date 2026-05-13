# -*- coding: utf-8 -*-
"""
=============================================================================
Celery Tasks - AI 异步任务定义
=============================================================================
任务清单:
  process_document_task - 文档处理（OCR + AI分析 + 标签）
  fill_form_task        - 端到端智能填表

进度追踪:
  - 每个任务通过 Redis 存储中间状态
  - Java 后端轮询 Redis 获取进度

状态键格式:
  lingdoc:task:{task_id}:status -> {"stage": "extracting", "progress": 50, "msg": "..."}
=============================================================================
"""

import time
import traceback
from typing import List, Optional

from app.celery_app import celery_app
from app.config import config
from app.services.form_service import FormService
from app.utils.logger import logger
import redis

# Redis 连接（用于进度存储）
_redis_client = None


def get_redis():
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis.from_url(
            f"redis://localhost:6379/0",
            decode_responses=True
        )
    return _redis_client


def _update_progress(task_id: str, stage: str, progress: int, msg: str = ""):
    """更新任务进度到 Redis"""
    try:
        r = get_redis()
        r.hset(f"lingdoc:task:{task_id}:status", mapping={
            "stage": stage,
            "progress": str(progress),
            "msg": msg,
            "updated_at": str(int(time.time()))
        })
        r.expire(f"lingdoc:task:{task_id}:status", 3600)  # 1 小时过期
    except Exception as e:
        logger.warning(f"更新进度失败: {e}")


def _mark_done(task_id: str, success: bool, result: dict, error: str = ""):
    """标记任务完成"""
    try:
        r = get_redis()
        r.hset(f"lingdoc:task:{task_id}:status", mapping={
            "stage": "done" if success else "failed",
            "progress": "100" if success else "0",
            "msg": error if error else "完成",
            "success": "1" if success else "0",
            "result": str(result),
            "updated_at": str(int(time.time()))
        })
    except Exception as e:
        logger.warning(f"标记完成失败: {e}")


# =============================================================================
# 1. 智能填表任务（核心）
# =============================================================================

@celery_app.task(bind=True, max_retries=1, default_retry_delay=30)
def fill_form_task(self,
                   task_id: str,
                   reference_paths: List[str],
                   template_path: str,
                   output_path: str):
    """
    端到端智能填表 Celery 任务

    执行流程:
      1. 提取参考文档信息 (0-30%)
      2. 生成填写值 (30-70%)
      3. 渲染最终文档 (70-100%)
    """
    logger.info(f"[Celery] fill_form_task 开始, task_id={task_id}")
    start = time.time()
    form_service = FormService()

    try:
        # Stage 1: 提取 (0-30%)
        _update_progress(task_id, "extracting", 10, "正在分析参考文档...")
        # TODO: 当 FormService 支持分步调用时，替换为实际分步逻辑
        # 目前直接调用端到端方法

        # Stage 2: 生成 (30-70%)
        _update_progress(task_id, "generating", 40, "正在匹配字段...")

        # Stage 3: 渲染 (70-100%)
        _update_progress(task_id, "rendering", 80, "正在生成最终文档...")

        # 执行端到端填表
        result = form_service.fill_form_end_to_end(
            reference_paths=reference_paths,
            template_path=template_path,
            output_path=output_path,
            task_id=task_id
        )

        duration_ms = int((time.time() - start) * 1000)

        if result.get("success"):
            _mark_done(task_id, True, {
                "output_path": result.get("output_path"),
                "fill_rate": result.get("fill_rate", 0),
                "duration_ms": duration_ms
            })
            logger.info(f"[Celery] fill_form_task 完成, task_id={task_id}, duration={duration_ms}ms")
        else:
            _mark_done(task_id, False, {}, result.get("error", "未知错误"))
            logger.error(f"[Celery] fill_form_task 失败, task_id={task_id}, error={result.get('error')}")

    except Exception as exc:
        logger.error(f"[Celery] fill_form_task 异常, task_id={task_id}: {exc}")
        logger.error(traceback.format_exc())
        _mark_done(task_id, False, {}, str(exc))
        # 重试一次
        raise self.retry(exc=exc)


# =============================================================================
# 2. 文档处理任务（预留）
# =============================================================================

@celery_app.task(bind=True, max_retries=1, default_retry_delay=30)
def process_document_task(self, file_path: str, task_id: str):
    """
    文档处理任务: OCR + AI分析 + 标签生成

    预留接口，供 Vault 文档入库时异步调用
    """
    logger.info(f"[Celery] process_document_task 开始, task_id={task_id}")
    _update_progress(task_id, "ocr", 10, "正在识别文档...")

    try:
        # TODO: 接入 OCR 引擎
        _mark_done(task_id, True, {"file_path": file_path}, "")
        logger.info(f"[Celery] process_document_task 完成, task_id={task_id}")
    except Exception as exc:
        logger.error(f"[Celery] process_document_task 异常: {exc}")
        _mark_done(task_id, False, {}, str(exc))
        raise self.retry(exc=exc)
