#!/bin/bash
# ============================================================================
# Redis 检查脚本
# 检查 Redis 是否运行，未运行则尝试启动
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔍 检查 Redis 状态..."

if redis-cli ping &> /dev/null; then
    echo -e "${GREEN}✅ Redis 已运行${NC}"
    exit 0
fi

echo -e "${YELLOW}⚠️ Redis 未运行，尝试启动...${NC}"

# 尝试 systemctl
if command -v systemctl &> /dev/null; then
    sudo systemctl start redis-server 2>/dev/null || sudo systemctl start redis 2>/dev/null || true
fi

# 检查是否启动成功
sleep 1
if redis-cli ping &> /dev/null; then
    echo -e "${GREEN}✅ Redis 启动成功${NC}"
    exit 0
fi

# 尝试 docker
echo "尝试用 Docker 启动 Redis..."
if command -v docker &> /dev/null; then
    docker run -d -p 6379:6379 --name redis-lingdoc --rm redis:7-alpine 2>/dev/null || true
    sleep 2
    if redis-cli ping &> /dev/null; then
        echo -e "${GREEN}✅ Redis (Docker) 启动成功${NC}"
        exit 0
    fi
fi

echo -e "${RED}❌ 无法启动 Redis，请手动安装: sudo apt install redis-server${NC}"
exit 1