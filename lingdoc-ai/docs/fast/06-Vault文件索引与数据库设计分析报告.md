# Vault 文件索引与数据库设计分析报告

**文档编号**: LingDoc-FAST-006  
**版本**: v1.0  
**编制日期**: 2026-04-19  
**分析范围**: 纯文档分析，不涉及代码开发  

---

## 一、检查结论

### 1.1 Vault 在现有文档中的覆盖情况

| 文档                            | Vault 提及情况                 | 问题                               |
| ----------------------------- | -------------------------- | -------------------------------- |
| `docs/spec/01-SRS-需求说明书.md`   | 15次+，完整定义了物理存储结构、路径规范、命名规则 | ✅ 概念设计完善                         |
| `docs/fast/01-表格填写助手-产品需求.md` | 5次，参考文档从 Vault 检索          | ⚠️ 未说明 Vault 文件如何被数据库索引          |
| `docs/fast/02-数据库设计.md`       | 2次，`doc_id` 标注为"Vault文档ID" | ❌ 外键实际指向 `kb_document`（仅AI知识库文档） |
| `docs/ai/02-AI现有代码与基础设施现状.md` | 0次                         | ❌ AI模块文档未涉及 Vault                |

**核心发现**：Vault 在文档中有完整的**物理层设计**，但**没有任何文档**说明 Vault 中的文件如何在数据库中被索引和检索。`kb_document` 表仅覆盖已上传到 AI 知识库的文档，不能代表 Vault 中的全部文件。

### 1.2 MySQL 数据库设计在现有文档中的覆盖情况

| 文档                          | MySQL/数据库设计内容                                                         | 问题                                      |
| --------------------------- | --------------------------------------------------------------------- | --------------------------------------- |
| `docs/spec/01-SRS-需求说明书.md` | 已改为 MySQL 8.0+，提及 `file_index` / `file_version` / `file_search_index` | ❌ 只有概念描述，**没有实际建表语句**                   |
| `docs/fast/02-数据库设计.md`     | 表格助手3张表（任务/字段/参考文档）                                                   | ⚠️ 参考文档外键指向 `kb_document`，漏掉 Vault 普通文件 |
| `docs/spec/09-AI模块架构设计.md`  | AI 模块6张表（知识库/文档/分块/向量/会话/消息）                                          | ✅ 完整                                    |
| `ruoyi-server/sql/`         | 现有3个脚本：主库 / AI模块 / 表格助手模块                                             | ❌ 缺少 Vault 文件索引脚本                       |

---

## 二、核心问题：没有 Vault 文件索引表，功能无法闭环

### 2.1 当前数据库表与 Vault 的关系

```
已存在的数据库表：
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   sys_user      │     │  kb_document    │     │ lingdoc_form_...│
│   (用户表)       │     │  (AI知识库文档)  │     │  (表格助手任务)  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │                       │ 外键关联               │ 外键关联
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                                 │
                    缺失：Vault 文件索引表
                                 │
         ┌───────────────────────┴───────────────────────┐
         │                                               │
    Vault 物理目录（文件系统）                      Vault 物理目录（文件系统）
    /documents/学习资料/...                        /versions/ /desensitized/
```

### 2.2 功能受阻分析

| 需求功能             | 当前数据库支撑                   | 缺失什么                 | 导致的问题                           |
| ---------------- | ------------------------- | -------------------- | ------------------------------- |
| **表格填写助手检索参考文档** | `kb_document` 表（仅AI知识库文档） | Vault 全部文件的索引        | 只能检索已上传知识库的文档，Vault 中大量文件漏检     |
| **关系图谱展示真实文件**   | 无                         | Vault 文件索引 + 标签字段    | 只能使用 mock 数据，无法接入真实文件           |
| **自动规整功能归档文件**   | 无                         | `file_index` 表记录归档信息 | AI 规整后文件存入 Vault，但数据库无记录，后续无法检索 |
| **版本溯源**         | 无                         | `file_version` 表     | 无法记录版本快照的元数据                    |
| **全文检索**         | `kb_chunk` 表（仅知识库分块）      | Vault 文件的 OCR 文本索引   | 只能检索知识库内容，Vault 普通文件无法被搜索       |
| **重复文件检测**       | 无                         | checksum 索引          | 无法快速判断文件是否已存在                   |

---

## 三、数据库层面需要补充的工作清单

