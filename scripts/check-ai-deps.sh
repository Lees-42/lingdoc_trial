#!/bin/bash
# ============================================================================
# AI 服务依赖检查
# ============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

cd "$(dirname "$0")/.."
AIF="lingdoc-ai-service"

echo "🔍 检查 AI 服务依赖..."

# 检查虚拟环境
if [ ! -d "$AIF/venv" ]; then
    echo -e "${YELLOW}⚠️ 虚拟环境不存在，正在创建...${NC}"
    cd "$AIF"
    python3 -m venv venv
    cd ..
fi

# 检查 .env
if [ ! -f "$AIF/.env" ]; then
    echo -e "${YELLOW}⚠️ .env 文件不存在，正在创建...${NC}"
    cat > "$AIF/.env" << 'EOF'
DASHSCOPE_API_KEY=sk-1786cb3f70ef4291a5514430cde941f8
AI_INTERNAL_TOKEN=lingdoc-ai-2026-a7f3e9d2b8c1e4f5
LLM_MODEL=qwen3-max
REDIS_URL=redis://localhost:6379/0
EOF
    echo -e "${YELLOW}⚠️ 请检查 $AIF/.env 中的 API Key 是否有效${NC}"
fi

# 检查关键依赖
cd "$AIF"
source venv/bin/activate

MISSING=0
for pkg in fastapi uvicorn celery redis requests; do
    if ! python -c "import $pkg" 2>/dev/null; then
        echo -e "${YELLOW}⚠️ 缺少依赖: $pkg${NC}"
        MISSING=$((MISSING + 1))
    fi
done

if [ $MISSING -gt 0 ]; then
    echo "正在安装依赖..."
    pip install -r requirements.txt -q
fi

echo -e "${GREEN}✅ AI 服务依赖已就绪${NC}"
