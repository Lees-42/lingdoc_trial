# 表格填写助手 —— Dify 输入输出规范

**文档编号**: LingDoc-FAST-014  
**版本**: v1.1  
**编制日期**: 2026-04-23  
**更新日期**: 2026-04-24  
**状态**: 定稿

---

## 1. 功能概述

表格填写助手是 LingDoc 的核心 AI 功能之一，帮助用户自动完成各类表格文档的填写工作。

**两阶段流程**：

```
用户上传空白表格
        ↓
┌─────────────────┐
│ Workflow 1: extract │  ← 识别表格字段（三步骤内部按需查询参考文档）
│   字段识别工作流     │
└─────────────────┘
        ↓ 返回字段列表 + 参考文档
前端渲染字段编辑面板，用户确认/修改字段值
        ↓
┌─────────────────┐
│ Workflow 2: generate│  ← 生成填写后的文档
│   文档生成工作流     │
└─────────────────┘
        ↓ 返回填写后文件路径
用户预览、下载、保存到 Vault
```

**状态流转**：

| 状态码 | 状态 | 说明 |
|--------|------|------|
| `0` | 待上传 | 刚创建任务，尚未上传文件 |
| `1` | 识别中 | AI 正在分析表格字段（Workflow 1 执行中） |
| `2` | 待确认 | 字段识别完成，等待用户确认字段值 |
| `3` | 已生成 | AI 已生成填写后的文档（Workflow 2 完成） |
| `4` | 失败 | 识别或生成过程中出错 |

---

## 2. Workflow 1：字段识别（extract）

### 2.1 接口基本信息

```
POST http://${DIFY_HOST}:${DIFY_PORT}/v1/workflows/run
Content-Type: application/json
Authorization: Bearer ${DIFY_API_KEY}
```

### 2.2 Workflow 1 内部流程（三步骤）

```
Step 1: LLM 分析表格
   输入：fileName + fileContent
   输出：字段列表 + 表格类型推断
   示例：fields=["姓名","学号","GPA"], tableType="奖学金申请表"
        ↓
Step 2: HTTP Request 节点（按需查询）
   调用后端 API：POST /lingdoc/ai/form/query-docs
   传入：fieldNames + tableType
   返回：与字段相关的参考文档列表（已截断）
        ↓
Step 3: LLM 填充字段值
   输入：fileContent + 返回的参考文档
   输出：完整的 fields（含 suggestedValue + sourceDoc）
```

> **设计理由**：不再一次性全量传入 `vaultDocs`，而是让 Dify 根据识别出的字段按需向后端查询相关文档，大幅降低 Token 消耗，避免无关文档干扰。

### 2.3 请求体

```json
{
  "inputs": {
    "fileName": "string",
    "fileContent": "string",
    "fileType": "string"
  },
  "response_mode": "blocking",
  "user": "user_{userId}"
}
```

### 2.4 输入参数说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `inputs.fileName` | String | 是 | 原始文件名（含扩展名），如 `"国家奖学金申请表.docx"` |
| `inputs.fileContent` | String | 是 | 文件文本内容。后端会将文件转为纯文本后传入 |
| `inputs.fileType` | String | 是 | 文件类型：`pdf` / `docx` / `doc` / `xlsx` / `xls` / `html` |
| `response_mode` | String | 是 | 固定 `"blocking"` |
| `user` | String | 是 | 用户标识，格式 `"user_{userId}"` |

> **注意**：`vaultDocs` 不再通过 `inputs` 传入。参考文档改为由 Dify Workflow 内部的 **HTTP Request 节点** 按需调用后端 API 获取。

### 2.5 成功响应

