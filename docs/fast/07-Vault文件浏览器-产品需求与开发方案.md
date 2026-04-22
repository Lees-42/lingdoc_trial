# Vault 文件浏览器 —— 产品需求与开发方案

**文档编号**: LingDoc-FAST-007  
**版本**: v3.0  
**编制日期**: 2026-04-19  
**更新日期**: 2026-04-22
**状态**: 开发中（上传功能 ✅ / 多Vault架构 ✅ / 标签系统 ✅）  

---

## 1. 功能概述

Vault 文件浏览器是 LingDoc 的核心基础设施功能，为用户提供类似 **Windows 文件资源管理器 + Obsidian 文件树** 的本地文件浏览体验。用户可通过该功能直观地查看、检索、预览和管理已归档到 Vault 中的所有文件。

**典型使用场景**：
- 浏览自动规整后的分类目录结构，快速定位学习资料或申请材料
- 在表格填写助手中选取参考文档时，通过文件浏览器弹窗统一选择
- 预览文本文件内容，确认文件信息后再进行后续操作
- 日常文件管理：重命名、移动、删除、下载 Vault 中的文件

> **约束继承**：系统仅支持文本格式数据，不做 OCR（不处理图片/扫描件），不做信息脱敏。文件浏览器的预览功能仅面向文本文件，其他类型仅展示元数据。

---

## 2. 用户界面设计

### 2.1 整体布局

采用经典的三栏式文件管理器布局：

```
┌─────────────────────────────────────────────────────────────┐
│ 【顶部工具栏】                                                │
│ [新建文件夹] [上传文件] [刷新] │ 搜索: [________] [列表/图标]   │
├──────────┬──────────────────────────────┬───────────────────┤
│          │                              │                   │
│ 【左侧】  │ 【中间】文件列表              │ 【右侧】预览/详情  │
│ 目录树    │                              │                   │
│          │ ┌────┬────────┬──────┬─────┐ │ ┌─────────────┐   │
│ 📁学习资料│ │名称│ 类型   │大小  │日期 │ │ │ 文件名.txt   │   │
│  ├ 📁大三 │ ├────┼────────┼──────┼─────┤ │ │             │   │
│  │  ├ 📁OS│ │简历│ txt    │ 2KB  │...  │ │ │ [文本预览区] │   │
│  │  └ 📁网│ │成绩│ csv    │ 5KB  │...  │ │ │             │   │
│  └ 📁申请 │ └────┴────────┴──────┴─────┘ │ │ [ 🔍 放大预览 ]│   │
│ 📁工作文档│   共 15 个文件                 │ │             │   │
│          │                              │ │ 大小: 2KB    │   │
│          │                              │ │ 路径: ...    │   │
│          │                              │ │ 标签: 学习资料│   │
│          │                              │ └─────────────┘   │
└──────────┴──────────────────────────────┴───────────────────┘
```

### 2.2 左侧目录树

- **数据源**：从 `lingdoc_file_index` 表中按 `sub_path` 字段聚合生成树形结构
- **一级节点**：`sub_path` 第一级目录（如"学习资料"、"申请材料"、"工作文档"）
- **子节点**：`sub_path` 按 `/` 分割后的层级目录
- **交互**：
  - 单击节点 → 中间列表筛选该目录下文件
  - 右键节点 → 新建文件夹、重命名、删除空文件夹
  - 支持折叠/展开全部

### 2.3 中间文件列表

支持**列表视图**和**图标视图**两种展示模式切换：

**列表视图（默认）**：
| 列名 | 说明 |
|------|------|
| 名称 | 文件名，点击可预览 |
| 类型 | 文件扩展名（pdf/docx/xlsx/doc/xls/txt/md/csv/json/xml/yaml/png/jpg等） |
| 大小 | 自动转换（B/KB/MB） |
| 修改日期 | `update_time` 格式化显示 |
| 来源 | 手动上传 / 自动规整 / 表格助手 |

**图标视图**：
- 大图标网格排列，显示文件类型图标 + 文件名
- 双击打开预览
- 支持框选多选

**通用交互**：
- 单击行 → 右侧显示预览/详情
- 双击行 → 同单击（文本文件预览，非文本显示元数据）
- 右键行 → 上下文菜单：打开 / 下载 / 重命名 / 移动 / 删除 / 复制路径 / 作为参考文档
- 顶部搜索框 → 实时对 `file_name` + `file_content` 做 FULLTEXT 检索
- 多选 → `el-table` 多选列，支持批量删除

### 2.4 右侧预览/详情面板

