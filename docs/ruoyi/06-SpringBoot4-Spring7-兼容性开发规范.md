# Spring Boot 4 / Spring 7 / Spring Security 7 兼容性开发规范

> **高优先级必读**：本项目基于 Spring Boot 4.x（底层 Spring Framework 7.x / Spring Security 7.x）构建，与 Spring Boot 2.x / Spring 5.x 存在大量不兼容的语法差异。后续开发、引入第三方库、复制网络代码片段时，必须严格遵循本文档约束，否则将导致编译失败或运行时异常。

---

## 1. 版本矩阵

| 组件 | 当前版本 | 说明 |
|------|---------|------|
| Spring Boot | **4.0.5** | 根 `pom.xml` 的 `spring-boot.version` 锁定 |
| Spring Framework | **7.x** | 由 Spring Boot 4 自动管理，无需显式声明 |
| Spring Security | **7.0.3** | 已验证 `spring-security-test-7.0.3.jar` |
| Java | **17** | `maven-compiler-plugin` 的 `source` / `target` |
| Jakarta EE | **9+** | 全量迁移至 `jakarta.*` 命名空间 |
| MyBatis Spring Boot | **4.0.1** | `mybatis-spring-boot-starter` |
| Maven | **3.8+** | 建议 3.9+ |

> ⚠️ **严禁**引入仅支持 Spring Boot 2.x / Spring 5.x 的第三方库。引入新依赖前，务必到该库的 Maven Central 页面或 GitHub Release 页面确认其最低兼容版本。

---

## 2. 已完成的迁移（项目现状）

以下迁移工作已在现有代码中完成，后续开发**必须保持**这种写法：

### 2.1 Jakarta EE 命名空间迁移 ✅

所有 Jakarta EE 相关的包已从 `javax.*` 迁移至 `jakarta.*`：

| 旧包（禁止） | 新包（必须） | 项目现状 |
|------------|------------|---------|
| `javax.servlet.*` | `jakarta.servlet.*` | ✅ 已迁移，`ruoyi-common/pom.xml` 使用 `jakarta.servlet-api` |
| `javax.validation.*` | `jakarta.validation.*` | ✅ 已迁移，`SysUser.java` 使用 `jakarta.validation.constraints.*` |
| `javax.annotation.*`（`@PostConstruct` 等） | `jakarta.annotation.*` | ✅ 已迁移（如有使用） |

> **JDK 标准库中的 `javax.*` 不受影响**：`javax.sql.DataSource`、`javax.imageio.ImageIO`、`javax.net.ssl.*` 等属于 Java SE，**不是** Jakarta EE 的一部分，无需迁移，可继续使用。

### 2.2 Spring Security 配置迁移 ✅

Spring Security 7.x 已彻底移除 `WebSecurityConfigurerAdapter`。本项目 `SecurityConfig.java` 已采用新的 `SecurityFilterChain` Bean 模式：

```java
// ✅ 正确写法（本项目已采用）
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig
{
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> {
                requests.requestMatchers("/login").permitAll()
                        .anyRequest().authenticated();
            })
            .build();
    }
}
```

```java
// ❌ 绝对禁止（已在 Spring Security 6+ 中移除）
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter  // 编译报错！
{
    @Override
    protected void configure(HttpSecurity http) throws Exception {  // 方法不存在！
        http.csrf().disable();
    }
}
```

### 2.3 自动配置注册方式

本项目**未使用** `spring.factories` 进行自动配置注册，因此不涉及此项迁移。若后续需要添加 Starter 模块：

