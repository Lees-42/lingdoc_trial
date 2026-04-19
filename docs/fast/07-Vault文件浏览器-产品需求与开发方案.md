# Vault 文件浏览器 —— 产品需求与开发方案

**文档编号**: LingDoc-FAST-007  
**版本**: v1.0  
**编制日期**: 2026-04-19  
**状态**: 开发中  

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

- **数据源**：从 `lingdoc_file_index` 表中按 `tag` + `sub_path` 字段聚合生成树形结构
- **一级节点**：`tag` 字段值（如"学习资料"、"申请材料"、"工作文档"）
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
| 类型 | 文件扩展名（txt/md/csv/json/xml/yaml） |
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

### 3.1 表结构概览

| 表名 | 说明 | 记录数预估 |
|------|------|-----------|
| `lingdoc_file_index` | Vault 文件主索引表 | 单用户 1,000~10,000 条 |

### 3.2 Vault 文件索引表 `lingdoc_file_index`

复用 `FAST-006` 中提出的设计，补充完整建表语句：

```sql
CREATE TABLE `lingdoc_file_index` (
  `file_id`       varchar(64)   NOT NULL COMMENT '文件唯一ID（UUID）',
  `user_id`       bigint(20)    NOT NULL COMMENT '所属用户ID（sys_user.user_id）',
  `file_name`     varchar(256)  NOT NULL COMMENT '文件名（含扩展名）',
  `vault_path`    varchar(512)  NOT NULL COMMENT 'Vault内相对路径（如 documents/学习资料/大三上/）',
  `abs_path`      varchar(512)  NOT NULL COMMENT '绝对路径',
  `file_type`     varchar(32)   NOT NULL COMMENT '文件类型（txt/md/csv/json/xml/yaml）',
  `file_size`     bigint(20)    NOT NULL COMMENT '文件大小（字节）',
  `checksum`      varchar(64)   NOT NULL COMMENT 'SHA256（去重用）',
  `tag`           varchar(128)  DEFAULT NULL COMMENT '一级目录标签（学习资料/申请材料/工作文档）',
  `sub_path`      varchar(512)  DEFAULT NULL COMMENT '子分类路径（相对于tag，如 大三上/操作系统/）',
  `source_type`   char(1)       DEFAULT '0' COMMENT '来源：0手动上传 1自动规整 2表格助手生成',
  `file_content`  longtext      DEFAULT NULL COMMENT '文件文本内容（全文检索用）',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_tag` (`tag`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_checksum` (`checksum`),
  UNIQUE KEY `uk_user_checksum` (`user_id`, `checksum`),
  KEY `idx_user_tag` (`user_id`, `tag`),
  KEY `idx_user_type_time` (`user_id`, `file_type`, `create_time`),
  FULLTEXT KEY `ft_name_content` (`file_name`, `file_content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Vault文件索引表';
```

### 3.3 索引设计说明

| 索引名 | 类型 | 字段 | 用途 |
|--------|------|------|------|
| PRIMARY | 主键 | `file_id` | 单文件查询 |
| idx_user_id | 普通 | `user_id` | 用户隔离查询 |
| idx_file_type | 普通 | `file_type` | 按类型筛选 |
| idx_tag | 普通 | `tag` | 按标签筛选 |
| idx_source_type | 普通 | `source_type` | 按来源筛选 |
| idx_checksum | 普通 | `checksum` | 去重校验 |
| uk_user_checksum | 唯一 | `user_id`, `checksum` | 同用户下文件去重 |
| idx_user_tag | 联合 | `user_id`, `tag` | 用户+标签联合查询（目录树常用） |
| idx_user_type_time | 联合 | `user_id`, `file_type`, `create_time` | 用户+类型+时间排序 |
| ft_name_content | 全文 | `file_name`, `file_content` | MySQL Ngram 全文检索 |

### 3.4 数据同步策略

`lingdoc_file_index` 表的数据来源：

| 来源场景 | 同步方式 | 触发时机 |
|---------|---------|---------|
| 自动规整归档 | 后端 Service 在文件移动完成后 INSERT | 规整流程最后一步 |
| 表格助手生成 | 后端 Controller 在文件保存后 INSERT | 生成填写结果后 |
| 手动上传（文件浏览器内） | 文件浏览器上传 API 同步 INSERT | 上传完成后 |
| 外部文件直接放入 Vault | 手动触发 `/lingdoc/vault/sync` 扫描 | 用户点击"刷新"或定时任务 |

---

## 4. 后端接口规范

### 4.1 接口清单

| 序号 | 方法 | 路径 | 说明 | 权限标识 |
|------|------|------|------|---------|
| 1 | GET | `/lingdoc/vault/tree` | 获取目录树 | `lingdoc:vault:list` |
| 2 | GET | `/lingdoc/vault/files` | 分页查询文件列表 | `lingdoc:vault:list` |
| 3 | GET | `/lingdoc/vault/file/{fileId}` | 获取单个文件详情 | `lingdoc:vault:list` |
| 4 | GET | `/lingdoc/vault/file/{fileId}/content` | 获取文本文件内容 | `lingdoc:vault:list` |
| 5 | GET | `/lingdoc/vault/file/{fileId}/download` | 下载文件 | `lingdoc:vault:download` |
| 6 | PUT | `/lingdoc/vault/file/{fileId}/rename` | 重命名文件 | `lingdoc:vault:edit` |
| 7 | PUT | `/lingdoc/vault/file/{fileId}/move` | 移动文件 | `lingdoc:vault:edit` |
| 8 | DELETE | `/lingdoc/vault/file/{fileId}` | 删除文件 | `lingdoc:vault:delete` |
| 9 | POST | `/lingdoc/vault/folder` | 新建文件夹 | `lingdoc:vault:edit` |
| 10 | POST | `/lingdoc/vault/sync` | 手动触发 Vault 扫描同步 | `lingdoc:vault:edit` |

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

**实现说明**：后端查询 `SELECT DISTINCT tag, sub_path FROM lingdoc_file_index WHERE user_id = ?`，按 `/` 分割 `sub_path` 构建树形结构返回。

### 4.3 分页查询文件列表

```
GET /lingdoc/vault/files?pageNum=1&pageSize=20&tag=学习资料&subPath=大三上&fileType=txt&keyword=简历
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |
| tag | string | 否 | 一级目录标签筛选 |
| subPath | string | 否 | 子路径筛选（相对于 tag） |
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
      "tag": "学习资料",
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

**安全限制**：仅当 `file_type` 为 txt/md/csv/json/xml/yaml 时返回内容，其他类型返回 403。

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
- 物理文件移动 + 数据库 `tag`、`sub_path`、`vault_path`、`abs_path` 更新

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

{ "tag": "学习资料", "subPath": "大四/毕业论文" }
```

**业务规则**：仅创建物理目录，不在 `lingdoc_file_index` 中插入记录（目录本身不是文件）。

### 4.11 手动触发 Vault 扫描同步

```
POST /lingdoc/vault/sync
```

**业务逻辑**：
1. 扫描 `vault/{user_uuid}/documents/` 目录下所有文本文件
2. 对每个文件计算 SHA256 checksum
3. 与 `lingdoc_file_index` 中现有记录比对：
   - 新文件 → INSERT
   - checksum 变更 → UPDATE `file_content`、`file_size`、`update_time`
   - 数据库中有但物理文件不存在 → DELETE（或标记已删除）
4. 返回同步统计：新增 / 更新 / 删除 数量

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

使用页面级 reactive state（不引入 Pinia store）：

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
  queryParams: { pageNum: 1, pageSize: 20, tag: '', subPath: '', keyword: '' },
  viewMode: 'list', // 'list' | 'icon'
  selectedFiles: [],

  // 预览
  currentFile: null,
  previewContent: '',
  previewLoading: false,

  // 放大预览弹窗
  previewDialogVisible: false,
  previewDialogConfig: { fontSize: 'medium', wordWrap: true, searchKeyword: '' }
})
```

### 5.4 API 封装

```javascript
// src/api/lingdoc/vault.js
import request from '@/utils/request'

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
```

---

## 6. 与现有功能衔接

### 6.1 自动规整

| 衔接点 | 说明 |
|--------|------|
| 数据写入 | 自动规整完成后，将规整后的文件信息 INSERT 到 `lingdoc_file_index` |
| 字段映射 | `tag` = AI 分类的一级目录，`sub_path` = AI 生成的子目录路径，`source_type` = '1' |
| 内容提取 | 规整时同步读取文本文件内容写入 `file_content`，供全文检索 |

### 6.2 表格填写助手

| 衔接点 | 说明 |
|--------|------|
| 参考文档选择 | 表格助手的"选择参考文档"弹窗改为调用文件浏览器的组件/接口 |
| 右键快捷操作 | 文件浏览器中右键文件 → "作为表格助手参考文档"，跳转表格助手页面并传入 file_id |
| 文件关联 | `lingdoc_form_reference` 表的 `doc_id` 改为指向 `lingdoc_file_index.file_id` |

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

## 附录 A：与 FAST-006 的衔接说明

本文档复用 `FAST-006`（Vault 文件索引与数据库设计分析报告）中提出的 `lingdoc_file_index` 表概念，补充了以下内容：

1. **完整建表语句**：`FAST-006` 仅列出字段设计，本文档提供可直接执行的 SQL
2. **索引细化**：增加 `idx_user_tag`、`idx_user_type_time` 等联合索引，覆盖文件浏览器的典型查询模式
3. **同步策略**：`FAST-006` 未涉及数据如何进入该表，本文档明确 4 种同步来源和触发时机
4. **接口层设计**：`FAST-006` 是纯分析文档，本文档向下延伸到 REST API 和前端组件

**后续文档建议**：
- `FAST-008`：AI 开发者接口契约（文件浏览器与 AI 模块的交互，如智能检索调用文件内容）
- `FAST-009`：Vault 文件扫描同步服务设计（定时任务 + 文件系统监听策略）