**文本文件预览**：
- 使用 `el-input` `type="textarea"` readonly 渲染文本内容
- 显示行号（左侧边栏）
- 自动识别文件类型，对 CSV/JSON/XML/YAML 做语法高亮（使用 `highlight.js` 或简单 CSS 区分）
- 底部显示文件大小、路径、标签、创建时间

**非文本文件详情**：
- 显示大图标占位
- 元数据卡片：文件名、类型、大小、路径、标签、来源、创建/修改时间、checksum 前 8 位

**放大预览功能**：
- 在预览区右上角放置醒目的 **"🔍 放大预览"** 按钮（`el-button` `type="primary"` `size="large"` 带图标）
- 点击后弹出 `el-dialog` 全屏弹窗（`fullscreen` 或 `width="90%"`）
- 弹窗内功能：
  - 文本内容以更大区域展示（占满弹窗 80% 高度）
  - **字体大小调节**：小 / 中 / 大 三档切换（`el-radio-group`）
  - **换行切换**：自动换行 / 不换行（`el-switch`）
  - **搜索高亮**：输入关键词后高亮匹配文本（`el-input` + CSS `background-color`）
  - 支持 `Ctrl+F` 唤起搜索框
  - 底部显示文件元数据摘要
  - `ESC` 或点击关闭按钮退出放大预览

---

## 3. 数据库设计

### 3.1 架构说明（v3.0 更新）

自 2026-04-22 起，Vault 文件浏览器的数据库架构已从**中心 MySQL** 迁移到**各 Vault 独立的 SQLite**。

| 数据库 | 位置 | 存放表 | 说明 |
|--------|------|--------|------|
| 中心 MySQL | `ruoyi` 库 | `lingdoc_user_repo` | 仅仓库注册配置 |
| Vault SQLite | `{vault_root}/.lingdoc/vault.db` | `lingdoc_file_index`、`lingdoc_file_version`、`lingdoc_tag`、`lingdoc_tag_binding` 等 | 所有 Vault 业务表 |

**SQLite 初始化脚本**：`ruoyi-server/sql/13-vault-sqlite.sql`
- 创建新仓库时，后端 `VaultDbInitializer` 自动执行
- 启用 WAL 模式提升并发性能
- 含触发器自动更新 `update_time`

### 3.2 表结构概览

| 表名 | 说明 | 记录数预估 | 状态 |
|------|------|-----------|------|
| `lingdoc_file_index` | Vault 文件主索引表 | 单用户 1,000~10,000 条 | ✅ **已创建**（`13-vault-sqlite.sql`） |
| `lingdoc_file_version` | 文件版本记录表 | 单文件 0~10 条 | ✅ **已创建**（`13-vault-sqlite.sql`） |
| `lingdoc_tag` | 标签定义表 | 单用户 10~100 条 | ✅ **已创建**（`13-vault-sqlite.sql`） |
| `lingdoc_tag_binding` | 标签绑定表 | 单用户 100~1,000 条 | ✅ **已创建**（`13-vault-sqlite.sql`） |

> **重要说明**：`09-vault-module-mysql.sql` 已废弃删除，所有 Vault 业务表现由各 Vault 的 SQLite 数据库管理。详见 `docs/fast/08-文件管理-数据层与用户配置设计.md`。

### 3.3 Vault 文件索引表 `lingdoc_file_index`

> **实现说明**：以下建表语句为**设计参考**。实际运行时由后端 `VaultDbInitializer` 自动执行 `13-vault-sqlite.sql` 创建 SQLite 表结构。

