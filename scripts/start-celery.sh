#!/bin/bash
# ============================================================================
# 启动 Celery Worker（后台）
# ============================================================================

set -e

cd "$(dirname "$0")/../lingdoc-ai-service"
source venv/bin/activate

# 检查是否已运行
PID=$(pgrep -f "celery -A app.celery_app worker" || true)
if [ -n "$PID" ]; then
    echo "Celery Worker 已在运行 (PID: $PID)"
    exit 0
fi

echo "🚀 启动 Celery Worker..."
echo "   队列: form, document, celery"
echo "   并发: 2"

nohup ./venv/bin/celery -A app.celery_app worker \
    --loglevel=info \
    --concurrency=2 \
    -Q form,document,celery \
    > /tmp/lingdoc-celery.log 2>&1 &

sleep 2

# 验证
if ./venv/bin/celery -A app.celery_app inspect active --timeout=3 > /dev/null 2>&1; then
    echo "✅ Celery Worker 启动成功"
else
    echo "⚠️ Worker 可能未启动，查看日志: tail -f /tmp/lingdoc-celery.log"
    exit 1
fi
