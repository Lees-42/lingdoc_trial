# 表格填写助手 —— Dify HTTP Request 节点输入输出详解

**文档编号**: LingDoc-FAST-017  
**版本**: v1.0  
**编制日期**: 2026-04-24  
**配套文档**: FAST-014《表格填写助手 Dify 输入输出规范》

> **本文档面向对象**：负责在 Dify 中配置和维护表格填写助手 Workflow 的 AI 同学。  
> **核心目标**：详细说明 Workflow 1（extract）中 HTTP Request 节点的完整输入输出规范，包括变量引用、Prompt 设计、失败处理等细节。

---

## 1. 节点在 Workflow 中的位置

表格填写助手的 Workflow 1（字段识别）由 **三个顺序节点** 组成：

```
Start ---> [LLM: 分析表格] ---> [HTTP: 查询文档]
                                       |
                              [LLM: 填充字段值] ---> End
```

**数据流**：

| 步骤 | 节点 | 输入 | 输出 |
|------|------|------|------|
| 1 | Start | fileName, fileContent, fileType | — |
| 2 | LLM (分析) | fileName + fileContent | fields, tableType |
| 3 | HTTP (查询) | fields + tableType | docs, totalMatched |
| 4 | LLM (填充) | fileContent + docs | fields, references, tokenCost |
| 5 | End | — | 最终 outputs |

---

## 2. Step 1 LLM 输出（HTTP 节点的输入来源）

### 2.1 节点配置

**节点类型**：LLM  
**节点命名建议**：`analyze_table`

### 2.2 System Prompt

你是一个表格分析助手。请分析用户上传的空白表格，完成以下任务：

1. 识别表格中所有需要填写的字段名称（按出现顺序）
2. 推断表格的类型（如"奖学金申请表"、"劳动合同"、"报销单"等）
3. 输出严格的 JSON 格式，不要包含任何解释性文字

必须输出以下 JSON 结构：

```json
{
  "fields": ["字段1", "字段2", "字段3"],
  "tableType": "表格类型"
}
```

要求：
- fields 必须是数组，每个元素是字段在文档中的原始标签文本
- tableType 不能为空，尽量具体（如"国家奖学金申请表"而非"申请表"）
- 只输出 JSON，不要输出 markdown 代码块标记

### 2.3 User Prompt

```
文件名：{{#start.fileName#}}

表格内容：
{{#start.fileContent#}}
```

> **变量引用**：`{{#start.fileName#}}` 引用 Start 节点的 `fileName` 输入变量。

### 2.4 输出变量

LLM 节点执行后，会在 Dify 变量池中生成以下变量（假设节点命名为 `analyze_table`）：

| 变量路径 | 类型 | 示例值 | 说明 |
|---------|------|--------|------|
| `{{#analyze_table.fields#}}` | Array | `["姓名", "学号", "GPA", "获奖情况"]` | 字段名称数组 |
| `{{#analyze_table.tableType#}}` | String | `"国家奖学金申请表"` | 表格类型 |
| `{{#analyze_table.text#}}` | String | 完整 JSON 字符串 | LLM 原始输出文本 |

> **关键**：Dify 会自动解析 LLM 返回的 JSON，将键值对提取为变量。如果 LLM 输出格式不标准，变量可能无法正确提取。

### 2.5 输出格式验证（建议）

在 LLM 节点后添加一个 **Code 节点** 做格式校验：

```javascript
function main({params}) {
    const fields = params.fields || [];
    const tableType = params.tableType || "";
    
    if (!Array.isArray(fields) || fields.length === 0) {
        return { error: "未识别到字段", fields: [], tableType: "" };
    }
    if (!tableType) {
        return { error: "未推断出表格类型", fields, tableType: "" };
    }
    
    return { fields, tableType, error: null };
}
```

---

## 3. HTTP Request 节点配置

### 3.1 节点基础配置

**节点类型**：HTTP Request  
**节点命名建议**：`query_docs`

| 配置项 | 值 | 说明 |
|--------|-----|------|
| URL | `http://host.docker.internal:8080/lingdoc/ai/form/query-docs` | Dify 本地部署访问宿主机后端 |
| Method | `POST` | |
| Timeout | `5000` | 5 秒超时，超时后走失败分支 |