```sql
-- ============================================================
-- Vault 文件浏览器模块数据库脚本（SQLite 3.45+）
-- 版本: v1.0
-- 数据库: SQLite 3.45+
-- 执行时机: 创建新 Vault 时自动执行
-- 文件位置: {vault_root}/.lingdoc/vault.db
-- ============================================================

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;
PRAGMA encoding = 'UTF-8';

CREATE TABLE IF NOT EXISTS `lingdoc_file_index` (
  `file_id`         TEXT    NOT NULL PRIMARY KEY,
  `user_id`         INTEGER NOT NULL,
  `file_name`       TEXT    NOT NULL,
  `vault_path`      TEXT    NOT NULL,
  `abs_path`        TEXT    NOT NULL,
  `file_type`       TEXT    NOT NULL,
  `file_size`       INTEGER NOT NULL,
  `checksum`        TEXT    NOT NULL,
  `sub_path`        TEXT    DEFAULT NULL,
  `source_type`     TEXT    DEFAULT '0',
  `file_content`    TEXT    DEFAULT NULL,
  `content_path`    TEXT    DEFAULT NULL,
  `content_size`    INTEGER DEFAULT 0,
  `is_desensitized` TEXT    DEFAULT '0',
  `create_time`     TEXT    DEFAULT (datetime('now','localtime')),
  `update_time`     TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_file_index_user_id` ON `lingdoc_file_index` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_file_index_file_type` ON `lingdoc_file_index` (`file_type`);
CREATE INDEX IF NOT EXISTS `idx_file_index_source_type` ON `lingdoc_file_index` (`source_type`);
CREATE INDEX IF NOT EXISTS `idx_file_index_checksum` ON `lingdoc_file_index` (`checksum`);
CREATE INDEX IF NOT EXISTS `idx_file_index_user_checksum` ON `lingdoc_file_index` (`user_id`, `checksum`);
CREATE INDEX IF NOT EXISTS `idx_file_index_user_type_time` ON `lingdoc_file_index` (`user_id`, `file_type`, `create_time`);
CREATE INDEX IF NOT EXISTS `idx_file_index_name` ON `lingdoc_file_index` (`file_name`);

-- 触发器：自动更新 update_time
CREATE TRIGGER IF NOT EXISTS `trg_file_index_update_time`
AFTER UPDATE ON `lingdoc_file_index`
FOR EACH ROW
BEGIN
  UPDATE `lingdoc_file_index` SET `update_time` = datetime('now','localtime') WHERE `file_id` = NEW.file_id;
END;
```

### 3.4 索引设计说明

| 索引名 | 类型 | 字段 | 用途 |
|--------|------|------|------|
| PRIMARY | 主键 | `file_id` | 单文件查询 |
| idx_file_index_user_id | 普通 | `user_id` | 用户隔离查询 |
| idx_file_index_file_type | 普通 | `file_type` | 按类型筛选 |
| idx_file_index_source_type | 普通 | `source_type` | 按来源筛选 |
| idx_file_index_checksum | 普通 | `checksum` | 去重校验 |
| idx_file_index_user_checksum | 联合 | `user_id`, `checksum` | 同用户下文件去重 |
| idx_file_index_user_type_time | 联合 | `user_id`, `file_type`, `create_time` | 用户+类型+时间排序 |
| idx_file_index_name | 普通 | `file_name` | 按文件名搜索 |

> **全文检索说明**：SQLite 标准版未启用 `FTS5` 扩展，当前全文检索通过 `LIKE` 模糊匹配实现。MVP 阶段数据量（万级）下性能可接受。后续若需增强，可引入 `FTS5` 虚拟表。

### 3.5 文件版本记录表 `lingdoc_file_version`

**作用**：记录文件的历史版本快照，支撑版本溯源功能。

```sql
CREATE TABLE IF NOT EXISTS `lingdoc_file_version` (
  `version_id`     TEXT    NOT NULL PRIMARY KEY,
  `file_id`        TEXT    NOT NULL,
  `version_no`     INTEGER NOT NULL,
  `snapshot_path`  TEXT    NOT NULL,
  `snapshot_size`  INTEGER DEFAULT NULL,
  `operation_type` TEXT    DEFAULT '0',
  `checksum`       TEXT    DEFAULT NULL,
  `operator_id`    INTEGER DEFAULT NULL,
  `create_time`    TEXT    DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS `idx_file_version_file_id` ON `lingdoc_file_version` (`file_id`);
CREATE INDEX IF NOT EXISTS `idx_file_version_version_no` ON `lingdoc_file_version` (`version_no`);
CREATE INDEX IF NOT EXISTS `idx_file_version_create_time` ON `lingdoc_file_version` (`create_time`);
```

**索引设计说明**：

| 索引名 | 类型 | 字段 | 用途 |
|--------|------|------|------|
| PRIMARY | 主键 | `version_id` | 单版本查询 |
| idx_file_id | 普通 | `file_id` | 查询某文件的所有版本 |
| idx_version_no | 普通 | `version_no` | 按版本号排序 |
| idx_create_time | 普通 | `create_time` | 按时间排序 |

### 3.6 与现有数据库的兼容性说明

`lingdoc_file_index` 最初由 `09-vault-module-mysql.sql`（MySQL）创建。自 2026-04-22 架构变更后：
- `09-vault-module-mysql.sql` 已**废弃删除**
- 所有 Vault 业务表迁移到各仓库的 **SQLite** 数据库
- 现有 MySQL 中的旧业务数据可通过 `POST /lingdoc/vault/migrate` 接口迁移到对应 Vault 的 SQLite
- 新仓库创建时，`VaultDbInitializer` 自动执行 `13-vault-sqlite.sql` 创建表结构

#### ID 统一原则