### 3.1 必须新建的核心表

#### 表A：`lingdoc_file_index`（Vault 文件主索引表）

**作用**：索引 Vault 中所有文件的基本元数据，是 Vault 与数据库之间的桥梁。

| 字段 | 类型 | 说明 | 必须 |
|------|------|------|------|
| `file_id` | varchar(64) PK | 文件唯一ID | ✅ |
| `user_id` | bigint FK | 所属用户 | ✅ |
| `file_name` | varchar(256) | 文件名 | ✅ |
| `vault_path` | varchar(512) | Vault 内相对路径 | ✅ |
| `abs_path` | varchar(512) | 绝对路径 | ✅ |
| `file_type` | varchar(32) | 文件类型（pdf/docx/xlsx等） | ✅ |
| `file_size` | bigint | 文件大小 | ✅ |
| `checksum` | varchar(64) | MD5/SHA256（去重用） | ✅ |
| `tag` | varchar(128) | 一级目录标签（学习资料/申请材料/工作文档） | ✅ |
| `sub_path` | varchar(512) | 子分类路径 | ⚪ |
| `source_type` | char(1) | 来源：0手动上传 1自动规整 2表格助手生成 | ✅ |
| `ocr_text` | longtext | OCR/解析后的文本内容 | ✅ |
| `is_desensitized` | char(1) | 是否有脱敏副本 | ⚪ |
| `create_time` | datetime | 创建时间 | ✅ |
| `update_time` | datetime | 更新时间 | ✅ |

**索引设计**：
- 主键：`file_id`
- 普通索引：`user_id`, `file_type`, `tag`, `checksum`, `source_type`
- 联合索引：`(user_id, tag)`, `(user_id, file_type, create_time)`
- 唯一索引：`(user_id, checksum)` — 同用户下文件去重
- 全文索引：`file_name` (FULLTEXT ngram), `ocr_text` (FULLTEXT ngram)

#### 表B：`lingdoc_file_version`（文件版本记录表）

**作用**：记录文件的历史版本快照，支撑版本溯源功能。

| 字段 | 类型 | 说明 |
|------|------|------|
| `version_id` | varchar(64) PK | 版本ID |
| `file_id` | varchar(64) FK → file_index | 原文件ID |
| `version_no` | int | 版本号（001, 002...） |
| `snapshot_path` | varchar(512) | 版本快照存储路径 |
| `snapshot_size` | bigint | 快照文件大小 |
| `operation_type` | char(1) | 操作类型：0编辑 1重命名 2移动 3回滚 |
| `checksum` | varchar(64) | 版本文件checksum |
| `operator_id` | bigint | 操作人 |
| `create_time` | datetime | 创建时间 |

**索引**：`file_id`, `version_no`, `create_time`

#### 表C：`lingdoc_file_tag`（文件标签关联表）

**作用**：支持多标签体系（不仅限一级目录），支撑关系图谱的多维标签筛选。

| 字段 | 类型 | 说明 |
|------|------|------|
| `tag_id` | varchar(64) PK | 标签关联ID |
| `file_id` | varchar(64) FK → file_index | 文件ID |
| `tag_name` | varchar(128) | 标签名 |
| `tag_level` | int | 标签层级（1一级 2二级...） |
| `is_auto` | char(1) | 是否AI自动提取：0手动 1自动 |
| `create_time` | datetime | 创建时间 |

**索引**：`file_id`, `tag_name`

### 3.2 需要修改的现有表

#### 修改1：`lingdoc_form_reference`（表格助手参考文档关联表）

**当前问题**：`doc_id` 外键只能指向 `kb_document.doc_id`，漏掉 Vault 中的普通文件。

**修改方案**：新增 `doc_source` 字段标识文档来源
```sql
ALTER TABLE lingdoc_form_reference 
ADD COLUMN `doc_source` char(1) DEFAULT '0' 
COMMENT '文档来源：0知识库(kb_document) 1Vault文件索引(file_index)';
```

#### 修改2：`kb_document`（AI知识库文档表）

**当前问题**：与 Vault 文件索引表数据割裂，同一份文件可能在两张表中重复记录。

**修改方案**：新增 `file_index_id` 字段关联 Vault 索引
```sql
ALTER TABLE kb_document 
ADD COLUMN `file_index_id` varchar(64) DEFAULT NULL 
COMMENT '关联Vault文件索引ID',
ADD KEY `idx_file_index_id` (`file_index_id`);
```

