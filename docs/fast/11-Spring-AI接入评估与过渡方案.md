# Spring AI 接入评估与过渡方案

**文档编号**: LingDoc-FAST-011  
**版本**: v1.1  
**编制日期**: 2026-04-23  
**状态**: 定稿（已更新 Java 21 评估）  

---

## 1. 评估结论（TL;DR）

> **当前阶段不适合接入 Spring AI（无论是 1.x 还是 2.0 M1）。**

建议策略：**等 Spring AI 2.0 GA 发布后再正式接入**。在此期间，采用**原生 HTTP 调用方案**作为过渡，保持现有 `IAiOrganizeService` 接口不变，内部用 `RestClient` 直接调用 LLM API。

---

## 2. 后端技术栈现状

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | **4.0.5** | 基于 Spring Framework 7.x |
| Spring Security | **7.0.3** | 已移除 `WebSecurityConfigurerAdapter`，采用 `SecurityFilterChain` Bean 模式 |
| Spring Framework | **7.x** | Jakarta EE 11 基线 |
| Jackson | **3.x** | Spring Boot 4 强制要求，与 Jackson 2.x 不兼容 |
| Java | **17** | 兼容 Spring Boot 4 最低要求 |
| Jakarta EE | **11** | 全量 `jakarta.*` 命名空间 |
| MyBatis Spring Boot | **4.0.1** | 已验证兼容 Spring Boot 4 |

### 2.5 Java 版本升级可行性分析

| 评估维度 | 结论 | 说明 |
|---------|------|------|
| 向后兼容性 | ✅ **完全兼容** | Java 21 JVM 可 100% 运行 Java 17 编译的字节码 |
| 编译兼容性 | ✅ **无需改代码** | 286 个 Java 文件全部使用传统语法，无 Record / Pattern Matching / Sealed Classes 等 |
| JDK 内部访问 | ✅ **无风险** | 全代码库未发现 `sun.misc.*`、`Unsafe`、`-add-opens` 等依赖 |
| 依赖兼容性 | ✅ **全部通过** | Spring Boot 4.0.5、MyBatis 4.0.1、Druid 1.2.28、Jackson 3.x 等均已验证支持 Java 21 |
| 唯一操作 | ⚠️ **需安装 JDK 21** | 当前系统为 OpenJDK 17.0.2，需额外安装 Java 21 并修改 `pom.xml` 中 `java.version` |

> **结论：Java 17 → 21 升级对本项目风险极低，是接入 Spring AI 2.0-M2 的可行前置步骤。**

---

## 3. Spring AI 兼容性矩阵

| Spring AI 版本 | 支持的 Spring Boot | 依赖基线 | 当前状态 | 是否适合本项目 |
|----------------|-------------------|---------|---------|---------------|
| **1.0 GA** | 3.4.x / 3.5.x | Jackson 2.x + Spring Framework 6.x | 已发布（2025-05） | ❌ **完全不兼容** |
| **1.1.x** | 3.5.x | Jackson 2.x + Spring Framework 6.x | 维护中 | ❌ **不兼容** |
| **2.0 M1** | 4.0.x | Jackson 3.x + Spring Framework 7.x | Milestone（2025-12） | ⚠️ **技术兼容，但 API 不稳定** |
| **2.0 M2** | 4.0.x | Jackson 3.x + Spring Framework 7.x + **Java 21** | Milestone（2025-12 后） | ⚠️ **技术兼容，需升级 Java 21** |
| **2.0 GA** | 4.0.x | Jackson 3.x + Spring Framework 7.x + **Java 21** | 预计 2026 年中 | ✅ **建议生产环境等待此版本** |

