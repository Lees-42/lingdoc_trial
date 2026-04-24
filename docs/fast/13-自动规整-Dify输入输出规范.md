# 自动规整 —— Dify 输入输出规范

**文档编号**: LingDoc-FAST-013  
**版本**: v1.1  
**编制日期**: 2026-04-23  
**更新日期**: 2026-04-24  
**状态**: 已联调验证

---

## 1. 自动规整功能概述

### 1.1 什么是自动规整

自动规整是 LingDoc 的核心 AI 功能：用户上传文件后，AI 自动分析文件内容，给出**分类建议**、**标签建议**、**重命名建议**、**内容摘要**和**关键词**，用户确认后文件自动归档到 Vault 的合适位置。

### 1.2 Inbox 模式架构

采用 **Vault Inbox 模式**，文件始终只有一份，不占用双倍磁盘：

```
{vaultRoot}/
├── documents/        ← 正式 Vault 文件目录
├── inbox/            ← 收件箱/临时文件目录（与 documents 并列）
└── .lingdoc/
    └── vault.db      ← SQLite（lingdoc_inbox + lingdoc_file_index）
```

**流程**：

```
前端上传 ──→ inbox/ 目录 + SQLite lingdoc_inbox
                ↓ organize（AI 分析，更新建议字段）
                ↓ confirm（物理 move 到 documents/ + 移入 lingdoc_file_index + 删 inbox 记录）
```

### 1.3 状态流转

| 状态 | 说明 | 流转条件 |
|------|------|----------|
| `uploaded` | 刚上传，等待规整 | 用户点击"自动规整" |
| `organizing` | AI 分析中 | AI 服务开始处理 |
| `pending` | 分析完成，等待用户确认 | AI 返回结果 |
| `confirmed` | 用户已确认归档（前端状态） | 用户点击"确认" |
| `failed` | 规整失败 | AI 服务异常或超时 |

---

## 2. Dify Workflow 请求规范

### 2.1 接口基本信息

```
POST http://localhost:5001/v1/workflows/run
Content-Type: application/json
Authorization: Bearer ${DIFY_API_KEY}
```

> **环境说明**：Dify 部署在本机本地，后端 Spring Boot 直接通过 `localhost:5001` 访问。前端开发服务器运行在 `http://localhost:3000`，通过 Vite 代理 `/dev-api` → `http://localhost:8080`。

### 2.2 请求体（Request Body）

```json
{
  "inputs": {
    "fileName": "string",
    "fileContent": "string",
    "existingDirs": "string",
    "existingTags": "string"
  },
  "response_mode": "blocking",
  "user": "user_{userId}"
}
```

### 2.3 输入参数说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `inputs` | Object | 是 | Workflow 输入变量，键值对形式 |
| `inputs.fileName` | String | 是 | 原始文件名（含扩展名），如 `"个人简历_初稿.docx"` |
| `inputs.fileContent` | String | 否 | 文件文本内容。非文本类型（图片/视频等）传空字符串 `""` |
| `inputs.existingDirs` | String | 否 | 用户 Vault 已有目录结构，逗号分隔，如 `"工作/合同,学习/笔记,个人/简历"` |
| `inputs.existingTags` | String | 否 | 用户已有标签列表，逗号分隔，如 `"合同,简历,笔记,发票"` |
| `response_mode` | String | 是 | 固定 `"blocking"`，同步阻塞等待结果 |
| `user` | String | 是 | 用户标识，格式 `"user_{userId}"`，如 `"user_1"` |

### 2.4 超时配置

- Dify Workflow 执行时间通常 3~10 秒
- 后端 `RestTemplate` 建议设置：**连接超时 10s，读取超时 30s**

---

## 3. Dify Workflow 响应规范

### 3.1 成功响应（HTTP 200）

```json
{
  "data": {
    "id": "run-xxx",
    "workflow_id": "workflow-xxx",
    "status": "succeeded",
    "outputs": {
      "suggestedSubPath": "工作/求职材料",
      "reason": "Detected keywords related to 'resume'.",
      "confidence": 0.8,
      "tags": [
        {
          "tagName": "简历",
          "tagColor": "#409EFF",
          "reason": "Auto-tagged based on content analysis (resume).",
          "confidence": 0.9
        }
      ],
      "suggestedName": "_参考-个人简历.docx",
      "renameReason": "Standardized filename format and removed special characters.",
      "renameConfidence": 0.9,
      "summary": "个人简历 基本信息 姓名：李明          性别：男 出生年月   ：2003年8月      民族：汉族 政治面貌：共青团员    学号   ：2022010507 联系电话：138****5678 电子邮箱：liming@example.edu.cn 家庭住址：北京市海淀区学院路37号 教育背景 2022年9月至今    XX大学    计算机学院    计算机科学与技术    2...",
      "keywords": [
        "负责课程通知传达",
        "家庭住址",
        "2023年9月",
        "电子邮箱",
        "担任班级学习委员",
        "数字乡村",
        "联系电话",
        "线性代数",
        "北京市海淀区学院路37号",
        "负责学院科技创新活动的策划与组织"
      ],
      "tokenCost": 153
    },
    "elapsed_time": 2.34
  }
}
```

