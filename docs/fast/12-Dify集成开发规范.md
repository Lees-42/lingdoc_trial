# Dify AI 与项目后端集成开发规范

**文档编号**: LingDoc-FAST-012
**版本**: v1.2
**编制日期**: 2026-04-23
**更新日期**: 2026-04-24
**状态**: 定稿（本机本地部署版）

> **变更记录**：
> - v1.2 —— Dify 从移动热点局域网方案切换为**本机本地部署**，前端端口更新为 3000。
> - v1.1 —— Dify 从官方云服务切换为 AI 同学电脑本地服务器，通过移动热点局域网访问。

---

## 1. 架构概述

### 1.1 系统交互图

```
+-------------------------------------------------------------------------+
|                              本机开发环境                                |
|                                                                          |
|  +-----------------+    +-----------------+    +---------------------+  |
|  | 前端 Vite Dev   |--->| Spring Boot 后端 |--->| Dify Workflow API   |  |
|  | Server (3000)   |    | (8080)           |    | (localhost:5001)    |  |
|  +-----------------+    +-----------------+    +---------------------+  |
|          |                       |                       |               |
|          | /dev-api 代理         | HTTP POST             | localhost     |
|          +-----------------------+ Bearer Token          +---------------+
|                                                                          |
|  浏览器访问 http://localhost:3000                                        |
|       |                                                                  |
|  Vite 代理 /dev-api -> http://localhost:8080                             |
|       |                                                                  |
|  Spring Boot 后端 -> RestTemplate -> http://localhost:5001/v1/workflows/run
|                                                                          |
+-------------------------------------------------------------------------+
```

### 1.2 服务地址速查

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | `http://localhost:3000` | Vite 开发服务器，见 `vite.config.js` |
| 后端 API | `http://localhost:8080` | Spring Boot Tomcat |
| Dify | `http://localhost:5001` | 本机本地部署的 Dify 服务 |
| Swagger | `http://localhost:8080/swagger-ui.html` | API 文档 |

### 1.3 与现有 Mock 实现的关系

当前 `MockAiOrganizeServiceImpl` 标记了 `@Primary`，返回全部空值。集成 Dify 后的切换方式：

1. **新建** `DifyAiOrganizeServiceImpl` 实现 `IAiOrganizeService`，**同样标记 `@Primary`**
2. **移除** `MockAiOrganizeServiceImpl` 的 `@Primary` 注解
3. Spring 容器自动注入 `DifyAiOrganizeServiceImpl`
4. 若 Dify 不可用，可临时将 `@Primary` 切回 Mock 实现

---

## 2. Dify Workflow 接口规范

### 2.1 请求规范

```
POST http://localhost/v1/workflows/run
Content-Type: application/json
Authorization: Bearer app-uKN8indYSXX1bzaXpKJHC2xy
```

**Request Body**：

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

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `inputs` | Object | 是 | Workflow 输入变量，键值对形式 |
| `inputs.fileName` | String | 是 | 原始文件名（含扩展名） |
| `inputs.fileContent` | String | 否 | 文件文本内容。非文本类型（图片/视频等）传空字符串 `""` |
| `inputs.existingDirs` | String | 否 | 用户 Vault 已有目录结构，逗号分隔，如 `"工作/合同,学习/笔记,个人/简历"` |
| `inputs.existingTags` | String | 否 | 用户已有标签列表，逗号分隔，如 `"合同,简历,笔记,发票"` |
| `response_mode` | String | 是 | 固定 `"blocking"`，同步阻塞等待结果 |
| `user` | String | 是 | 用户标识，建议格式 `"user_{userId}"` |

### 2.2 响应规范

