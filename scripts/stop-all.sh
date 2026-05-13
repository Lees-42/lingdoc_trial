#!/bin/bash
# ============================================================================
# 一键停止所有服务
# ============================================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🛑 正在停止所有服务..."

# 停止 AI 服务
PID=$(pgrep -f "uvicorn app.main:app" || true)
if [ -n "$PID" ]; then
    kill $PID 2>/dev/null || true
    echo -e "${GREEN}✅ AI 服务已停止${NC}"
else
    echo -e "${YELLOW}⚠️ AI 服务未运行${NC}"
fi

# 停止 Celery Worker
PID=$(pgrep -f "celery -A app.celery_app worker" || true)
if [ -n "$PID" ]; then
    pkill -f "celery -A app.celery_app worker" || true
    echo -e "${GREEN}✅ Celery Worker 已停止${NC}"
else
    echo -e "${YELLOW}⚠️ Celery Worker 未运行${NC}"
fi

# 停止 Java 后端
PID=$(pgrep -f "ruoyi-admin" || true)
if [ -n "$PID" ]; then
    kill $PID 2>/dev/null || true
    echo -e "${GREEN}✅ Java 后端已停止${NC}"
else
    echo -e "${YELLOW}⚠️ Java 后端未运行${NC}"
fi

echo ""
echo "所有服务已停止。如需重启，请运行 VSCode 任务 '🚀 一键启动完整开发环境'"