**关键层级**：`response.data.outputs` 才是真正的业务输出 JSON。

### 3.2 错误响应

```json
{
  "code": "invalid_api_key",
  "message": "API key is invalid",
  "status": 401
}
```

| HTTP Status | Dify Code | 说明 | 后端处理建议 |
|-------------|-----------|------|-------------|
| 401 | `invalid_api_key` | API Key 无效 | 记录日志，返回前端"AI服务配置错误" |
| 404 | `workflow_not_found` | Workflow 不存在 | 检查 workflow ID 配置 |
| 422 | `validation_error` | 输入参数校验失败 | 检查 inputs 字段名是否与 Dify 定义一致 |
| 429 | `rate_limit` | 请求频率限制 | 限流重试或返回"AI服务繁忙" |
| 500 | `internal_error` | Dify 内部错误 | 记录日志，返回前端"AI分析失败，请重试" |

---

## 4. 数据映射总表

Dify Workflow 输出字段（`data.outputs` 内）→ Java `AiOrganizeResult` 字段映射：

| Dify 输出字段 | Java 字段 | 类型 | 说明 | 必填 |
|--------------|-----------|------|------|------|
| `suggestedSubPath` | `category.suggestedSubPath` | String | 建议保存路径，如 `"工作/求职材料"` | ✅ |
| `reason` | `category.reason` | String | 分类理由 | 否 |
| `confidence` | `category.confidence` / `confidence` | Number | 分类置信度 / 整体置信度 | 否 |
| `tags` | `tags` | List | 标签建议数组 | 否 |
| `tags[i].tagName` | `tags[i].tagName` | String | 标签名 | 否 |
| `tags[i].tagColor` | `tags[i].tagColor` | String | 标签颜色 Hex，如 `#409EFF` | 否 |
| `tags[i].reason` | `tags[i].reason` | String | 标签理由 | 否 |
| `tags[i].confidence` | `tags[i].confidence` | Number | 标签置信度 | 否 |
| `suggestedName` | `rename.suggestedName` | String | 建议文件名（含扩展名） | ✅ |
| `renameReason` | `rename.reason` | String | 重命名理由 | 否 |
| `renameConfidence` | `rename.confidence` | Number | 重命名置信度 | 否 |
| `summary` | `summary` | String | 内容摘要 | 否 |
| `keywords` | `keywords` | List<String> | 关键词列表 | 否 |
| `tokenCost` | `tokenCost` | Integer | Token 消耗 | 否 |

> **注意**：Dify 侧输出字段名由 AI 同学定义。若实际字段名与上表不一致，后端 `DifyOrganizeOutput` DTO 需相应调整。

---

## 5. Java 结果模型

### 5.1 AiOrganizeResult（顶层结果）

```java
public class AiOrganizeResult {
    private AiCategorySuggestion category;   // 分类建议（路径）
    private List<AiTagSuggestion> tags;      // 标签建议列表
    private AiRenameSuggestion rename;       // 重命名建议
    private String summary;                  // 内容摘要
    private List<String> keywords;           // 关键词列表
    private BigDecimal confidence;           // 整体置信度
    private Integer tokenCost;               // Token 消耗
}
```

### 5.2 AiCategorySuggestion（分类建议）

```java
public class AiCategorySuggestion {
    private String suggestedSubPath;   // 建议子路径，如 "工作/合同/2024"
    private String reason;             // 建议理由
    private BigDecimal confidence;     // 置信度
}
```

### 5.3 AiTagSuggestion（标签建议）

```java
public class AiTagSuggestion {
    private String tagName;     // 建议标签名
    private String tagColor;    // 建议颜色，如 #409EFF
    private String reason;      // 建议理由
    private BigDecimal confidence; // 置信度
}
```

### 5.4 AiRenameSuggestion（重命名建议）

```java
public class AiRenameSuggestion {
    private String suggestedName;   // 建议文件名（含扩展名）
    private String reason;          // 建议理由
    private BigDecimal confidence;  // 置信度
}
```

---

## 6. 兜底策略

Dify 可能不返回某些字段（如 `tags` 为空数组、`summary` 为 null）。后端解析时需做空值处理：

