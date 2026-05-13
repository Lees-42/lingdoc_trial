#!/bin/bash
# ============================================================================
# 环境检查脚本
# 检查所有必要的环境依赖是否就绪
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "🔍 LingDoc 环境检查"
echo "=========================================="
echo ""

ERRORS=0

# 检查 Java
echo -n "Java 17+       ... "
if command -v java &> /dev/null; then
    VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2)
    MAJOR=$(echo "$VERSION" | cut -d '.' -f 1)
    if [ "$MAJOR" -ge 17 ] || [ "$MAJOR" = "1" ]; then
        echo -e "${GREEN}✓ ${VERSION}${NC}"
    else
        echo -e "${RED}✗ ${VERSION} (需要 17+)${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗ 未安装${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 检查 Maven
echo -n "Maven 3.8+     ... "
if command -v mvn &> /dev/null; then
    VERSION=$(mvn -version | head -n 1 | grep -oP '\d+\.\d+\.\d+')
    echo -e "${GREEN}✓ ${VERSION}${NC}"
else
    echo -e "${RED}✗ 未安装${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 检查 Node.js
echo -n "Node.js 18+    ... "
if command -v node &> /dev/null; then
    VERSION=$(node -v | sed 's/v//')
    MAJOR=$(echo "$VERSION" | cut -d '.' -f 1)
    if [ "$MAJOR" -ge 18 ]; then
        echo -e "${GREEN}✓ ${VERSION}${NC}"
    else
        echo -e "${RED}✗ ${VERSION} (需要 18+)${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗ 未安装${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 检查 Python
echo -n "Python 3.10+   ... "
if command -v python3 &> /dev/null; then
    VERSION=$(python3 --version | cut -d ' ' -f 2)
    MAJOR=$(echo "$VERSION" | cut -d '.' -f 1)
    MINOR=$(echo "$VERSION" | cut -d '.' -f 2)
    if [ "$MAJOR" -eq 3 ] && [ "$MINOR" -ge 10 ]; then
        echo -e "${GREEN}✓ ${VERSION}${NC}"
    else
        echo -e "${RED}✗ ${VERSION} (需要 3.10+)${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗ 未安装${NC}"
    ERRORS=$((ERRORS + 1))
fi

# 检查 Redis
echo -n "Redis          ... "
if command -v redis-cli &> /dev/null && redis-cli ping &> /dev/null; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${YELLOW}⚠ 未运行 (将自动启动)${NC}"
fi

# 检查 Git
echo -n "Git            ... "
if command -v git &> /dev/null; then
    VERSION=$(git --version | cut -d ' ' -f 3)
    echo -e "${GREEN}✓ ${VERSION}${NC}"
else
    echo -e "${RED}✗ 未安装${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo ""
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有环境依赖已就绪！${NC}"
    exit 0
else
    echo -e "${RED}❌ 发现 ${ERRORS} 个环境依赖缺失，请先安装。${NC}"
    exit 1
fi
