#!/bin/bash
# ============================================================================
# 启动 AI 服务（后台）
# ============================================================================

set -e

cd "$(dirname "$0")/../lingdoc-ai-service"
source venv/bin/activate

# 检查是否已运行
PID=$(pgrep -f "uvicorn app.main:app" || true)
if [ -n "$PID" ]; then
    echo "AI 服务已在运行 (PID: $PID)"
    exit 0
fi

echo "🚀 启动 AI 服务..."
nohup ./venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > /tmp/lingdoc-ai-service.log 2>&1 &

sleep 2

# 验证
if curl -s http://localhost:8000/api/ai/v1/health > /dev/null; then
    echo "✅ AI 服务启动成功: http://localhost:8000"
else
    echo "⚠️ AI 服务可能未启动，查看日志: tail -f /tmp/lingdoc-ai-service.log"
    exit 1
fi