```
# ❌ 禁止（Spring Boot 2.x 方式）
META-INF/spring.factories

# ✅ 正确（Spring Boot 3+ / 4+ 方式）
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 3. 绝对禁止的语法

以下语法在本项目环境下**必然导致编译失败或运行时异常**，严禁使用：

### 3.1 包名层面

| 禁止的导入 | 后果 | 替代方案 |
|-----------|------|---------|
| `import javax.servlet.*;` | 编译失败：包不存在 | `import jakarta.servlet.*;` |
| `import javax.validation.*;` | 编译失败：包不存在 | `import jakarta.validation.*;` |
| `import javax.persistence.*;` | 编译失败：包不存在 | `import jakarta.persistence.*;` |
| `import javax.annotation.PostConstruct;` | 编译失败或运行时找不到类 | `import jakarta.annotation.PostConstruct;` |

### 3.2 Spring Security 层面

| 禁止的写法 | 后果 | 替代方案 |
|-----------|------|---------|
| `extends WebSecurityConfigurerAdapter` | 编译失败：类不存在 | 使用 `@Bean SecurityFilterChain filterChain(HttpSecurity)` |
| `http.csrf().disable()` | 编译失败：`csrf()` 返回类型已变 | `http.csrf(csrf -> csrf.disable())` |
| `http.authorizeRequests()` | 编译失败：方法已移除 | `http.authorizeHttpRequests(...)` |
| `http.sessionManagement().sessionCreationPolicy(...)` | 可能编译失败 | `http.sessionManagement(session -> session.sessionCreationPolicy(...))` |

### 3.3 Spring Boot 测试层面

| 禁止的假设 | 后果 | 替代方案 |
|-----------|------|---------|
| 假设 `@MockBean` 一定可用 | 某些构建环境下 `spring-boot-test` JAR 缺失 `mock.mockito` 包，导致编译失败 | 准备 fallback：Mockito `@Mock` + `MockMvcBuilders.standaloneSetup()`，或手动注入 `SecurityContextHolder` |
| 假设 `@AutoConfigureMockMvc` 一定可用 | 某些构建环境下 `spring-boot-test-autoconfigure` JAR 缺失 `web.servlet` 包 | 准备 fallback：`MockMvcBuilders.standaloneSetup(controller)` 或 `webAppContextSetup(context)` |

> 详细 fallback 方案见 [`docs/test/02-后端测试指南.md`](../test/02-后端测试指南.md) 第 8.2 节。

### 3.4 依赖引入层面

| 禁止的行为 | 后果 |
|-----------|------|
| 引入基于 `javax.servlet` 的第三方库（如旧版 `swagger`、`javax.ws.rs` 等） | 与 `jakarta.servlet-api` 冲突，导致启动失败或 `ClassNotFoundException` |
| 引入仅标注 "Spring Boot 2.x compatible" 的 Starter | 可能与 Spring Boot 4 的自动配置机制不兼容，导致启动失败 |
| 引入使用 `spring.factories` 但未提供 Spring Boot 3+ 变体的库 | 自动配置不会被加载 |

---

## 4. 依赖引入规范

### 4.1 引入新库前必读检查清单

在 `pom.xml` 中添加新依赖之前，必须确认：

1. **官方文档**是否明确声明支持 Spring Boot 3.x 或 4.x？
2. **Maven Central**上该库的最新版本是否使用了 `jakarta.*` 包？
3. 该库是否依赖了本项目已排除的旧版库（如 `javax.servlet:servlet-api`）？
4. 该库的许可证是否允许商用（Apache 2.0、MIT、BSD 等）？

### 4.2 优先选择原则

| 场景 | 优先选择 |
|------|---------|
| Jakarta Servlet | `jakarta.servlet:jakarta.servlet-api`（已由父 POM 管理） |
| Jakarta Validation | `jakarta.validation:jakarta.validation-api` + `hibernate-validator` |
| JSON 处理 | `com.alibaba.fastjson2:fastjson2`（项目已用）或 `jackson-databind` |
| HTTP 客户端 | `org.springframework.web.client.RestTemplate` / `RestClient` |
| 数据库连接池 | `com.alibaba:druid-spring-boot-4-starter`（项目已用） |

---

## 5. 测试层特殊约束

### 5.1 测试注解可用性

本项目测试依赖由 `spring-boot-starter-test`（版本跟随 Spring Boot 4.0.5）和 `spring-security-test`（版本 7.0.3）提供。在**大多数环境**下，以下注解正常工作：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@MockBean`
- `SecurityMockMvcRequestPostProcessors.authentication()`

但在**极少数构建环境**中，由于 `spring-boot-test` 和 `spring-boot-test-autoconfigure` JAR 可能存在包缺失，上述注解可能不可用。

### 5.2 防御式测试编写原则

**所有 Controller 集成测试必须遵循以下原则**：

1. **主方案**：使用 `@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockBean` + `.with(authentication(auth))`
2. **Fallback 方案**：如果主方案在目标环境中编译失败，必须能切换到 `standaloneSetup` + `@Mock` + `SecurityContextHolder` 手动注入
3. **禁止假设**：不要假设 `@MockBean` 一定存在；如果测试类使用了 `@MockBean`，请在类注释中说明 fallback 方案的位置

### 5.3 测试中的安全上下文

Controller 测试必须使用 `UsernamePasswordAuthenticationToken`，且其 principal 必须是 **`LoginUser`** 类型：

