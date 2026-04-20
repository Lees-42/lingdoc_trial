# 后端 Spring Boot 启动流程

本文档解读后端应用的启动入口、自动配置加载顺序，以及 `application.yml` 中的核心配置项。

---

## 1. 启动入口

```
ruoyi-server/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java
```

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("灵档启动成功");
    }
}
```

---

## 2. @SpringBootApplication 注解拆解

该注解是三个注解的复合：

```java
@Configuration          // 标记为配置类
@EnableAutoConfiguration// 启用 Spring Boot 自动配置
@ComponentScan          // 扫描当前包及子包的 Spring Bean
```

### 2.1 排除 DataSourceAutoConfiguration

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
```

**原因**：项目使用 **Druid** 作为数据库连接池，需要在 `ruoyi-framework` 中手动配置数据源。排除默认的 `DataSourceAutoConfiguration` 可避免冲突。

---

## 3. 启动时加载的模块

Spring Boot 启动时会自动扫描并加载以下模块中的 Spring Bean：

```
com.ruoyi
├── RuoYiApplication（启动类）
├── web/controller/...（ruoyi-admin 中的控制器）
└── framework/...（ruoyi-framework 中的配置类）
    ├── security/（Spring Security 配置）
    ├── config/（Druid、Redis、MyBatis 配置）
    ├── interceptor/（拦截器注册）
    └── aspectj/（AOP 切面）
```

由于 Maven 依赖关系，`ruoyi-system`、`ruoyi-common` 等模块的类也会被加载到类路径中。

---

## 4. 核心配置解读（application.yml）

### 4.1 服务器配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

- **端口**：8080
- **上下文路径**：根路径 `/`
- **编码**：强制 UTF-8（防止中文乱码）

### 4.2 数据源配置

```yaml
spring:
  profiles:
    active: druid
```

激活 `application-druid.yml`，其中包含：
- MySQL 连接地址、用户名、密码
- Druid 连接池参数（初始连接数、最大连接数等）

### 4.3 Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:
      lettuce:
        pool:
          max-active: 8
```

Redis 用途：
- 存储用户登录 Token 的缓存副本
- 存储字典数据缓存
- 分布式锁、会话缓存

### 4.4 Token 配置（JWT）

```yaml
token:
  header: Authorization
  secret: abcdefghijklmnopqrstuvwxyz
  expireTime: 30
```

- **header**：前端请求头中携带 Token 的字段名
- **secret**：JWT 签名密钥（生产环境务必更换为强密码）
- **expireTime**：Token 有效期 30 分钟

### 4.5 MyBatis 配置

```yaml
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain
  mapperLocations: classpath*:mapper/**/*Mapper.xml
  configLocation: classpath:mybatis/mybatis-config.xml
```

- **typeAliasesPackage**：自动为 `domain` 包下的类创建别名
- **mapperLocations**：扫描所有 `*Mapper.xml` 文件
- **configLocation**：MyBatis 全局配置文件

### 4.6 分页插件配置

```yaml
pagehelper:
  helperDialect: mysql
  supportMethodsArguments: true
```

集成 PageHelper 实现物理分页，自动适配 MySQL 语法。

### 4.7 文件上传限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

- 单文件最大 10MB
- 单次请求最大 20MB

### 4.8 XSS 过滤配置

```yaml
xss:
  enabled: true
  excludes: /system/notice
  urlPatterns: /system/*,/monitor/*,/tool/*
```

对指定路径的请求参数进行 XSS 过滤，防止跨站脚本攻击。

---

## 5. 启动生命周期

```
1. JVM 加载 RuoYiApplication.class
    ↓
2. SpringApplication.run() 启动 Spring 容器
    ↓
3. 加载 application.yml + application-druid.yml
    ↓
4. 自动配置类执行：
   - DataSourceAutoConfiguration（已排除）
   - RedisAutoConfiguration
   - JacksonAutoConfiguration
   - ServletWebServerFactoryAutoConfiguration
    ↓
5. 扫描并注册 Spring Bean：
   - @Controller、@Service、@Mapper、@Component
    ↓
6. 执行自定义配置类：
   - SecurityConfig（Spring Security 过滤器链）
   - DruidConfig（数据源）
   - MyBatisConfig（SqlSessionFactory）
    ↓
7. 初始化拦截器和过滤器：
   - JwtAuthenticationTokenFilter
   - RepeatSubmitInterceptor
   - XSSFilter
    ↓
8. 内嵌 Tomcat 启动，监听端口 8080
    ↓
9. 控制台输出启动成功信息
```

---

## 6. 环境配置切换

| 配置文件 | 用途 |
|---------|------|
| `application.yml` | 通用配置（所有环境共享） |
| `application-druid.yml` | 数据源配置（开发/生产可分别维护） |
| `application-dev.yml` | 开发环境（如有） |
| `application-prod.yml` | 生产环境（如有） |

当前项目通过 `spring.profiles.active: druid` 激活数据源配置，可根据需要扩展为 `dev`、`prod` 等多环境配置。