> **URL 环境适配**：
> - Dify Docker 本地部署：`http://host.docker.internal:8080`
> - Dify 与后端在同一网络：`http://backend-service:8080`
> - 生产环境：`https://api.lingdoc.example.com`

### 3.2 Headers 配置

| Header Key | Header Value | 说明 |
|-----------|-------------|------|
| `Content-Type` | `application/json` | 固定 |
| `X-Vault-Path` | `{{#sys.user_id#}}` | 传递用户标识，后端自动回退到默认 Vault |
| `Authorization` | `Bearer {{#sys.app.api_key#}}` | JWT Token，若系统变量不可用则在前序节点生成 |

> **关于鉴权**：如果 Dify 无法直接获取后端 JWT，可在后端将 `/lingdoc/ai/form/query-docs` 配置为**公开接口**（仅校验 Vault 路径合法性），或在工作流开始时由后端预生成一个临时 Token 存入变量。

### 3.3 Body 配置（JSON）

```json
{
  "fieldNames": {{#analyze_table.fields#}},
  "tableType": "{{#analyze_table.tableType#}}",
  "maxDocs": 3,
  "maxCharsPerDoc": 2000
}
```

**变量引用解析**：

| Body 中的写法 | Dify 实际替换为 | 类型 |
|--------------|----------------|------|
| `{{#analyze_table.fields#}}` | `["姓名", "学号", "GPA"]` | JSON 数组 |
| `"{{#analyze_table.tableType#}}"` | `"国家奖学金申请表"` | JSON 字符串 |

> **数组变量注意**：`{{#analyze_table.fields#}}` 在 JSON Body 中**不要加引号**，Dify 会自动将其序列化为 JSON 数组。如果写成 `"{{#analyze_table.fields#}}"` 会变成字符串 `"[\"姓名\",\"学号\"]"`，后端解析会失败。

### 3.4 完整请求示例

HTTP 节点实际发送的请求：

```http
POST http://host.docker.internal:8080/lingdoc/ai/form/query-docs
Content-Type: application/json
X-Vault-Path: user_1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

{
  "fieldNames": ["姓名", "学号", "GPA", "获奖情况"],
  "tableType": "国家奖学金申请表",
  "maxDocs": 3,
  "maxCharsPerDoc": 2000
}
```

---

## 4. HTTP Request 节点输出

### 4.1 成功响应（HTTP 200）

HTTP 节点执行成功后，在 Dify 变量池中生成以下变量（假设节点命名为 `query_docs`）：

| 变量路径 | 类型 | 示例值 | 说明 |
|---------|------|--------|------|
| `{{#query_docs.status_code#}}` | Number | `200` | HTTP 状态码 |
| `{{#query_docs.body#}}` | Object | 完整响应 JSON | 响应体对象 |
| `{{#query_docs.body.code#}}` | Number | `200` | 业务状态码 |
| `{{#query_docs.body.data.docs#}}` | Array | 文档数组 | 匹配到的参考文档列表 |
| `{{#query_docs.body.data.totalMatched#}}` | Number | `2` | 匹配到的文档总数 |
| `{{#query_docs.body.data.queryTimeMs#}}` | Number | `45` | 查询耗时 |

**`body.data.docs` 数组元素结构**：

```json
{
  "docId": "file_001",
  "docName": "成绩单.pdf",
  "docPath": "学习/成绩单_大三下.pdf",
  "content": "姓名：张三，学号：2023001，GPA：3.8...",
  "matchReason": "字段「学号」「GPA」匹配到文档类型「成绩单」"
}
```

### 4.2 失败响应

HTTP 节点失败时（非 200 或超时）：

| 变量路径 | 类型 | 示例值 | 说明 |
|---------|------|--------|------|
| `{{#query_docs.status_code#}}` | Number | `500` 或 `0` | 状态码（超时为 0） |
| `{{#query_docs.body#}}` | Object / null | `{"code":500,"msg":"..."}` 或 `null` | 错误响应体 |

---

## 5. Step 3 LLM 输入（引用 HTTP 输出）

### 5.1 节点配置

**节点类型**：LLM  
**节点命名建议**：`fill_fields`

### 5.2 System Prompt

