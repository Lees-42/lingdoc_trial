# -*- coding: utf-8 -*-
"""
=============================================================================
Celery Application - AI 服务异步任务队列
=============================================================================
用途：管理 AI 耗时任务的异步执行，防止并发过大导致 OOM

配置：
  - Broker: Redis（任务队列）
  - Backend: Redis（结果存储）
  - 并发限制: 2（防止 PaddleOCR 模型同时加载过多）

启动 Worker:
  cd lingdoc-ai-service
  source venv/bin/activate
  celery -A app.celery_app worker --loglevel=info --concurrency=2

Flower 监控（可选）:
  celery -A app.celery_app flower --port=5555
=============================================================================
"""

from celery import Celery
from app.config import config

# Redis URL（从配置读取，默认本地）
REDIS_URL = f"redis://localhost:6379/0"

# 创建 Celery 应用实例
celery_app = Celery(
    "lingdoc_ai",
    broker=REDIS_URL,
    backend=REDIS_URL,
    include=["app.tasks"],  # 加载任务模块
)

# Celery 配置
celery_app.conf.update(
    # 序列化
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    # 时区
    timezone="Asia/Shanghai",
    enable_utc=True,
    # 任务执行限制
    task_track_started=True,
    task_time_limit=300,  # 单任务最大 5 分钟
    worker_prefetch_multiplier=1,  # 每个 worker 只预取 1 个任务（公平调度）
    # 结果存储
    result_expires=3600,  # 结果保留 1 小时
    # 任务路由（未来可按任务类型分配不同队列）
    task_routes={
        "app.tasks.process_document_task": {"queue": "document"},
        "app.tasks.fill_form_task": {"queue": "form"},
    },
)

# 健康检查：启动时验证 Redis 连接
@celery_app.on_after_configure.connect
def setup_periodic_tasks(sender, **kwargs):
    sender.add_periodic_task(60.0, health_check.s(), name="health-check-every-60s")


@celery_app.task(bind=True)
def health_check(self):
    """Celery 健康检查任务"""
    return {"status": "ok", "worker": self.request.hostname}