**成功响应（HTTP 200）**：

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
      "summary": "个人简历 基本信息 姓名：李明...",
      "keywords": ["负责课程通知传达", "家庭住址", "2023年9月", "电子邮箱"],
      "tokenCost": 153
    },
    "elapsed_time": 2.34
  }
}
```

**关键层级**：`response.data.outputs` 才是真正的业务输出 JSON。

**错误响应**：

```json
{
  "code": "invalid_api_key",
  "message": "API key is invalid",
  "status": 401
}
```

常见错误码：

| HTTP Status | Dify Code | 说明 | 后端处理建议 |
|-------------|-----------|------|-------------|
| 401 | `invalid_api_key` | API Key 无效 | 记录日志，返回前端"AI服务配置错误" |
| 404 | `workflow_not_found` | Workflow 不存在 | 检查 workflow ID 配置 |
| 422 | `validation_error` | 输入参数校验失败 | 检查 inputs 字段名是否与 Dify 定义一致 |
| 429 | `rate_limit` | 请求频率限制 | 限流重试或返回"AI服务繁忙" |
| 500 | `internal_error` | Dify 内部错误 | 记录日志，返回前端"AI分析失败，请重试" |

### 2.3 超时控制

- Dify Workflow 执行时间视模型和复杂度而定，通常 3~10 秒
- 后端 `RestTemplate` 建议设置 **连接超时 10s，读取超时 30s**
- 前端自动规整按钮需显示"AI分析中..."状态，避免用户重复点击

### 2.4 本地部署注意事项

> **适用场景**：Dify 部署在本机本地，后端 Spring Boot 和前端 Vite 均运行在同一台机器上。

#### 2.4.1 Dify 本地服务配置

1. **Dify 监听地址**：
   - Dify 本地服务启动时必须监听 `0.0.0.0`（所有接口），而非 `127.0.0.1`（仅本机）
   - 确保端口 `5001` 未被其他程序占用

2. **端口确认**：
   - Dify 默认端口通常为 **5001**
   - 若端口被占用或自定义，需同步更新 `application.yml` 中的配置

3. **防火墙放行**：
   - Windows Defender / 第三方防火墙可能拦截入站请求
   - 建议临时放行 Dify 端口，或在防火墙中添加规则

#### 2.4.2 后端配置调整

本地环境下 `application.yml` 示例：

```yaml
lingdoc:
  ai:
    dify:
      enabled: true
      base-url: http://localhost/v1
      api-key: ${DIFY_API_KEY:app-uKN8indYSXX1bzaXpJHC2xy}
      timeout:
        connect: 10000
        read: 30000
```

> **注意**：
> - 协议必须为 `http://`，不能使用 `https://`（本地 Dify 通常无 TLS 证书）
> - 所有服务（前端 3000、后端 8080、Dify 5001）均运行在本机，无需关注 IP 变化

#### 2.4.3 网络故障排查速查表

| 现象 | 可能原因 | 排查步骤 |
|------|----------|---------|
| `Connection refused` | Dify 未启动或监听 `127.0.0.1` | 检查 Dify 启动日志中的绑定地址 |
| 无响应 / 防火墙提示 | Windows 防火墙拦截 | 临时关闭防火墙测试 |
| Dify 返回 404 | Workflow ID 错误或 Dify 版本不匹配 | 检查 Dify 版本和 Workflow 部署状态 |

---

## 3. 自动规整对接规范

### 3.1 数据映射总表

Dify Workflow 输出字段（`data.outputs` 内）→ Java `AiOrganizeResult` 字段映射：

| Dify 输出字段 | Java 字段 | 类型 | 说明 | 必填 |
|--------------|-----------|------|------|------|
| `suggestedSubPath` | `category.suggestedSubPath` | String | 建议保存路径，如 `"工作/求职材料"` | ✅ |
| `reason` | `category.reason` | String | 分类理由 | 否 |
| `confidence` | `category.confidence` | Number | 分类置信度 | 否 |
| `tags` | `tags` | List | 标签建议数组 | 否 |
| `tags[i].tagName` | `tags[i].tagName` | String | 标签名 | 否 |
| `tags[i].tagColor` | `tags[i].tagColor` | String | 标签颜色 Hex | 否 |
| `tags[i].reason` | `tags[i].reason` | String | 标签理由 | 否 |
| `tags[i].confidence` | `tags[i].confidence` | Number | 标签置信度 | 否 |
| `suggestedName` | `rename.suggestedName` | String | 建议文件名（含扩展名） | ✅ |
| `renameReason` | `rename.reason` | String | 重命名理由 | 否 |
| `renameConfidence` | `rename.confidence` | Number | 重命名置信度 | 否 |
| `summary` | `summary` | String | 内容摘要 | 否 |
| `keywords` | `keywords` | List<String> | 关键词列表 | 否 |
| `confidence` | `confidence` | Number | 整体置信度 | 否 |
| `tokenCost` | `tokenCost` | Integer | Token 消耗 | 否 |

> **注意**：Dify 侧输出字段名由 AI 同学定义。若实际字段名与上表不一致，后端 `DifyOrganizeResponse` DTO 需相应调整。

### 3.2 完整调用示例

**Request**：