```json
{
  "data": {
    "id": "run-xxx",
    "workflow_id": "workflow-xxx",
    "status": "succeeded",
    "outputs": {
      "fields": [
        {
          "fieldName": "姓名",
          "fieldType": "text",
          "fieldLabel": "姓名（中文）",
          "suggestedValue": "张三",
          "confidence": 0.92,
          "sourceDocId": "doc_002",
          "sourceDocName": "简历.docx",
          "sortOrder": 1
        },
        {
          "fieldName": "学号",
          "fieldType": "text",
          "fieldLabel": "学号",
          "suggestedValue": "2023001001",
          "confidence": 0.95,
          "sourceDocId": "doc_001",
          "sourceDocName": "成绩单.pdf",
          "sortOrder": 2
        },
        {
          "fieldName": "申请日期",
          "fieldType": "date",
          "fieldLabel": "申请日期",
          "suggestedValue": "2026-04-23",
          "confidence": 0.88,
          "sourceDocId": null,
          "sourceDocName": null,
          "sortOrder": 3
        },
        {
          "fieldName": "申请类别",
          "fieldType": "select",
          "fieldLabel": "申请类别",
          "suggestedValue": "国家奖学金",
          "confidence": 0.75,
          "options": ["国家奖学金", "校级奖学金", "社会奖学金"],
          "sourceDocId": "doc_003",
          "sourceDocName": "奖学金申请通知.pdf",
          "sortOrder": 4
        }
      ],
      "references": [
        {
          "docId": "doc_001",
          "docName": "成绩单.pdf",
          "docPath": "/学习资料/成绩单_大三下.pdf",
          "docType": "pdf",
          "relevance": 0.95
        },
        {
          "docId": "doc_002",
          "docName": "简历.docx",
          "docPath": "/个人材料/简历_2026版.docx",
          "docType": "docx",
          "relevance": 0.92
        }
      ],
      "tokenCost": 850
    },
    "elapsed_time": 3.21
  }
}
```

### 2.5 Dify HTTP Request 节点配置

在 Workflow 1 中，Step 1（LLM 分析表格）和 Step 3（LLM 填充字段值）之间插入一个 **HTTP Request** 节点：

**节点配置**：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| URL | `http://host.docker.internal:8080/lingdoc/ai/form/query-docs` | Dify 本地部署时访问宿主机后端 |
| Method | `POST` | |
| Headers | `Content-Type: application/json` | |
| Headers | `X-Vault-Path: {{#sys.user_id#}}` | 传递当前 Vault 路径（需根据实际变量调整） |
| Timeout | `5000` | 5 秒超时 |

**请求体（Body）**：

```json
{
  "fieldNames": {{#step1.fields#}},
  "tableType": {{#step1.tableType#}},
  "maxDocs": 3,
  "maxCharsPerDoc": 2000
}
```

> **变量说明**：`step1` 为前序 LLM 节点的输出变量名，实际配置时需与 Dify 节点命名对应。

**响应处理**：
- HTTP 200 → 将 `data.docs` 注入 Step 3 的 LLM Prompt
- HTTP 非 200 或超时 → Step 3 的 LLM 在无参考文档模式下运行（字段建议值可能为空）

---

### 2.6 后端 API 规范

**接口**：`POST /lingdoc/ai/form/query-docs`

**功能**：根据表格字段列表，从 Vault 中筛选最相关的参考文档并返回文本内容。

**请求头**：

| 头字段 | 必填 | 说明 |
|--------|------|------|
| `Content-Type` | 是 | `application/json` |
| `X-Vault-Path` | 是 | 当前 Vault 根目录绝对路径 |
| `Authorization` | 是 | `Bearer {token}`（JWT 鉴权） |

**请求体**：

```json
{
  "fieldNames": ["姓名", "学号", "GPA", "获奖情况"],
  "tableType": "奖学金申请表",
  "maxDocs": 3,
  "maxCharsPerDoc": 2000
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fieldNames` | List<String> | 是 | 表格中识别出的字段名称列表 |
| `tableType` | String | 否 | 表格类型推断（如"奖学金申请表"），用于辅助匹配 |
| `maxDocs` | Integer | 否 | 最多返回几个文档，默认 `3` |
| `maxCharsPerDoc` | Integer | 否 | 每个文档内容截取的最大字符数，默认 `2000` |