| 字段为空情况 | 兜底值 | 说明 |
|-------------|--------|------|
| `category` / `suggestedSubPath` | `"/"` | 默认根目录 |
| `rename` / `suggestedName` | 原始文件名 | 保持原文件名 |
| `tags` | `new ArrayList<>()` | 空列表，不报错 |
| `summary` | `null` | 允许为空 |
| `keywords` | `new ArrayList<>()` | 空列表 |
| `confidence` | `BigDecimal.ZERO` | 置信度为 0 |
| `tokenCost` | `0` | Token 消耗为 0 |

---

## 7. 完整调用示例

### 7.1 Request

```http
POST http://localhost/v1/workflows/run
Authorization: Bearer app-uKN8indYSXX1bzaXpKJHC2xy
Content-Type: application/json

{
  "inputs": {
    "fileName": "个人简历_初稿.docx",
    "fileContent": "姓名：张三...",
    "existingDirs": "工作/合同,工作/简历,学习/笔记,个人/证件",
    "existingTags": "合同,简历,笔记,发票,证书"
  },
  "response_mode": "blocking",
  "user": "user_1"
}
```

### 7.2 Response

```json
{
  "data": {
    "status": "succeeded",
    "outputs": {
      "suggestedSubPath": "工作/求职材料",
      "reason": "Detected keywords related to 'resume'.",
      "confidence": 0.8,
      "tags": [
        {
          "tagName": "简历",
          "tagColor": "#409EFF",
          "reason": "Auto-tagged based on content analysis (resume).",
          "confidence": 0.9
        }
      ],
      "suggestedName": "_参考-个人简历.docx",
      "renameReason": "Standardized filename format and removed special characters.",
      "renameConfidence": 0.9,
      "summary": "个人简历 基本信息 姓名：李明          性别：男 出生年月   ：2003年8月      民族：汉族 政治面貌：共青团员    学号   ：2022010507 联系电话：138****5678 电子邮箱：liming@example.edu.cn 家庭住址：北京市海淀区学院路37号 教育背景 2022年9月至今    XX大学    计算机学院    计算机科学与技术    2...",
      "keywords": [
        "负责课程通知传达",
        "家庭住址",
        "2023年9月",
        "电子邮箱",
        "担任班级学习委员",
        "数字乡村",
        "联系电话",
        "线性代数",
        "北京市海淀区学院路37号",
        "负责学院科技创新活动的策划与组织"
      ],
      "tokenCost": 153
    }
  }
}
```

### 7.3 解析后的 Java 对象

```java
AiOrganizeResult result = new AiOrganizeResult();

// 分类建议
AiCategorySuggestion category = new AiCategorySuggestion();
category.setSuggestedSubPath("工作/求职材料");
category.setReason("Detected keywords related to 'resume'.");
category.setConfidence(new BigDecimal("0.8"));
result.setCategory(category);

// 标签建议
AiTagSuggestion tag = new AiTagSuggestion();
tag.setTagName("简历");
tag.setTagColor("#409EFF");
tag.setReason("Auto-tagged based on content analysis (resume).");
tag.setConfidence(new BigDecimal("0.9"));
result.setTags(List.of(tag));

// 重命名建议
AiRenameSuggestion rename = new AiRenameSuggestion();
rename.setSuggestedName("_参考-个人简历.docx");
rename.setReason("Standardized filename format and removed special characters.");
rename.setConfidence(new BigDecimal("0.9"));
result.setRename(rename);

// 其他字段
result.setSummary("个人简历 基本信息 姓名：李明...");
result.setKeywords(List.of("负责课程通知传达", "家庭住址", "2023年9月", "电子邮箱",
    "担任班级学习委员", "数字乡村", "联系电话", "线性代数",
    "北京市海淀区学院路37号", "负责学院科技创新活动的策划与组织"));
result.setConfidence(new BigDecimal("0.8"));
result.setTokenCost(153);
```

---

## 8. 相关代码索引

| 文件 | 路径 | 说明 |
|------|------|------|
| AI 服务接口 | `ruoyi-system/.../service/lingdoc/ai/IAiOrganizeService.java` | 需实现的接口 |
| Dify 实现 | `ruoyi-system/.../service/lingdoc/ai/impl/DifyAiOrganizeServiceImpl.java` | 当前 @Primary 实现 |
| Mock 实现 | `ruoyi-system/.../service/lingdoc/ai/impl/MockAiOrganizeServiceImpl.java` | fallback 占位实现 |
| 结果领域模型 | `ruoyi-system/.../service/lingdoc/ai/result/AiOrganizeResult.java` | Dify 输出映射目标 |
| 后端 Service | `ruoyi-system/.../service/lingdoc/LingdocInboxServiceImpl.java` | 调用 AI 服务的业务层 |
| Dify 集成规范 | `docs/fast/12-Dify集成开发规范.md` | 完整后端实施规范（含配置、客户端、DTO） |
| 后端实现计划 | `docs/plan/自动规整后端实现计划.md` | 完整架构设计与代码实现 |