```http
POST http://localhost:5001/v1/workflows/run
Authorization: Bearer app-MEV0eZXsC3AQwOjDCW3rmXNR
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

**Response**：

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
      "summary": "个人简历 基本信息 姓名：李明...",
      "keywords": ["负责课程通知传达", "家庭住址", "2023年9月", "电子邮箱"],
      "tokenCost": 153,
      "reason": "Detected keywords related to 'resume'."
    }
  }
}
```

**解析后的 `AiOrganizeResult`**：

```java
AiOrganizeResult result = new AiOrganizeResult();
result.setCategory(new AiCategorySuggestion("工作/求职材料", "Detected keywords related to 'resume'.", 0.8));
result.setTags(List.of(new AiTagSuggestion("简历", "#409EFF", "Auto-tagged based on content analysis (resume).", 0.9)));
result.setRename(new AiRenameSuggestion("_参考-个人简历.docx", "Standardized filename format and removed special characters.", 0.9));
result.setSummary("个人简历 基本信息 姓名：李明...");
result.setKeywords(List.of("负责课程通知传达", "家庭住址", "2023年9月", "电子邮箱"));
result.setConfidence(new BigDecimal("0.8"));
result.setTokenCost(153);
```

### 3.3 字段为空的兜底策略

Dify 可能不返回某些字段（如 `tags` 为空数组、`summary` 为 null）。后端解析时需做空值处理：

```java
// category 为空 → 默认根目录 "/"
if (result.getCategory() == null || StringUtils.isEmpty(result.getCategory().getSuggestedSubPath())) {
    result.setCategory(new AiCategorySuggestion("/", "默认根目录", BigDecimal.ZERO));
}

// rename 为空 → 保持原文件名
if (result.getRename() == null || StringUtils.isEmpty(result.getRename().getSuggestedName())) {
    result.setRename(new AiRenameSuggestion(originalFileName, "保持原名", BigDecimal.ZERO));
}

// tags 为空 → 空列表（不报错）
if (result.getTags() == null) {
    result.setTags(new ArrayList<>());
}
```

---

## 4. 后端实施规范

### 4.1 配置 `application.yml`

在 `ruoyi-server/ruoyi-admin/src/main/resources/application.yml` 追加：

```yaml
# Dify AI 配置
lingdoc:
  ai:
    dify:
      enabled: true
      base-url: http://localhost:5001/v1
      api-key: ${DIFY_API_KEY:app-MEV0eZXsC3AQwOjDCW3rmXNR}
      timeout:
        connect: 10000    # 连接超时 10s
        read: 30000       # 读取超时 30s
```

> **安全提示**：生产环境务必通过环境变量 `${DIFY_API_KEY}` 传入 API Key，不要硬编码。

### 4.2 新建文件清单

| # | 文件路径 | 说明 |
|---|----------|------|
| 1 | `ruoyi-system/src/main/java/com/ruoyi/system/config/DifyProperties.java` | 配置属性类，`@ConfigurationProperties(prefix = "lingdoc.ai.dify")` |
| 2 | `ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc/ai/dify/DifyWorkflowClient.java` | HTTP 客户端，封装 `RestTemplate` 调用 |
| 3 | `ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc/ai/dify/DifyWorkflowResponse.java` | Dify 返回 JSON 的完整 DTO（含 data/outputs 层级） |
| 4 | `ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc/ai/dify/DifyOrganizeOutput.java` | Dify outputs 层级的业务 DTO（字段与 3.1 映射表对齐） |
| 5 | `ruoyi-system/src/main/java/com/ruoyi/system/service/lingdoc/ai/impl/DifyAiOrganizeServiceImpl.java` | `IAiOrganizeService` 的 Dify 实现，替换 Mock |

### 4.3 各文件职责与伪代码

#### 4.3.1 `DifyProperties.java`

```java
@ConfigurationProperties(prefix = "lingdoc.ai.dify")
@Component
public class DifyProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private Timeout timeout = new Timeout();

    public static class Timeout {
        private int connect = 10000;
        private int read = 30000;
        // getters/setters...
    }
    // getters/setters...
}
```

#### 4.3.2 `DifyWorkflowClient.java`

```java
@Component
public class DifyWorkflowClient {
    @Autowired
    private DifyProperties difyProps;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(difyProps.getTimeout().getConnect());
        factory.setReadTimeout(difyProps.getTimeout().getRead());
        this.restTemplate = new RestTemplate(factory);
    }

    public DifyWorkflowResponse runWorkflow(Map<String, Object> inputs, String userId) {
        String url = difyProps.getBaseUrl() + "/workflows/run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + difyProps.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", "user_" + userId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(url, entity, DifyWorkflowResponse.class);
    }
}
```