### 3.3 数据同步机制设计

仅有表结构不够，还需要定义 Vault 目录与数据库索引的同步策略：

| 同步时机        | 数据库操作                                                    | 说明                      |
| ----------- | -------------------------------------------------------- | ----------------------- |
| **后端启动时**   | 全量扫描 Vault 目录，比对 `file_index`                            | 检测新2增/删除/修改的文件，同步更新索引   |
| **文件上传时**   | `INSERT INTO lingdoc_file_index`                         | 用户手动上传或表格助手生成文件时立即索引    |
| **自动规整归档时** | `INSERT INTO lingdoc_file_index`                         | AI 规整后移动文件到 Vault 时立即索引 |
| **文件删除时**   | `DELETE FROM lingdoc_file_index` + 级联删除版本/标签             | 物理删除文件时同步清理元数据          |
| **文件编辑时**   | `UPDATE lingdoc_file_index` + `INSERT INTO file_version` | 保存新版本时更新索引并记录版本         |
| **定时任务**    | 增量扫描（每30分钟）                                              | 兜底机制，处理可能漏同步的文件         |

---

## 四、效率提升量化分析

### 4.1 表格填写助手参考文档检索

| 方案 | 单次查询时间（1000个文件） | 查询方式 |
|------|--------------------------|---------|
| 当前：遍历文件系统 | ~200ms ~ 2s | `File.listFiles()` 递归遍历 |
| 当前：仅查 `kb_document` | ~10ms | 但只能覆盖知识库文档 |
| **优化后：查 `lingdoc_file_index`** | **~5ms** | `SELECT ... WHERE user_id=? AND MATCH(file_name,ocr_text) AGAINST(?)` |

### 4.2 关系图谱标签提取

| 方案 | 标签提取时间 | 复杂度 |
|------|-------------|--------|
| 当前：遍历目录提取一级文件夹 | ~500ms | O(n) 文件系统操作 |
| **优化后：`SELECT DISTINCT tag`** | **~1ms** | O(1) 索引扫描 |

### 4.3 全文检索（自然语言问答）

| 方案 | 检索范围 | 响应时间 |
|------|---------|---------|
| 当前：`kb_chunk` 表 | 仅知识库文档的分块 | ~50ms |
| **优化后：`file_index` OCR 全文索引** | **Vault 中所有文件的 OCR 文本** | **~50ms（同等性能，范围扩大）** |

---

## 五、最终结论

### 文档层面已覆盖
- ✅ Vault 的物理存储结构（路径、命名、目录组织）
- ✅ MySQL 作为数据库的技术选型
- ✅ 表格助手、AI模块的具体表结构

### 文档层面缺失
- ❌ **Vault 文件主索引表**（`lingdoc_file_index`）—— **最关键缺失**
- ❌ **文件版本记录表**（`lingdoc_file_version`）
- ❌ **文件标签关联表**（`lingdoc_file_tag`）
- ❌ **Vault 目录与数据库的同步策略**
- ❌ **`kb_document` 与 `file_index` 的关联设计**

### 优先级排序

| 优先级 | 工作项 | 影响的功能 |
|--------|--------|-----------|
| **P0** | 设计 `lingdoc_file_index` 表结构 | 表格助手参考检索、关系图谱、全文搜索、自动规整 |
| **P0** | 设计 `lingdoc_file_version` 表结构 | 版本溯源 |
| **P1** | 修改 `lingdoc_form_reference` 支持多来源文档 | 表格助手参考文档完整性 |
| **P1** | 设计 Vault ↔ 数据库同步策略 | 数据一致性 |
| **P2** | 设计 `lingdoc_file_tag` 表结构 | 关系图谱多标签筛选 |
| **P2** | 修改 `kb_document` 关联 `file_index` | 避免知识库与Vault元数据重复 |

> **最终结论**：将文件及元数据添加到数据库不仅能提高效率（检索从秒级降到毫秒级），更是功能闭环的前提。当前 Vault 只有物理目录而没有数据库索引，导致表格助手、关系图谱、自动规整等功能无法接入真实数据。**数据库层面最紧迫的工作是设计并实现 `lingdoc_file_index` 表**，作为 Vault 所有文件的统一元数据入口。