**响应体**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "docs": [
      {
        "docId": "file_001",
        "docName": "成绩单.pdf",
        "docPath": "学习/成绩单_大三下.pdf",
        "content": "姓名：张三，学号：2023001，GPA：3.8...",
        "matchReason": "字段「学号」「GPA」匹配到文档类型「成绩单」"
      },
      {
        "docId": "file_002",
        "docName": "简历.docx",
        "docPath": "个人/简历_2026版.docx",
        "content": "张三，男，计算机专业，联系电话：138****5678...",
        "matchReason": "字段「姓名」匹配到文档类型「简历」"
      }
    ],
    "totalMatched": 2,
    "queryTimeMs": 45
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `docs` | List<DocSnippet> | 匹配到的参考文档片段列表 |
| `docs[i].docId` | String | Vault 文件ID |
| `docs[i].docName` | String | 文档名称 |
| `docs[i].docPath` | String | 文档在 Vault 中的路径 |
| `docs[i].content` | String | 文档文本内容（已截断） |
| `docs[i].matchReason` | String | 匹配原因说明 |
| `totalMatched` | Integer | 匹配到的文档总数 |
| `queryTimeMs` | Integer | 查询耗时（毫秒） |

**匹配策略（后端实现参考）**：

1. **字段映射**：根据 `fieldNames` 映射到文档类型关键词
   ```
   "学号" → ["成绩单", "学籍", "学生证"]
   "GPA"  → ["成绩单", "成绩"]
   "姓名" → ["简历", "证件", "身份证"]
   ```
2. **多策略检索**：
   - 文件名关键词匹配（`file_name LIKE '%关键词%'`）
   - 路径匹配（`sub_path LIKE '%关键词%'`）
   - 标签匹配（通过 `lingdoc_tag_binding` 查找）
   - 内容搜索（`file_content LIKE '%关键词%'`）
3. **去重排序**：同一文档被多个策略命中时去重并加权，按权重取 Top-N

---

### 2.7 数据映射表（Dify → Java）

| Dify 输出字段 | Java 字段 | 类型 | 说明 |
|--------------|-----------|------|------|
| `fields` | `AiExtractResult.fields` | List<AiField> | 识别出的字段列表 |
| `fields[i].fieldName` | `AiField.fieldName` | String | 字段名称（如：姓名） |
| `fields[i].fieldType` | `AiField.fieldType` | String | 字段类型：`text`/`date`/`number`/`select`/`checkbox` |
| `fields[i].fieldLabel` | `AiField.fieldLabel` | String | 文档中的原始标签文本 |
| `fields[i].suggestedValue` | `AiField.suggestedValue` | String | AI 建议的填写值 |
| `fields[i].confidence` | `AiField.confidence` | Number | AI 置信度（0.00~1.00） |
| `fields[i].sourceDocId` | `AiField.sourceDocId` | String | 值来源的 Vault 文档ID |
| `fields[i].sourceDocName` | `AiField.sourceDocName` | String | 来源文档名称 |
| `fields[i].sortOrder` | `AiField.sortOrder` | Integer | 字段排序号 |
| `references` | `AiExtractResult.references` | List<AiReference> | 参考文档列表 |
| `references[i].docId` | `AiReference.docId` | String | Vault 文档ID |
| `references[i].docName` | `AiReference.docName` | String | 文档名称 |
| `references[i].docPath` | `AiReference.docPath` | String | 文档存储路径 |
| `references[i].docType` | `AiReference.docType` | String | 文档类型 |
| `references[i].relevance` | `AiReference.relevance` | Number | 相关性评分（0.00~1.00） |
| `tokenCost` | `AiExtractResult.tokenCost` | Integer | Token 消耗量 |