**`lingdoc_file_index` 是 Vault 中所有文件的唯一主索引**。AI 知识库处理状态通过独立的 `lingdoc_file_ai_meta` 表（脚手架，由 AI 开发同学完善）以 `file_id` 1:1 关联，不再使用 `kb_document` 概念。

**规则**：
1. 所有文件必须先进入 `lingdoc_file_index`（入 Vault 即索引），获得 `file_id`。
2. 删除文件时，先删 `lingdoc_file_ai_meta`（AI 元数据），再删 `lingdoc_file_index`（主索引）及级联版本记录。
3. **`lingdoc_form_reference.doc_id`** 直接指向 `lingdoc_file_index.file_id`。

### 3.6 数据同步策略

`lingdoc_file_index` 表的数据来源：

| 来源场景 | 同步方式 | 触发时机 |
|---------|---------|---------|
| 自动规整归档 | 后端 Service 在文件移动完成后 INSERT | 规整流程最后一步 |
| 表格助手生成 | 后端 Controller 在文件保存后 INSERT | 生成填写结果后 |
| 手动上传（文件浏览器内） | 文件浏览器上传 API 同步 INSERT | 上传完成后 |
| 外部文件直接放入 Vault | 手动触发 `/lingdoc/vault/sync` 扫描 | 用户点击"刷新"或定时任务 |

### 3.7 标签体系 ✅ 已实现

> **设计详情**：见 FAST-008《文件管理 —— 数据层与用户配置设计》第2节。

**核心变更**：标签已从 `lingdoc_file_index` 中解耦，改为独立的标签体系：
- `lingdoc_tag`：标签定义表（存放于各 Vault 的 SQLite）
- `lingdoc_tag_binding`：标签绑定表（支持目录标签继承）

目录树不再依赖 `tag` 字段，直接从 `sub_path` 聚合生成。

**实现状态**：
- ✅ 后端：`LingdocTagController` / `LingdocTagServiceImpl` 完整实现
- ✅ 前端：`VaultFileList` / `VaultFilePreview` / `VaultFileTree` / `upload/index.vue` 均集成标签能力
- ✅ 目录标签继承：文件自动继承父目录标签，继承标签以灰色样式展示
- ✅ 已修复 6 个已知 bug（详见 `docs/fast/05-六日开发排期.md` 追加任务）

---

## 4. 后端接口规范

### 4.1 通用请求约定

所有 `/lingdoc/vault/` 接口均需携带 `X-Vault-Path` HTTP Header，指定当前操作的 Vault 仓库路径。后端据此路由到对应的 SQLite 数据源。

```
X-Vault-Path: D:/LingDocVault
```

前端 `src/utils/request.js` 会自动为所有 `/lingdoc/` 前缀的请求注入该 Header（从 `vaultStore.currentVaultPath` 读取）。

### 4.2 接口清单

> **实现状态**：`LingdocVaultController` 已存在，核心浏览/管理接口已上线；上传接口为新实现。

| 序号 | 方法 | 路径 | 说明 | 权限标识 | 实现状态 |
|------|------|------|------|---------|---------|
| 1 | GET | `/lingdoc/vault/tree` | 获取目录树 | `lingdoc:vault:list` | ✅ 已实现 |
| 2 | GET | `/lingdoc/vault/files` | 分页查询文件列表 | `lingdoc:vault:list` | ✅ 已实现 |
| 3 | GET | `/lingdoc/vault/file/{fileId}` | 获取单个文件详情 | `lingdoc:vault:list` | ✅ 已实现 |
| 4 | GET | `/lingdoc/vault/file/{fileId}/content` | 获取文本文件内容 | `lingdoc:vault:list` | ✅ 已实现 |
| 5 | GET | `/lingdoc/vault/file/{fileId}/download` | 下载文件 | `lingdoc:vault:download` | ✅ 已实现 |
| 6 | PUT | `/lingdoc/vault/file/{fileId}/rename` | 重命名文件 | `lingdoc:vault:edit` | ✅ 已实现 |
| 7 | PUT | `/lingdoc/vault/file/{fileId}/move` | 移动文件 | `lingdoc:vault:edit` | ✅ 已实现 |
| 8 | DELETE | `/lingdoc/vault/file/{fileId}` | 删除文件 | `lingdoc:vault:delete` | ✅ 已实现 |
| 9 | POST | `/lingdoc/vault/folder` | 新建文件夹 | `lingdoc:vault:edit` | ✅ 已实现 |
| 10 | POST | `/lingdoc/vault/sync` | 手动触发 Vault 扫描同步 | `lingdoc:vault:edit` | ✅ 已实现 |
| 11 | POST | `/lingdoc/vault/upload` | 上传文件到 Vault | `lingdoc:vault:edit` | ✅ **已实现** |
| 12 | POST | `/lingdoc/vault/migrate` | 数据迁移（MySQL → SQLite） | `lingdoc:vault:edit` | ✅ **已实现** |