#### 4.3.3 `DifyWorkflowResponse.java`

```java
public class DifyWorkflowResponse {
    private WorkflowData data;

    public static class WorkflowData {
        private String id;
        private String workflowId;
        private String status;      // "succeeded" / "failed"
        private DifyOrganizeOutput outputs;
        private Double elapsedTime;
        // getters/setters...
    }
    // getters/setters...
}
```

#### 4.3.4 `DifyOrganizeOutput.java`

```java
public class DifyOrganizeOutput {
    private String suggestedSubPath;
    private String reason;
    private BigDecimal confidence;
    private List<DifyTagOutput> tags;
    private String suggestedName;
    private String renameReason;
    private BigDecimal renameConfidence;
    private String summary;
    private List<String> keywords;
    private Integer tokenCost;

    public static class DifyTagOutput {
        private String tagName;
        private String tagColor;
        private String reason;
        private BigDecimal confidence;
        // getters/setters...
    }
    // getters/setters...
}
```

#### 4.3.5 `DifyAiOrganizeServiceImpl.java`

```java
@Service
@Primary
public class DifyAiOrganizeServiceImpl implements IAiOrganizeService {

    @Autowired
    private DifyWorkflowClient difyClient;

    @Autowired
    private ILingdocUserRepoService userRepoService;

    @Autowired
    private ILingdocTagService tagService;

    @Override
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId) {
        // 1. 组装 Dify 输入
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("fileName", fileName);
        inputs.put("fileContent", fileContent != null ? fileContent : "");
        inputs.put("existingDirs", getExistingDirs(userId));
        inputs.put("existingTags", getExistingTags(userId));

        // 2. 调用 Dify
        DifyWorkflowResponse resp = difyClient.runWorkflow(inputs, String.valueOf(userId));

        // 3. 校验响应
        if (resp == null || resp.getData() == null || !"succeeded".equals(resp.getData().getStatus())) {
            throw new RuntimeException("Dify 工作流执行失败");
        }

        // 4. 解析 outputs → AiOrganizeResult
        return convertToAiOrganizeResult(resp.getData().getOutputs(), fileName);
    }

    private AiOrganizeResult convertToAiOrganizeResult(DifyOrganizeOutput output, String originalFileName) {
        AiOrganizeResult result = new AiOrganizeResult();

        // category
        if (output.getSuggestedSubPath() != null) {
            AiCategorySuggestion cat = new AiCategorySuggestion();
            cat.setSuggestedSubPath(output.getSuggestedSubPath());
            cat.setReason(output.getReason());
            cat.setConfidence(output.getConfidence());
            result.setCategory(cat);
        }

        // tags
        if (output.getTags() != null) {
            List<AiTagSuggestion> tagList = output.getTags().stream().map(t -> {
                AiTagSuggestion tag = new AiTagSuggestion();
                tag.setTagName(t.getTagName());
                tag.setTagColor(t.getTagColor());
                tag.setReason(t.getReason());
                tag.setConfidence(t.getConfidence());
                return tag;
            }).collect(Collectors.toList());
            result.setTags(tagList);
        }

        // rename
        if (output.getSuggestedName() != null) {
            AiRenameSuggestion rename = new AiRenameSuggestion();
            rename.setSuggestedName(output.getSuggestedName());
            rename.setReason(output.getRenameReason());
            rename.setConfidence(output.getRenameConfidence());
            result.setRename(rename);
        }

        result.setSummary(output.getSummary());
        result.setKeywords(output.getKeywords());
        result.setConfidence(output.getConfidence());
        result.setTokenCost(output.getTokenCost());

        // 兜底
        applyFallbacks(result, originalFileName);
        return result;
    }

    private void applyFallbacks(AiOrganizeResult result, String originalFileName) { ... }
    private String getExistingDirs(Long userId) { ... }
    private String getExistingTags(Long userId) { ... }
}
```

### 4.4 Mock 切换说明

修改 `MockAiOrganizeServiceImpl.java`：

```java
@Service
// @Primary  <-- 移除这行
public class MockAiOrganizeServiceImpl implements IAiOrganizeService { ... }
```

**回退机制**：若 Dify 服务异常，可临时将 `@Primary` 从 `DifyAiOrganizeServiceImpl` 切回 `MockAiOrganizeServiceImpl`，无需改其他代码。

---

## 5. 表格填写助手对接规范（预留）

表格填写助手的 Dify 对接思路与自动规整一致，但存在以下差异：