---

## 3. Workflow 2：文档生成（generate）

### 3.1 接口基本信息

```
POST http://${DIFY_HOST}:${DIFY_PORT}/v1/workflows/run
Content-Type: application/json
Authorization: Bearer ${DIFY_API_KEY}
```

### 3.2 请求体

```json
{
  "inputs": {
    "originalFilePath": "string",
    "fileType": "string",
    "confirmedFields": "string"
  },
  "response_mode": "blocking",
  "user": "user_{userId}"
}
```

### 3.3 输入参数说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `inputs.originalFilePath` | String | 是 | 原始空白表格文件的绝对路径 |
| `inputs.fileType` | String | 是 | 文件类型：`pdf` / `docx` / `doc` / `xlsx` / `xls` / `html` |
| `inputs.confirmedFields` | String | 是 | 用户已确认的字段列表，JSON 数组字符串 |
| `response_mode` | String | 是 | 固定 `"blocking"` |
| `user` | String | 是 | 用户标识，格式 `"user_{userId}"` |

**`confirmedFields` 格式示例**：

```json
[
  {"fieldName": "姓名", "fieldValue": "张三", "fieldType": "text"},
  {"fieldName": "学号", "fieldValue": "2023001001", "fieldType": "text"},
  {"fieldName": "申请日期", "fieldValue": "2026-04-23", "fieldType": "date"},
  {"fieldName": "申请类别", "fieldValue": "国家奖学金", "fieldType": "select"}
]
```

> **注意**：`fieldValue` 优先使用用户确认值（`userValue`），若用户未修改则使用 AI 建议值（`aiValue`）。

### 3.4 成功响应

**方式 A（AI 直接生成文件，优先）**：

```json
{
  "data": {
    "id": "run-yyy",
    "workflow_id": "workflow-yyy",
    "status": "succeeded",
    "outputs": {
      "filledFilePath": "/tmp/lingdoc/form/filled/国家奖学金申请表_已填写.docx",
      "filledValues": {
        "姓名": "张三",
        "学号": "2023001001",
        "申请日期": "2026-04-23",
        "申请类别": "国家奖学金"
      },
      "tokenCost": 1200
    },
    "elapsed_time": 5.67
  }
}
```

**方式 B（仅返回字段值映射，后端自行渲染）**：

```json
{
  "data": {
    "status": "succeeded",
    "outputs": {
      "filledFilePath": null,
      "filledValues": {
        "姓名": "张三",
        "学号": "2023001001",
        "申请日期": "2026-04-23",
        "申请类别": "国家奖学金"
      },
      "tokenCost": 800
    }
  }
}
```

### 3.5 生成方式说明

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| **方式 A** | AI 直接用 python-docx / PyPDF2 / openpyxl 等库操作原文件，生成填写后的文件，返回 `filledFilePath` | docx / pdf / xlsx 等二进制格式 |
| **方式 B** | AI 仅返回 `filledValues` 字段值映射表，后端用 Jsoup / POI 等本地渲染 | html 格式，或其他格式的兜底方案 |

**后端处理逻辑**：

```
if (result.filledFilePath != null) {
    // 方式 A：直接使用 AI 生成的文件
    复制文件到标准输出目录
} else if (result.filledValues != null) {
    // 方式 B：本地渲染
    if (fileType == "html") → Jsoup 渲染
    else → 复制原文件（未填写，兜底）
}
```

### 3.6 数据映射表（Dify → Java）

| Dify 输出字段 | Java 字段 | 类型 | 说明 |
|--------------|-----------|------|------|
| `filledFilePath` | `AiGenerateResult.filledFilePath` | String | 方式 A：AI 生成的填写后文件绝对路径 |
| `filledValues` | `AiGenerateResult.filledValues` | Map<String, String> | 方式 B：字段值映射表（key=字段名, value=填写值） |
| `tokenCost` | `AiGenerateResult.tokenCost` | Integer | Token 消耗量 |

