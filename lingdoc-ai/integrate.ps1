#Requires -Version 5.1
<#
.SYNOPSIS
    一键将 lingdoc-ai/ 的 AI 代码合并到 ling-doc/ 主项目中。
.DESCRIPTION
    该脚本用于联调前将独立管理的 AI 模块代码复制回 Gitee 主仓库骨架，
    使 ling-doc/ 成为一个包含完整 AI 功能的可运行项目。
.NOTES
    必须在 lingdoc-ai/ 目录下执行。
#>

$ErrorActionPreference = "Stop"

# ---------- 配置 ----------
$TargetPath = "../ling-doc"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# ---------- 检查目标目录 ----------
if (-not (Test-Path $TargetPath)) {
    Write-Error "找不到目标目录: $TargetPath。请确保 ling-doc/ 与 lingdoc-ai/ 位于同一父目录下。"
    exit 1
}

$TargetPath = Resolve-Path $TargetPath
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  LingDoc AI 模块一键集成脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "源: $ScriptDir" -ForegroundColor Gray
Write-Host "目标: $TargetPath" -ForegroundColor Gray
Write-Host ""

# ---------- 辅助函数 ----------
function Copy-WithLog ($Source, $Destination, $Label) {
    $src = Join-Path $ScriptDir $Source
    $dst = Join-Path $TargetPath $Destination
    if (Test-Path $src) {
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item -Path "$src\*" -Destination $dst -Recurse -Force
        Write-Host "  [OK] $Label" -ForegroundColor Green
    } else {
        Write-Host "  [SKIP] $Label (源不存在)" -ForegroundColor Yellow
    }
}

# ---------- 1. 前端代码 ----------
Write-Host "[1/4] 复制前端代码..." -ForegroundColor Cyan
Copy-WithLog "frontend/api/ai"     "src/api/ai"     "AI 基础设施 API"
Copy-WithLog "frontend/api/lingdoc" "src/api/lingdoc" "灵档业务 API"
Copy-WithLog "frontend/views/lingdoc" "src/views/lingdoc" "AI 业务页面"

# ---------- 2. 后端代码 ----------
Write-Host ""
Write-Host "[2/4] 复制后端代码..." -ForegroundColor Cyan
Copy-WithLog "backend/controller/lingdoc" "ruoyi-server/ruoyi-admin/src/main/java/com/ruoyi/web/controller/lingdoc" "Controller"
Copy-WithLog "backend/domain/lingdoc"     "ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/domain/lingdoc" "Domain 实体"
Copy-WithLog "backend/mapper/lingdoc"     "ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/mapper/lingdoc" "Mapper 接口"
Copy-WithLog "backend/service/lingdoc"    "ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc" "Service"
Copy-WithLog "backend/resources/mapper/lingdoc" "ruoyi-server/ruoyi-system/src/main/resources/mapper/lingdoc" "MyBatis XML"

# ---------- 3. 数据库脚本 ----------
Write-Host ""
Write-Host "[3/4] 复制数据库脚本..." -ForegroundColor Cyan
Copy-WithLog "sql" "ruoyi-server/sql" "SQL 脚本"

# ---------- 4. 文档（可选） ----------
Write-Host ""
Write-Host "[4/4] 复制文档..." -ForegroundColor Cyan
Copy-WithLog "docs" "docs" "AI 设计文档"

# ---------- 完成 ----------
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  集成完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "接下来请执行以下操作：" -ForegroundColor Yellow
Write-Host "  1. 检查 application.yml 中 springdoc.packages-to-scan 是否包含 com.ruoyi.web.controller.lingdoc" -ForegroundColor White
Write-Host "  2. 在 MySQL 中执行新增的 SQL 脚本（如尚未执行）" -ForegroundColor White
Write-Host "  3. 启动后端: cd ruoyi-server && mvn clean package -DskipTests" -ForegroundColor White
Write-Host "  4. 启动前端: cd ling-doc && npm run dev" -ForegroundColor White
Write-Host ""