| 维度 | 自动规整 | 表格填写助手 |
|------|----------|-------------|
| 接口 | `IAiOrganizeService.organize()` | `IAiFormService.extract()` / `generate()` |
| Dify Workflow 数量 | 1 个 | 2 个（字段识别 + 文档生成） |
| 输入 | 文件名 + 文本内容 | 文件内容（表格结构）/ 字段值映射 |
| 输出 | 元数据 JSON | 字段列表 / 填写后的文件 |
| 文件处理 | 不修改原文件 | 可能需要生成新文件（方式 A） |

**字段识别 Workflow**（`workflow-form-extract`）建议输入：
- `fileContent`：表格文本内容
- `fileType`：`pdf` / `docx` / `xlsx`

**文档生成 Workflow**（`workflow-form-generate`）建议输入：
- `originalFilePath`：原始空白表格路径
- `filledValues`：JSON 字符串，如 `{"姓名":"张三","学号":"2022010507"}`

> 表格填写助手的完整 Dify 对接文档待 AI 同学部署对应 Workflow 后补充。

---

## 6. 联调测试清单

### 6.1 测试用例

| # | 用例 | 预期结果 | 验证点 |
|---|------|----------|--------|
| 1 | 正常文本文件（简历.docx） | 返回完整 AiOrganizeResult | category/tags/rename/summary/keywords 均有值 |
| 2 | 空内容文件（fileContent=""） | 基于文件名推断 | suggestedName 和 category 有值，summary 可为空 |
| 3 | 非文本文件（图片.jpg） | fileContent=""，基于文件名+扩展名推断 | 不报错，返回基础分类建议 |
| 4 | Dify 网络超时 | 捕获异常，返回"AI分析超时" | 前端状态变为 failed |
| 5 | Dify 返回格式异常 | 捕获解析异常，返回"AI结果解析失败" | 记录原始响应便于排查 |
| 6 | Dify API Key 错误 | 返回 401，前端提示"AI服务配置错误" | 后端记录 error 日志 |

### 6.2 调试方法

1. **Postman/ curl 直接调用 Dify**：验证 Workflow 本身是否正常
2. **后端断点调试**：在 `DifyWorkflowClient.runWorkflow()` 处断点，查看 Request/Response
3. **日志打印**：在 `DifyAiOrganizeServiceImpl` 中打印 inputs 和 outputs JSON，便于与 AI 同学对齐字段名

---

## 7. 附录

### 7.1 相关代码文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| AI 服务接口 | `ruoyi-system/.../service/lingdoc/ai/IAiOrganizeService.java` | 需实现的接口 |
| Dify 实现 | `ruoyi-system/.../service/lingdoc/ai/impl/DifyAiOrganizeServiceImpl.java` | 当前 @Primary |
| Mock 实现 | `ruoyi-system/.../service/lingdoc/ai/impl/MockAiOrganizeServiceImpl.java` | 需移除 @Primary |
| 结果领域模型 | `ruoyi-system/.../service/lingdoc/ai/result/AiOrganizeResult.java` | Dify 输出映射目标 |
| 后端服务 | `ruoyi-system/.../service/lingdoc/LingdocInboxServiceImpl.java` | 调用 AI 服务的 Service 层 |
| 配置入口 | `ruoyi-admin/src/main/resources/application.yml` | 追加 Dify 配置 |

### 7.2 Dify API 官方文档

- Workflow API 文档：https://docs.dify.ai/guides/workflow
- API 鉴权说明：https://docs.dify.ai/getting-started/quickstart

### 7.3 快速联调命令

```bash
# 直接测试 Dify Workflow（替换为实际 inputs）
# 本地部署环境
curl -X POST http://localhost:5001/v1/workflows/run   -H "Authorization: Bearer app-MEV0eZXsC3AQwOjDCW3rmXNR"   -H "Content-Type: application/json"   -d '{
    "inputs": {"fileName":"test.pdf","fileContent":""},
    "response_mode": "blocking",
    "user": "user_test"
  }'
```

### 7.4 前后端联调地址速查

| 环节 | 地址 | 说明 |
|------|------|------|
| 浏览器入口 | `http://localhost:3000` | 前端 Vite 开发服务器 |
| 前端 API 代理 | `/dev-api` → `http://localhost:8080` | Vite 代理配置 |
| 后端直接访问 | `http://localhost:8080` | Spring Boot |
| Dify 直接访问 | `http://localhost:5001` | 本地 Dify 服务 |