---

## 4. Java 结果模型

### 4.1 字段识别结果

```java
public class AiExtractResult {
    private List<AiField> fields;       // 识别出的字段列表
    private List<AiReference> references; // 参考文档列表
    private Integer tokenCost;          // Token 消耗量
}

public class AiField {
    private String fieldName;       // 字段名称
    private String fieldType;       // text/date/number/select/checkbox
    private String fieldLabel;      // 文档中的原始标签文本
    private String suggestedValue;  // AI 建议的填写值
    private BigDecimal confidence;  // 置信度
    private String sourceDocId;     // 值来源的 Vault 文档ID
    private String sourceDocName;   // 来源文档名称
    private Integer sortOrder;      // 字段排序号
}

public class AiReference {
    private String docId;       // Vault 文档ID
    private String docName;     // 文档名称
    private String docPath;     // 文档存储路径
    private String docType;     // 文档类型
    private BigDecimal relevance; // 相关性评分
}
```

### 4.2 文档生成结果

```java
public class AiGenerateResult {
    private String filledFilePath;          // 方式 A：AI 生成的文件绝对路径
    private Map<String, String> filledValues; // 方式 B：字段值映射表
    private Integer tokenCost;              // Token 消耗量
}
```

---

## 5. 兜底策略

| 场景 | 兜底处理 |
|------|----------|
| `fields` 为空或 null | 返回空列表，前端显示"未识别到字段"，任务状态置为 `4`（失败） |
| `fieldType` 不在枚举范围内 | 后端默认按 `text` 处理 |
| `suggestedValue` 为空 | 字段显示为空，用户手动填写 |
| `references` 为空 | 返回空列表，不显示参考文档区域 |
| HTTP 节点查询超时 | Dify 侧捕获超时，Step 3 LLM 在无参考文档模式下运行，字段建议值可能为空 |
| HTTP 节点返回非 200 | Dify 侧忽略错误，Step 3 LLM 在无参考文档模式下运行 |
| `filledFilePath` 和 `filledValues` 均为空 | 返回生成失败，任务状态置为 `4`（失败） |
| `filledFilePath` 指向的文件不存在 | 回退到方式 B（若 filledValues 有值），否则失败 |
| Dify 响应超时 | 捕获异常，任务状态置为 `4`，返回前端"AI 服务繁忙，请重试" |
| Dify 返回非 200 | 根据错误码返回对应提示（参见 FAST-012 第 2.2 节错误码表） |

---

## 6. 完整调用示例

### 6.1 Workflow 1：字段识别

**Request**：

```http
POST http://192.168.43.100:5001/v1/workflows/run
Authorization: Bearer app-MEV0eZXsC3AQwOjDCW3rmXNR
Content-Type: application/json

{
  "inputs": {
    "fileName": "国家奖学金申请表.docx",
    "fileContent": "国家奖学金申请表\n\n姓名：____\n学号：____\n申请日期：____\n申请类别：□国家奖学金 □校级奖学金 □社会奖学金\n...",
    "fileType": "docx"
  },
  "response_mode": "blocking",
  "user": "user_1"
}
```

> **注意**：`vaultDocs` 不再通过 `inputs` 传入。Workflow 1 内部通过 HTTP Request 节点调用 `POST /lingdoc/ai/form/query-docs` 按需获取参考文档。

**Response（解析后的 Java）**：

```java
AiExtractResult result = new AiExtractResult();

AiField field1 = new AiField();
field1.setFieldName("姓名");
field1.setFieldType("text");
field1.setFieldLabel("姓名");
field1.setSuggestedValue("张三");
field1.setConfidence(new BigDecimal("0.92"));
field1.setSourceDocId("doc_001");
field1.setSourceDocName("成绩单.pdf");
field1.setSortOrder(1);

AiReference ref1 = new AiReference();
ref1.setDocId("doc_001");
ref1.setDocName("成绩单.pdf");
ref1.setDocPath("/学习资料/成绩单.pdf");
ref1.setDocType("pdf");
ref1.setRelevance(new BigDecimal("0.95"));

result.setFields(List.of(field1, ...));
result.setReferences(List.of(ref1, ...));
result.setTokenCost(850);
```