> 参考来源：
> - [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/getting-started.html)：明确声明 "Spring AI supports Spring Boot 3.4.x and 3.5.x"
> - [Spring Boot 4 Upgrade Playbook](https://loiane.com/2026/04/spring-boot-3-eol-to-4-upgrade-playbook-jackson-3/)："Spring AI 1.x aligns to Spring Boot 3.5, while Spring AI 2.0 tracks Boot 4"
> - [GitHub Discussion #5149](https://github.com/spring-projects/spring-ai/discussions/5149)：社区用户验证 2.0 M1 可在 Boot 4.0.1 运行，但明确声明 "won't move this to production until Spring AI 2 is GA"

---

## 4. 风险分析

### 4.1 版本冲突（已解除）

**Spring AI 1.x**：全家桶硬编码依赖 **Jackson 2.x** 和 **Spring Framework 6.x**，与本项目 **Jackson 3.x + Spring Framework 7.x** 完全不兼容，已排除。

**Spring AI 2.0-M2**：依赖基线与项目完全一致：
- ✅ Jackson 3.x（包名 `tools.jackson.*`）
- ✅ Spring Framework 7.x
- ✅ Spring Boot 4.0.x
- ✅ Jakarta EE 11
- ⚠️ **Java 21（强制要求，当前项目为 17）**

### 4.2 API 稳定性（高）

Spring AI 2.0 M1 处于早期里程碑阶段：
- M1 → M2 → M3 之间已确认会调整 `ChatClient` 和 `Advisor` API
- 官方路线图显示 2.0 GA 前还有多轮 API 重构
- 若现在接入，GA 发布后可能需要大面积重写业务代码

### 4.3 安全与 CVE（高）

2026 年已披露 Spring AI 1.x 的漏洞：
- **CVE-2026-22729**：JSONPath 注入
- **CVE-2026-22730**：SQL 注入

2.0 M1 尚未经过充分的安全审计，生产环境使用存在合规风险。

### 4.4 项目规范约束（中）

`docs/ruoyi/06-SpringBoot4-Spring7-兼容性开发规范.md` 第 6.2 条明确要求：
> "新增第三方依赖官方文档明确支持 Spring Boot 3.x 或 4.x"

Spring AI 官方文档目前**仅明确支持 3.4.x / 3.5.x**，2.0 M1 不满足"生产可用"标准。

---

## 5. 推荐过渡方案：原生 HTTP 调用

### 5.1 设计原则

- **接口不变**：保持现有 `IAiOrganizeService` / `IAiFormService` 等接口定义
- **实现替换**：`MockAiOrganizeServiceImpl` 替换为 `HttpAiOrganizeServiceImpl`
- **零额外依赖**：使用 Spring Boot 4 原生 `RestClient`（或 `WebClient`），不引入任何 AI 框架 starter
- **可迁移性**：Spring AI 2.0 GA 后，只需替换实现类内部逻辑，接口与业务代码无需改动

### 5.2 架构对比

```
┌─────────────────────────────────────────────────────────────────┐
│                          当前状态                                │
│  Controller ──► IAiOrganizeService ──► MockAiOrganizeServiceImpl │
│                                              (空壳返回)           │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 过渡方案
┌─────────────────────────────────────────────────────────────────┐
│                          目标状态                                │
│  Controller ──► IAiOrganizeService ──► HttpAiOrganizeServiceImpl │
│                                              RestClient ──► LLM │
└─────────────────────────────────────────────────────────────────┘
                              ↓ 未来迁移（Spring AI 2.0 GA 后）
┌─────────────────────────────────────────────────────────────────┐
│                     最终状态（可选）                              │
│  Controller ──► IAiOrganizeService ──► SpringAiOrganizeServiceImpl│
│                                              ChatClient ──► LLM │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 代码示例：RestClient 实现

```java
@Service
@Primary
public class HttpAiOrganizeServiceImpl implements IAiOrganizeService
{
    private final RestClient restClient;

    @Value("${lingdoc.ai.base-url:}")
    private String baseUrl;

    @Value("${lingdoc.ai.api-key:}")
    private String apiKey;

    @Value("${lingdoc.ai.model:gpt-4o-mini}")
    private String model;

    public HttpAiOrganizeServiceImpl(RestClient.Builder restClientBuilder)
    {
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public AiOrganizeResult organize(String fileId, String filePath, String fileName,
                                     String fileContent, Long userId)
    {
        // 1. 构建系统提示词
        String systemPrompt = buildSystemPrompt(userId);

        // 2. 构建用户提示词
        String userPrompt = buildUserPrompt(fileName, fileContent);

        // 3. 发送请求
        AiChatResponse response = restClient.post()
            .uri("/v1/chat/completions")
            .body(Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
            ))
            .retrieve()
            .body(AiChatResponse.class);

        // 4. 解析 JSON 响应为 AiOrganizeResult
        return parseOrganizeResult(response);
    }
}
```

### 5.4 配置示例（application.yml）

```yaml
lingdoc:
  ai:
    # OpenAI 兼容接口（支持 OneAPI / Ollama / 本地模型中转）
    base-url: "https://api.openai.com"
    api-key: "${OPENAI_API_KEY:}"
    model: "gpt-4o-mini"
    # 超时与重试
    connect-timeout: 10s
    read-timeout: 60s
    max-retries: 2
```

### 5.5 Prompt 工程规范

为保证 AI 返回 JSON 可被可靠解析，需强制启用 JSON Mode（或 Structured Output）：

```json
{
  "model": "gpt-4o-mini",
  "messages": [
    {
      "role": "system",
      "content": "你是一个文档整理助手。请分析用户上传的文件，返回 JSON 格式的整理建议。必须严格遵循以下 JSON Schema：{...}"
    },
    {
      "role": "user",
      "content": "文件名：{fileName}\n文件内容摘要：{fileContent}"
    }
  ],
  "response_format": { "type": "json_object" }
}
```

返回 JSON Schema 需与 `AiOrganizeResult` 字段一一对应，确保 `parseOrganizeResult()` 使用 Jackson 3 可靠反序列化。

### 5.6 接入 Spring AI 2.0-M2 的前置条件清单

若决定在开发/测试环境实验性接入 2.0-M2（非生产），需完成以下前置步骤：

| 步骤 | 操作 | 验证方式 |
|------|------|---------|
| 1 | 安装 **Java 21 JDK**（Eclipse Temurin / Amazon Corretto / Oracle JDK） | `java -version` 输出 `openjdk version "21"` |
| 2 | 修改根 `pom.xml`：`<java.version>17</java.version>` → `21` | `mvn compile` 全模块编译通过 |
| 3 | 修改 `maven-compiler-plugin` 的 `source` / `target` 为 21 | 同上 |
| 4 | 运行全量单元测试与集成测试 | `mvn test` 无失败 |
| 5 | 验证前后端联调无异常 | `npm run dev` + 后端启动，登录及核心功能正常 |
| 6 | 引入 `spring-ai-bom:2.0.0-M2` 并排除冲突依赖 | `mvn dependency:tree` 无红色冲突 |

> **生产环境门槛**：即使完成以上全部步骤，生产环境仍建议等待 **Spring AI 2.0 GA**。

---

## 6. 未来迁移路线图

### 阶段零：Java 21 升级（如需接入 2.0-M2）
- 安装 Java 21 JDK，修改 `pom.xml` 中 `java.version`
- 全量编译与测试验证
- 此步骤独立于 Spring AI 接入，可提前完成

### 阶段一：过渡实现（当前 → Spring AI 2.0 GA 前）
- 使用 `HttpAiOrganizeServiceImpl` + `RestClient` 完成 AI 功能上线
- 积累 Prompt 模板、JSON Schema、异常处理经验
- 监控 Token 消耗与响应延迟，为后续调优提供数据

### 阶段二：评估迁移（Spring AI 2.0 GA 发布后）
- 验证 Spring AI 2.0 GA 与项目所有依赖的兼容性（`mvn dependency:tree`）
- 在独立分支引入 `spring-ai-bom:2.0.x`，跑通单元测试和集成测试
- 评估迁移收益：是否能显著减少样板代码、是否支持所需的全部模型（OpenAI / Ollama / 本地模型）

### 阶段三：正式迁移（验证通过后）
- 将 `HttpAiOrganizeServiceImpl` 替换为 `SpringAiOrganizeServiceImpl`
- 保留 `IAiOrganizeService` 接口不变，业务代码零改动
- 逐步将手动 Prompt 管理迁移到 Spring AI 的 `PromptTemplate` 和 `ChatClient`
- 使用 `HttpAiOrganizeServiceImpl` + `RestClient` 完成 AI 功能上线
- 积累 Prompt 模板、JSON Schema、异常处理经验
- 监控 Token 消耗与响应延迟，为后续调优提供数据

### 阶段二：评估迁移（Spring AI 2.0 GA 发布后）
- 验证 Spring AI 2.0 GA 与项目所有依赖的兼容性（`mvn dependency:tree`）
- 在独立分支引入 `spring-ai-bom:2.0.x`，跑通单元测试和集成测试
- 评估迁移收益：是否能显著减少样板代码、是否支持所需的全部模型（OpenAI / Ollama / 本地模型）

### 阶段三：正式迁移（验证通过后）
- 将 `HttpAiOrganizeServiceImpl` 替换为 `SpringAiOrganizeServiceImpl`
- 保留 `IAiOrganizeService` 接口不变，业务代码零改动
- 逐步将手动 Prompt 管理迁移到 Spring AI 的 `PromptTemplate` 和 `ChatClient`

---

## 7. 与现有文档的衔接

| 本文档章节 | 衔接文档 | 衔接内容 |
|-----------|---------|---------|
| §5.3 代码示例 | FAST-004 §3 | `IAiOrganizeService.organize()` 的输入输出契约 |
| §5.5 Prompt 规范 | FAST-004 §5.2 | AI 返回的 JSON 需满足 `AiOrganizeResult` 字段映射 |
| §4.4 项目规范 | `docs/ruoyi/06` | Spring Boot 4 / Spring Security 7 兼容性约束 |
| §6 迁移路线 | FAST-001 / FAST-009 | 不影响现有功能排期，AI 模块为可插拔设计 |

---

## 8. 决策 Checklist

接入 Spring AI 前，必须全部满足以下条件：

- [ ] **Java 21** 已安装且 `mvn compile` 全模块通过
- [ ] Spring AI 2.0 **GA** 已发布（非 Milestone / RC）
- [ ] 官方文档明确声明支持 **Spring Boot 4.0.x**
- [ ] `mvn dependency:tree` 验证无 Jackson 2.x 传递依赖冲突
- [ ] 与 Spring Security 7.x 的集成测试全部通过
- [ ] 无未修复的高危 CVE（CVSS ≥ 7.0）
- [ ] 所需 AI 模型提供商（OpenAI / Ollama / 本地）均有官方 starter 支持

---

## 9. 附录：参考链接

1. [Spring AI 官方文档 - Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
2. [Spring AI GitHub Discussion #5149 - Spring Boot 4 兼容性](https://github.com/spring-projects/spring-ai/discussions/5149)
3. [Spring Boot 4 Upgrade Playbook - Loiane](https://loiane.com/2026/04/spring-boot-3-eol-to-4-upgrade-playbook-jackson-3/)
4. [Spring Boot 4 Release Notes](https://github.com/spring-projects/spring-boot/wiki)
5. 本项目兼容性规范：`docs/ruoyi/06-SpringBoot4-Spring7-兼容性开发规范.md`