```java
// ✅ 正确
LoginUser loginUser = new LoginUser(1L, 100L, sysUser, permissions);
UsernamePasswordAuthenticationToken auth =
    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

mockMvc.perform(get("/xxx").with(authentication(auth)));
```

```java
// ❌ 错误：principal 不是 LoginUser，SecurityUtils.getLoginUser() 会抛 ClassCastException
mockMvc.perform(get("/xxx").with(user("admin").roles("admin")));
```

---

## 6. 自查清单（Code Review 用）

提交新功能或修改现有代码时，请对照以下清单自检：

### 6.1 通用代码检查

- [ ] 代码中**不存在** `javax.servlet`、`javax.validation`、`javax.persistence`、`javax.annotation.PostConstruct` 等 Jakarta EE 旧包导入
- [ ] 新增的配置类**没有**继承 `WebSecurityConfigurerAdapter`
- [ ] 新增的 Spring Security 配置使用了 Lambda DSL（`csrf -> csrf.disable()`）而非 Method Chaining（`csrf().disable()`）
- [ ] 新增的自动配置（如有）注册在 `META-INF/spring/*.imports` 中，而非 `spring.factories`

### 6.2 依赖检查

- [ ] 新增的第三方依赖官方文档明确支持 Spring Boot 3.x 或 4.x
- [ ] 新依赖没有传递引入 `javax.servlet:servlet-api`、`javax.validation:validation-api` 等旧包
- [ ] 新依赖版本与项目现有依赖无冲突（可通过 `mvn dependency:tree` 检查）

### 6.3 测试检查

- [ ] 新增的 Service 单元测试使用 Mockito `@Mock` + `@InjectMocks`，无 Spring 上下文依赖
- [ ] 新增的 Controller 集成测试使用了 `.with(authentication(auth))` 注入安全上下文
- [ ] `authentication()` 中的 principal 是 `LoginUser` 类型，而非普通 `UserDetails`
- [ ] 如果使用了 `@MockBean` / `@AutoConfigureMockMvc`，注释中注明了 fallback 方案
- [ ] 测试运行命令已验证通过：`mvn test -pl 对应模块 -Dtest=测试类名`

---

## 7. 网络代码片段引用规范

从网络（Stack Overflow、CSDN、博客等）复制代码片段到本项目时，必须执行以下转换：

1. **包名转换**：将所有 `javax.servlet`、`javax.validation`、`javax.persistence` 替换为 `jakarta.*` 对应包
2. **Security 配置转换**：将所有 `WebSecurityConfigurerAdapter`、`authorizeRequests()`、`csrf().disable()` 替换为 `SecurityFilterChain` + Lambda DSL
3. **版本确认**：确认该代码片段的来源文章发布于 2023 年之后，且明确声明支持 Spring Boot 3+；若来源是 2022 年或更早的文章，**默认不信任**，需到官方文档二次确认
4. **测试运行**：复制过来的代码必须经过 `mvn compile` 和对应模块的 `mvn test` 验证

---

## 8. 排障速查表

| 报错信息 | 根因 | 解决方案 |
|---------|------|---------|
| `package javax.servlet does not exist` | 使用了旧 Jakarta EE 包 | 替换为 `jakarta.servlet` |
| `cannot find symbol: class WebSecurityConfigurerAdapter` | Spring Security 7 已移除该类 | 改用 `SecurityFilterChain` Bean |
| `cannot find symbol: method authorizeRequests()` | Spring Security 7 已移除该方法 | 改用 `authorizeHttpRequests(...)` |
| `cannot find symbol: class AutoConfigureMockMvc` | `spring-boot-test-autoconfigure` 包缺失 | 使用 `MockMvcBuilders.standaloneSetup()` fallback |
| `cannot find symbol: class MockBean` | `spring-boot-test` 包缺失 `mock.mockito` | 使用 Mockito `@Mock` + `@InjectMocks` fallback |
| `获取用户信息异常`（测试时） | `SecurityContextHolder` 中无 `Authentication` | 测试请求添加 `.with(authentication(auth))` |
| `ClassCastException: User cannot be cast to LoginUser` | `authentication()` 的 principal 不是 `LoginUser` | 构造 `LoginUser` 作为 principal |

---

## 参考文档

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki)（发布时关注）
- [Spring Security 7.0 Migration Guide](https://docs.spring.io/spring-security/reference/7.0/migration/index.html)（发布时关注）
- [Jakarta EE 9 to 10 Migration](https://jakarta.ee/release/)
- 本项目测试指南：[`docs/test/02-后端测试指南.md`](../test/02-后端测试指南.md)