```
你是一个表格填写助手。请根据提供的参考文档，为表格中的每个字段给出建议值。

规则：
1. 每个字段必须提供 suggestedValue，如果参考文档中没有对应信息则填空字符串 ""
2. 必须标注值来源的文档（sourceDocId 和 sourceDocName）
3. 对于 select 类型字段，从表格上下文中推断可选选项
4. 输出严格的 JSON 格式，不要包含 markdown 代码块

必须输出以下 JSON 结构：
{
  "fields": [
    {
      "fieldName": "字段名",
      "fieldType": "text/date/number/select/checkbox",
      "fieldLabel": "文档中的原始标签",
      "suggestedValue": "建议值",
      "confidence": 0.95,
      "sourceDocId": "来源文档ID（无则null）",
      "sourceDocName": "来源文档名称（无则null）",
      "sortOrder": 1,
      "options": ["选项1", "选项2"]
    }
  ],
  "references": [
    {
      "docId": "文档ID",
      "docName": "文档名称",
      "docPath": "文档路径",
      "docType": "pdf/docx/xlsx",
      "relevance": 0.95
    }
  ],
  "tokenCost": 850
}

注意：
- options 只在 fieldType 为 "select" 时需要
- references 数组包含所有被引用过的文档（去重）
- confidence 范围 0.00 ~ 1.00
```

### 5.3 User Prompt（有参考文档）

```
表格内容：
{{#start.fileContent#}}

参考文档（已从 Vault 中检索）：
{{#query_docs.body.data.docs#}}

字段列表（需为以下字段提供建议值）：
{{#analyze_table.fields#}}

表格类型：{{#analyze_table.tableType#}}
```

> **变量引用说明**：
> - `{{#query_docs.body.data.docs#}}` 注入 HTTP 节点返回的文档数组
> - 如果 HTTP 节点失败（status_code != 200），此变量可能为空，需在 Prompt 中处理

### 5.4 User Prompt（无参考文档）

```
表格内容：
{{#start.fileContent#}}

参考文档：暂无相关文档。

字段列表：
{{#analyze_table.fields#}}

注意：由于没有找到相关参考文档，suggestedValue 可能为空，confidence 设为 0。
```

---

## 6. 失败处理分支

### 6.1 推荐的分支设计

```
                    [analyze_table]
                         |
                    [query_docs]
                         |
              +----------+----------+
              | status_code == 200   | else
              v                      v
    [fill_with_docs]        [fill_without_docs]
              |                      |
              +----------+-----------+
                         v
                       [End]
```

### 6.2 条件节点配置

| 条件表达式 | 说明 |
|-----------|------|
| `{{#query_docs.status_code#}}` == `200` | HTTP 成功 |
| `{{#query_docs.status_code#}}` != `200` | HTTP 失败 |
| `{{#query_docs.body.data.totalMatched#}}` > `0` | 有匹配到文档（可选的额外条件） |

---

## 7. 关键限制与注意事项

### 7.1 变量引用语法

| 场景 | 正确写法 | 错误写法 | 说明 |
|------|---------|---------|------|
| 数组变量 | `{{#analyze_table.fields#}}` | `"{{#analyze_table.fields#}}"` | 数组不要加引号 |
| 字符串变量 | `"{{#analyze_table.tableType#}}"` | `{{#analyze_table.tableType#}}` | 字符串必须加引号 |
| 嵌套对象 | `{{#query_docs.body.data.docs#}}` | `{{#query_docs.docs#}}` | 必须写完整路径 |

### 7.2 JSON 序列化行为

Dify 在 Body 中替换变量时：
- **Array 类型**：直接替换为 JSON 数组（如 `["a","b"]`）
- **String 类型**：替换为原始字符串值（不含引号，需手动在 JSON 中加 `""`）
- **Number 类型**：直接替换为数字

### 7.3 网络可达性

| 部署场景 | URL 配置 | 说明 |
|---------|---------|------|
| Dify Docker + 后端本机 | `http://host.docker.internal:8080` | Windows/macOS 有效 |
| Dify Docker + 后端容器 | `http://backend:8080` | 需在同一 Docker 网络 |
| Dify 本机 + 后端本机 | `http://localhost:8080` | 非 Docker 部署 |
| 生产环境 | `https://api.xxx.com` | 需 HTTPS + 域名 |

### 7.4 超时与重试