### 4.2 获取目录树

```
GET /lingdoc/vault/tree
```

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "label": "学习资料",
      "value": "学习资料",
      "children": [
        {
          "label": "大三上",
          "value": "学习资料/大三上",
          "children": [
            { "label": "操作系统", "value": "学习资料/大三上/操作系统" }
          ]
        }
      ]
    },
    {
      "label": "申请材料",
      "value": "申请材料",
      "children": []
    }
  ]
}
```

**实现说明**：后端查询 `SELECT DISTINCT sub_path FROM lingdoc_file_index WHERE user_id = ?`，按 `/` 分割 `sub_path` 提取第一级作为根节点，后续层级构建子树。

### 4.3 分页查询文件列表

```
GET /lingdoc/vault/files?pageNum=1&pageSize=20&subPath=学习资料/大三上&fileType=txt&keyword=简历
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |
| subPath | string | 否 | 子路径筛选（如 `学习资料/大三上`） |
| fileType | string | 否 | 文件类型筛选（逗号分隔多选） |
| keyword | string | 否 | 搜索关键词（FULLTEXT 检索） |
| sourceType | string | 否 | 来源筛选：0/1/2 |

**响应**：标准 `TableDataInfo` 分页结构
```json
{
  "code": 200,
  "msg": "操作成功",
  "rows": [
    {
      "fileId": "f_abc123...",
      "fileName": "个人简历_20260417.txt",
      "fileType": "txt",
      "fileSize": 2048,
      "fileSizeText": "2KB",
      "subPath": "学习资料/大三上",
      "subPath": "大三上/操作系统",
      "sourceType": "1",
      "sourceTypeName": "自动规整",
      "createTime": "2026-04-17 14:30:00",
      "updateTime": "2026-04-17 14:30:00"
    }
  ],
  "total": 15
}
```

### 4.4 获取文件详情

```
GET /lingdoc/vault/file/{fileId}
```

**响应**：返回完整字段（含 `abs_path`、`checksum`、`file_content` 前 500 字摘要）

### 4.5 获取文本文件内容

```
GET /lingdoc/vault/file/{fileId}/content
```

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "content": "文件完整文本内容...",
    "encoding": "UTF-8",
    "lineCount": 128
  }
}
```

**安全限制**：`file_type` 为 txt/md/csv/json/xml/yaml 时直接返回文本内容；其他类型（PDF/Word/Excel/图片等）返回 403，前端引导用户下载后通过系统默认程序打开。

### 4.6 下载文件

```
GET /lingdoc/vault/file/{fileId}/download
```

**响应**：`application/octet-stream`，使用 `FileUtils.setAttachmentResponseHeader`

### 4.7 重命名文件

```
PUT /lingdoc/vault/file/{fileId}/rename
Content-Type: application/json

{ "newName": "新文件名.txt" }
```

**业务规则**：
- 校验新文件名合法（正则 `[a-zA-Z0-9_\-|\.\u4e00-\u9fa5]+`）
- 校验扩展名不变（仅支持文本格式）
- 物理文件重命名 + 数据库 `file_name`、`vault_path`、`abs_path` 更新

### 4.8 移动文件

```
PUT /lingdoc/vault/file/{fileId}/move
Content-Type: application/json

{ "targetTag": "申请材料", "targetSubPath": "奖学金/2026" }
```

**业务规则**：
- 目标目录不存在则自动创建
- 物理文件移动 + 数据库 `sub_path`、`vault_path`、`abs_path` 更新

### 4.9 删除文件

```
DELETE /lingdoc/vault/file/{fileId}
```

**业务规则**：
- 软删除：数据库设置 `del_flag`（若表未设计则直接 DELETE）
- 物理删除 Vault 中的文件
- 若文件被表格助手任务引用，拒绝删除并提示

### 4.10 新建文件夹

```
POST /lingdoc/vault/folder
Content-Type: application/json

