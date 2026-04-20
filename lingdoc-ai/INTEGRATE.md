# AI 模块与主项目联调集成指南

## 概述

`lingdoc-ai/` 是 AI 代码的独立仓库，`ling-doc/` 是 Gitee 主仓库的纯净骨架。联调时需要将 `lingdoc-ai/` 的代码临时合并到 `ling-doc/` 中，运行测试后再分别提交。

## 前置条件

- `ling-doc/` 已存在且是从 `https://gitee.com/magician336/ling-doc` clone 的纯净版，**必须处于 `fast` 分支**（`fast` 分支比 `main` 领先 20+ 个提交，已包含完整的 AI 前后端代码）。

> ⚠️ **切勿使用 `main` 分支作为开发基线**，`main` 分支缺少 AI 业务代码且落后于 `fast`。
- `lingdoc-ai/` 与 `ling-doc/` 位于同一父目录下：
  ```
  D:\lingdoc_trail\
  ├── ling-doc/      # Gitee 主仓库
  └── lingdoc-ai/    # 本仓库
  ```

## 一键集成

在 `lingdoc-ai/` 目录下打开 PowerShell，执行：

```powershell
.\integrate.ps1
```

该脚本会自动完成以下所有复制操作。

## 手动集成步骤

### 1. 复制前端代码

```powershell
# API 层
Copy-Item "frontend/api/ai/*"     "../ling-doc/src/api/ai/"     -Recurse -Force
Copy-Item "frontend/api/lingdoc/*" "../ling-doc/src/api/lingdoc/" -Recurse -Force

# 视图层
Copy-Item "frontend/views/lingdoc/*" "../ling-doc/src/views/lingdoc/" -Recurse -Force
```

### 2. 复制后端代码

```powershell
# Controller
Copy-Item "backend/controller/lingdoc/*" "../ling-doc/ruoyi-server/ruoyi-admin/src/main/java/com/ruoyi/web/controller/lingdoc/" -Recurse -Force

# Domain / Service / Mapper
Copy-Item "backend/domain/lingdoc/*"  "../ling-doc/ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/domain/lingdoc/" -Recurse -Force
Copy-Item "backend/mapper/lingdoc/*"  "../ling-doc/ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/mapper/lingdoc/" -Recurse -Force
Copy-Item "backend/service/lingdoc/*" "../ling-doc/ruoyi-server/ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc/" -Recurse -Force

# MyBatis XML
Copy-Item "backend/resources/mapper/lingdoc/*" "../ling-doc/ruoyi-server/ruoyi-system/src/main/resources/mapper/lingdoc/" -Recurse -Force
```

### 3. 复制数据库脚本

```powershell
Copy-Item "sql/*.sql" "../ling-doc/ruoyi-server/sql/" -Force
```

### 4. 复制文档（可选，用于本地参考）

```powershell
Copy-Item "docs/*" "../ling-doc/docs/" -Recurse -Force
```

## 集成后必须修改的配置

### 4.1 Swagger / SpringDoc 扫描包

文件：`ling-doc/ruoyi-server/ruoyi-admin/src/main/resources/application.yml`

找到以下配置：

```yaml
springdoc:
  group-configs:
    - group: 'default'
      display-name: '测试模块'
      paths-to-match: '/**'
      packages-to-scan: com.ruoyi.web.controller.tool
```

修改为：

```yaml
springdoc:
  group-configs:
    - group: 'default'
      display-name: '测试模块'
      paths-to-match: '/**'
      packages-to-scan: com.ruoyi.web.controller.tool,com.ruoyi.web.controller.lingdoc
```

> 否则 Swagger 页面不会显示 `/lingdoc/**` 下的 AI 接口。

### 4.2 菜单数据（首次集成）

如果 `ling-doc/ruoyi-server/sql/` 下的菜单 SQL 尚未执行，需要在 MySQL 中执行：

```sql
-- 按顺序执行
source ry_20260321.sql
source 08-form-module-mysql.sql
source 09-form-menu-update.sql
source migration_20260418_fix_lingdoc_menu.sql
source update_menu_form_assistant.sql
```

## Gitee 主仓库更新后的同步流程

当 Gitee 主仓库有更新时：

```powershell
cd ../ling-doc
git checkout fast
git pull origin fast

# 如有需要，补充 lingdoc-ai/ 中独立开发的新代码
cd ../lingdoc-ai
.\integrate.ps1
```

> 注意：`fast` 分支已经包含了 `lingdoc-ai/` 中分离出来的大部分 AI 前后端代码。`integrate.ps1` 在 `fast` 分支上通常只需要补充 `lingdoc-ai/` 独立仓库后续新增的代码。如果 Gitee 与 `lingdoc-ai/` 存在同名文件冲突，需手动解决。

## 回滚

如果集成后出现问题，可以直接删除 `ling-doc/` 重新从 Gitee clone，再执行 `integrate.ps1`。