### 6.2 Workflow 2：文档生成

**Request**：

```http
POST http://192.168.43.100:5001/v1/workflows/run
Authorization: Bearer app-MEV0eZXsC3AQwOjDCW3rmXNR
Content-Type: application/json

{
  "inputs": {
    "originalFilePath": "D:/upload/lingdoc/form/2026/04/23/国家奖学金申请表.docx",
    "fileType": "docx",
    "confirmedFields": "[{\"fieldName\":\"姓名\",\"fieldValue\":\"张三\",\"fieldType\":\"text\"},{\"fieldName\":\"学号\",\"fieldValue\":\"2023001001\",\"fieldType\":\"text\"}]"
  },
  "response_mode": "blocking",
  "user": "user_1"
}
```

**Response（解析后的 Java）**：

```java
AiGenerateResult result = new AiGenerateResult();
result.setFilledFilePath("/tmp/lingdoc/form/filled/国家奖学金申请表_已填写.docx");
result.setFilledValues(Map.of(
    "姓名", "张三",
    "学号", "2023001001"
));
result.setTokenCost(1200);
```

---

## 7. 与自动规整的差异

| 维度 | 自动规整（FAST-013） | 表格填写助手（FAST-014） |
|------|---------------------|------------------------|
| Workflow 数量 | 1 个 | 2 个（extract + generate） |
| 输入 | 文件名 + 内容 | 文件内容（extract）/ 文件路径 + 字段值（generate） |
| 参考文档获取 | 后端组装 `existingDirs` + `existingTags` 传入 | Dify HTTP 节点按需调用后端 API 获取 |
| 输出 | 元数据 JSON | 字段列表（extract）/ 文件路径（generate） |
| 文件处理 | 不修改原文件 | AI 直接修改原文件生成新文件 |
| 结果持久化 | inbox → Vault | task → filledFile → Vault（可选） |

---

## 8. 相关代码索引

| 文件 | 路径 | 说明 |
|------|------|------|
| AI 服务接口 | `ruoyi-system/.../service/lingdoc/ai/IAiFormService.java` | `extract()` + `generate()` |
| Mock 实现 | `ruoyi-system/.../service/lingdoc/ai/impl/MockAiFormServiceImpl.java` | 当前 `@Primary` 占位实现 |
| 字段识别结果 | `ruoyi-system/.../domain/lingdoc/ai/AiExtractResult.java` | Dify extract 输出映射目标 |
| 字段定义 | `ruoyi-system/.../domain/lingdoc/ai/AiField.java` | 识别出的字段模型 |
| 参考文档 | `ruoyi-system/.../domain/lingdoc/ai/AiReference.java` | 参考文档模型 |
| 文档生成结果 | `ruoyi-system/.../domain/lingdoc/ai/AiGenerateResult.java` | Dify generate 输出映射目标 |
| Controller | `ruoyi-admin/.../controller/lingdoc/LingdocFormController.java` | 前端 API 入口 |
| Service 接口 | `ruoyi-system/.../service/lingdoc/ILingdocFormTaskService.java` | 业务层接口 |
| **文档查询 API** | `ruoyi-admin/.../controller/lingdoc/FormDocQueryController.java` | **HTTP 节点调用的后端 API（需新增）** |
| Dify 通用规范 | `docs/fast/12-Dify集成开发规范.md` | 后端实施规范、配置、错误码 |
| AI 接口契约 | `docs/fast/04-AI开发者接口契约.md` | 与 AI 开发者的协作规范 |