{ "subPath": "学习资料/大四/毕业论文" }
```

**业务规则**：仅创建物理目录，不在 `lingdoc_file_index` 中插入记录（目录本身不是文件）。

### 4.11 手动触发 Vault 扫描同步

```
POST /lingdoc/vault/sync
```

**业务逻辑**：
1. 扫描 `vault/{user_uuid}/documents/` 目录下所有文件（不限格式）
2. 对每个文件计算 SHA256 checksum
3. 与 `lingdoc_file_index` 中现有记录比对：
   - 新文件 → INSERT
   - checksum 变更 → UPDATE `file_content`、`file_size`、`update_time`
   - 数据库中有但物理文件不存在 → DELETE（或标记已删除）
4. 返回同步统计：新增 / 更新 / 删除 数量

### 4.12 上传文件

```
POST /lingdoc/vault/upload
Content-Type: multipart/form-data
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 上传的文件 |
| subPath | string | 否 | 目标子目录（如 `学习资料/大三上`），默认为根目录 |

**响应**：返回插入后的 `LingdocFileIndex` 记录
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "fileId": "f_xxx...",
    "fileName": "简历.txt",
    "fileSize": 2048,
    "fileType": "txt",
    "subPath": "学习资料/大三上",
    "vaultPath": "学习资料/大三上/简历.txt",
    "checksum": "a1b2c3...",
    "sourceType": "0"
  }
}
```

**业务规则**：
- 文件保存到 `{repo}/documents/{subPath}/`
- 自动计算 SHA256 checksum
- 文本文件（txt/md/csv/json/xml/yaml）自动读取内容写入 `file_content`
- 重复 checksum 允许上传（`checksum_count` 累加，不拒绝）
- 单文件大小限制继承 Spring Boot 配置（默认 10MB）

---

## 5. 前端组件设计

### 5.1 页面路由

```javascript
// src/router/index.js 动态路由或后端菜单配置
{
  path: '/lingdoc/vault',
  component: Layout,
  hidden: false,
  children: [
    {
      path: 'index',
      component: () => import('@/views/lingdoc/vault/index'),
      name: 'VaultBrowser',
      meta: { title: '文件浏览器', icon: 'folder' }
    }
  ]
}
```

### 5.2 组件拆分

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| `VaultBrowser` | `src/views/lingdoc/vault/index.vue` | 主页面，三栏布局协调 |
| `VaultToolbar` | `src/views/lingdoc/vault/components/VaultToolbar.vue` | 顶部工具栏：新建、上传、刷新、搜索、视图切换 |
| `VaultFileTree` | `src/views/lingdoc/vault/components/VaultFileTree.vue` | 左侧目录树：`el-tree` 渲染，支持右键菜单 |
| `VaultFileList` | `src/views/lingdoc/vault/components/VaultFileList.vue` | 中间文件列表：`el-table` / 图标网格，支持多选、右键 |
| `VaultFilePreview` | `src/views/lingdoc/vault/components/VaultFilePreview.vue` | 右侧预览面板：文本预览 + 元数据卡片 + **放大预览按钮** |
| `VaultPreviewDialog` | `src/views/lingdoc/vault/components/VaultPreviewDialog.vue` | 放大预览弹窗：`el-dialog` 全屏，字体/换行/搜索 |
| `VaultContextMenu` | `src/views/lingdoc/vault/components/VaultContextMenu.vue` | 右键上下文菜单（文件/目录两种模式） |

### 5.3 状态管理

#### 页面级状态（VaultBrowser）

```javascript
// VaultBrowser/index.vue
const state = reactive({
  // 目录树
  treeData: [],
  treeLoading: false,
  selectedNode: null,

  // 文件列表
  fileList: [],
  fileLoading: false,
  total: 0,
  queryParams: { pageNum: 1, pageSize: 20, subPath: '', keyword: '' },
  viewMode: 'list', // 'list' | 'icon'
  selectedFiles: [],

  // 预览
  currentFile: null,
  previewContent: '',
  previewLoading: false,

  // 放大预览弹窗
  previewDialogVisible: false,
  previewDialogConfig: { fontSize: 'medium', wordWrap: true, searchKeyword: '' },

  // 标签系统（v3.0 新增）
  tagList: [],           // 当前 Vault 的所有标签定义
  fileTags: {},          // 文件ID → 标签列表映射
  folderTagMap: {}       // 目录路径 → 标签列表映射
})
```

#### 全局 Vault Store（Pinia）

```javascript
// src/store/modules/vault.js
const useVaultStore = defineStore('vault', {
  state: () => ({
    currentVaultPath: localStorage.getItem('lingdoc-current-vault-path') || '',
    currentRepo: null,
    repoList: []
  }),
  getters: {
    currentRepoName(state) {
      return state.currentRepo?.repoName || '默认仓库'
    }
  },
  actions: {
    async loadRepos() { /* 加载用户仓库列表 */ },
    setCurrentVault(repo) { /* 切换仓库并持久化到 localStorage */ },
    clearVault() { /* 退出登录时清理 */ }
  }
})
```

**关键说明**：
- `vaultStore.currentVaultPath` 持久化在 `localStorage`，刷新页面后自动恢复
- 所有 `/lingdoc/` API 请求自动携带 `X-Vault-Path` Header（由 `request.js` 拦截器注入）
- 切换仓库时，`vaultStore.setCurrentVault()` 更新路径并触发页面数据重载

### 5.4 API 封装

```javascript
// src/api/lingdoc/vault.js
import request from '@/utils/request'