| 配置项 | 建议值 | 说明 |
|--------|--------|------|
| Timeout | `5000` | 5 秒，后端查询 SQLite 通常 < 1 秒 |
| 重试 | Dify 不支持自动重试 | 如需重试，需用循环节点或条件分支实现 |
| 失败兜底 | 必须设计无文档分支 | 确保即使 HTTP 失败，Workflow 也能正常结束 |

### 7.5 Token 消耗估算

| 环节 | 输入 Token | 说明 |
|------|-----------|------|
| Step 1 LLM | ~500 | 分析表格字段 |
| HTTP 查询 | 0 | 不消耗 LLM Token |
| Step 3 LLM（有文档）| ~2000-5000 | 取决于文档数量和长度 |
| Step 3 LLM（无文档）| ~500 | 仅识别字段，无建议值 |

**优化建议**：
- `maxDocs` 设为 3，`maxCharsPerDoc` 设为 2000，可控制 Step 3 的输入在 6000 Token 以内
- 如果 `totalMatched` 为 0，直接走无文档分支，节省 Token

---

## 8. 完整 Workflow 配置示例

### 8.1 节点清单

| # | 节点名称 | 类型 | 说明 |
|---|---------|------|------|
| 1 | `start` | Start | 接收 fileName/fileContent/fileType |
| 2 | `analyze_table` | LLM | 分析表格，输出 fields + tableType |
| 3 | `check_format` | Code | 校验 LLM 输出格式（可选） |
| 4 | `query_docs` | HTTP | 调用后端 API 获取参考文档 |
| 5 | `has_docs` | IF/ELSE | 判断 HTTP 是否成功且有文档 |
| 6 | `fill_with_docs` | LLM | 有参考文档时填充字段 |
| 7 | `fill_without_docs` | LLM | 无参考文档时填充字段 |
| 8 | `end` | End | 输出最终结果 |

### 8.2 节点间连线

```
start --> analyze_table --> check_format --> query_docs --> has_docs
                                                              |
                                    +-------------------------+
                                    |
                    +---------------+---------------+
                    | status_code == 200             | else
                    v                                v
            fill_with_docs                    fill_without_docs
                    |                                |
                    +---------------+----------------+
                                    v
                                   end
```

### 8.3 关键配置参数

**analyze_table（LLM 节点）**：
- System Prompt：见 2.2
- User Prompt：见 2.3
- Model：建议 `gpt-4o-mini` 或同级模型
- Temperature：`0.2`（低温度保证 JSON 格式稳定）
- Max Tokens：`1024`

**query_docs（HTTP 节点）**：
- URL：`http://host.docker.internal:8080/lingdoc/ai/form/query-docs`
- Method：`POST`
- Headers：`Content-Type: application/json`
- Body：见 3.3
- Timeout：`5000`

**has_docs（IF/ELSE 节点）**：
- 条件1：`{{#query_docs.status_code#}}` == `200`
- 分支1 -> `fill_with_docs`
- 分支2（else）-> `fill_without_docs`

**fill_with_docs（LLM 节点）**：
- System Prompt：见 5.2
- User Prompt：见 5.3（含 `{{#query_docs.body.data.docs#}}`）

**fill_without_docs（LLM 节点）**：
- System Prompt：见 5.2
- User Prompt：见 5.4（无参考文档版本）

---

## 9. 调试检查清单

在 Dify 中配置完成后，按以下步骤验证：

- [ ] **Step 1 输出正确**：`analyze_table.fields` 是数组且不为空
- [ ] **HTTP 请求格式正确**：Body 中的 `fieldNames` 是 JSON 数组而非字符串
- [ ] **HTTP 响应解析正确**：`query_docs.body.data.docs` 能正确提取
- [ ] **分支逻辑正确**：HTTP 200 时走 `fill_with_docs`，失败时走 `fill_without_docs`
- [ ] **Step 3 输出正确**：`fill_with_docs.fields` 包含 suggestedValue 和 sourceDocId
- [ ] **End 节点输出正确**：最终 outputs 与 FAST-014 数据映射表对齐

---

## 10. 相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 表格填写助手输入输出规范 | `docs/fast/14-表格填写助手-Dify输入输出规范.md` | 整体 Workflow 规范 |
| Dify 集成开发规范 | `docs/fast/12-Dify集成开发规范.md` | 后端 API 通用规范 |
| 本地知识库开发规范 | `docs/fast/16-本地知识库开发规范.md` | Vault 架构与 API 定义 |