// ==================== Vault 文件管理 ====================
export const getVaultTree = () => request({ url: '/lingdoc/vault/tree', method: 'get' })
export const listVaultFiles = (query) => request({ url: '/lingdoc/vault/files', method: 'get', params: query })
export const getVaultFile = (fileId) => request({ url: `/lingdoc/vault/file/${fileId}`, method: 'get' })
export const getVaultFileContent = (fileId) => request({ url: `/lingdoc/vault/file/${fileId}/content`, method: 'get' })
export const downloadVaultFile = (fileId) => request({ url: `/lingdoc/vault/file/${fileId}/download`, method: 'get', responseType: 'blob' })
export const renameVaultFile = (fileId, data) => request({ url: `/lingdoc/vault/file/${fileId}/rename`, method: 'put', data })
export const moveVaultFile = (fileId, data) => request({ url: `/lingdoc/vault/file/${fileId}/move`, method: 'put', data })
export const deleteVaultFile = (fileId) => request({ url: `/lingdoc/vault/file/${fileId}`, method: 'delete' })
export const createVaultFolder = (data) => request({ url: '/lingdoc/vault/folder', method: 'post', data })
export const syncVault = () => request({ url: '/lingdoc/vault/sync', method: 'post' })
export const uploadVaultFile = (data) => request({ url: '/lingdoc/vault/upload', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
export const migrateVaultData = () => request({ url: '/lingdoc/vault/migrate', method: 'post' })

// ==================== Vault 仓库管理 ====================
export const listVaultRepos = () => request({ url: '/lingdoc/vault/repos', method: 'get' })
export const getVaultRepo = (repoId) => request({ url: `/lingdoc/vault/repo/${repoId}`, method: 'get' })
export const createVaultRepo = (data) => request({ url: '/lingdoc/vault/repo', method: 'post', data })
export const setDefaultVaultRepo = (repoId) => request({ url: `/lingdoc/vault/repo/${repoId}/default`, method: 'put' })

// ==================== 标签系统 ====================
export const listVaultTags = () => request({ url: '/lingdoc/tag/list', method: 'get' })
export const createVaultTag = (data) => request({ url: '/lingdoc/tag', method: 'post', data })
export const getFileTags = (fileId) => request({ url: `/lingdoc/tag/file/${fileId}`, method: 'get' })
export const bindVaultTag = (data) => request({ url: '/lingdoc/tag/bind', method: 'post', data })
export const unbindVaultTag = (bindingId) => request({ url: `/lingdoc/tag/bind/${bindingId}`, method: 'delete' })
export const getInheritedTags = (targetType, targetId) => request({
  url: '/lingdoc/tag/inherited',
  method: 'get',
  params: { targetType, targetId }
})
```

> **X-Vault-Path 自动注入**：上述所有接口均无需手动设置 `X-Vault-Path`，`request.js` 拦截器会自动从 `vaultStore.currentVaultPath` 读取并注入请求头。

---

## 6. 与现有功能衔接

### 6.1 自动规整

| 衔接点 | 说明 |
|--------|------|
| 数据写入 | 自动规整完成后，将规整后的文件信息 INSERT 到 `lingdoc_file_index` |
| 字段映射 | `sub_path` = AI 生成的完整分类路径（如 `学习资料/大三上/操作系统`），`source_type` = '1' |
| 内容提取 | 规整时同步读取文本文件内容写入 `file_content`，供全文检索 |

### 6.2 表格填写助手

| 衔接点 | 说明 |
|--------|------|
| 参考文档选择 | 表格助手的"选择参考文档"弹窗改为调用文件浏览器的组件/接口 |
| 右键快捷操作 | 文件浏览器中右键文件 → "作为表格助手参考文档"，跳转表格助手页面并传入 file_id |
| 文件关联 | `lingdoc_form_reference` 表的 `doc_id` 直接指向 `lingdoc_file_index.file_id` |

### 6.3 版本溯源

| 衔接点 | 说明 |
|--------|------|
| 入口集成 | 文件浏览器中右键文件 → "查看版本历史"，跳转 `/lingdoc/version/index?fileId=xxx` |
| 数据打通 | 版本溯源页面的文件列表从 `lingdoc_file_index` 获取，替代 mock 数据 |

### 6.4 智能检索

| 衔接点 | 说明 |
|--------|------|
| 索引复用 | 智能检索的 Vault 文件搜索直接查询 `lingdoc_file_index` 的 FULLTEXT 索引 |
| 结果跳转 | 检索结果点击后打开文件浏览器并定位到该文件 |

---

## 7. 菜单与权限配置

### 7.1 菜单 SQL

```sql
-- ============================================================
-- Vault 文件浏览器菜单初始化脚本
-- 菜单父节点：2000 灵档
-- 执行前提：lingdoc_file_index 表已创建
-- ============================================================

INSERT INTO sys_menu 
VALUES('2005', '文件浏览器', '2000', '5', 'vault', 'lingdoc/vault/index', '', '', 1, 0, 'C', '0', '0', 
       'lingdoc:vault:list,lingdoc:vault:download,lingdoc:vault:edit,lingdoc:vault:delete', 
       'folder', 'admin', sysdate(), '', null, 'Vault文件浏览器菜单：浏览本地Vault中的所有文件，支持预览、搜索、管理');
```

### 7.2 权限说明

| 权限标识 | 说明 | 前端控制 |
|---------|------|---------|
| `lingdoc:vault:list` | 浏览文件列表和目录树 | 页面访问、搜索、预览 |
| `lingdoc:vault:download` | 下载文件 | 下载按钮 `v-hasPermi` |
| `lingdoc:vault:edit` | 重命名、移动、新建文件夹 | 编辑类按钮 `v-hasPermi` |
| `lingdoc:vault:delete` | 删除文件 | 删除按钮 `v-hasPermi` |

---

## 8. 开发优先级建议

| 优先级 | 任务 | 依赖 | 说明 |
|--------|------|------|------|
| P0 | `lingdoc_file_index` 建表 + 同步逻辑 | 无 | 所有功能的数据基础，必须先完成 |
| P0 | `GET /lingdoc/vault/tree` + `GET /lingdoc/vault/files` | 建表 | 核心浏览能力 |
| P0 | 前端三栏布局 + 目录树 + 文件列表 | 上述 API | 最小可用界面 |
| P1 | `GET /lingdoc/vault/file/{fileId}/content` + 右侧预览 | P0 | 文本预览是核心体验 |
| P1 | **放大预览弹窗**（字体/换行/搜索） | 预览 API | 明确需求的功能点 |
| P1 | 下载 + 删除 | P0 | 基础文件操作 |
| P2 | 重命名 + 移动 + 新建文件夹 | P1 | 文件管理增强 |
| P2 | 搜索（FULLTEXT）+ 视图切换 | P0 | 体验优化 |
| P3 | 与表格助手/版本溯源的功能衔接 | P1 | 跨模块集成 |
| P3 | `POST /lingdoc/vault/sync` 手动同步 | P0 | 兜底同步机制 |

---

## 附录 A：与 FAST-006/008 的衔接说明

本文档复用 `FAST-008` 中提出的 Vault 文件浏览器功能设计，在数据库层面做了以下**现实化修正**：

1. **表状态标注**：`lingdoc_file_index` 已由 `13-vault-sqlite.sql` 创建并投入实际使用（SQLite，非 MySQL）
2. **兼容性分析**：说明 `lingdoc_file_index` 作为统一文件索引的定位，以及 `lingdoc_file_ai_meta` 脚手架的关联设计
3. **实现状态标注**：`LingdocVaultController`、`LingdocVaultService`、`LingdocVaultMapper` 均已实现并上线；上传接口（`POST /lingdoc/vault/upload`）为最新补充功能

**与 FAST-006 的关系**：
- `FAST-006` 分析了当前数据库中**缺少** `lingdoc_file_index` 的问题
- 本文档（`FAST-007`）提供**解决方案**：完整的建表 SQL + 接口设计 + 前端方案
- 执行顺序：建表 SQL 已执行 → 接口和前端已按第 8 节优先级逐批实现，上传功能为最新增量交付

## 附录 B：变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-04-19 | 初稿：Vault 文件浏览器产品需求与开发方案 |
| v2.0 | 2026-04-19 | 更新：上传功能已实现，补充上传接口和菜单配置 |
| v3.0 | 2026-04-22 | **重大架构变更**：数据库从 MySQL 迁移到 SQLite；引入多Vault架构（X-Vault-Path）；标签系统状态更新为已实现；补充 Vault Store、仓库管理接口、数据迁移接口 |
