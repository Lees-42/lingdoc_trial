# 后端安全认证与 JWT 过滤器

本文档解读后端的安全认证链路，重点分析 JWT Token 的生成、验证过程，以及 Spring Security 过滤器链的执行顺序。

---

## 1. 核心文件

| 文件 | 职责 |
|------|------|
| `ruoyi-framework/.../security/filter/JwtAuthenticationTokenFilter.java` | JWT Token 验证过滤器 |
| `ruoyi-framework/.../web/service/TokenService.java` | Token 的生成、解析、验证、刷新 |
| `ruoyi-framework/.../security/config/SecurityConfig.java` | Spring Security 配置 |
| `ruoyi-admin/.../web/controller/common/SysLoginController.java` | 登录/退出接口 |

---

## 2. JWT Token 认证流程

### 2.1 登录时 Token 生成

```
用户提交用户名密码
    ↓
SysLoginController.login()
    ↓
SysLoginService.login() 验证用户名密码
    ↓
验证成功 → TokenService.createToken(loginUser)
    ↓
生成 JWT Token（含 userId、userName、loginTime、expireTime）
    ↓
将 loginUser 缓存到 Redis（key: login_tokens:<uuid>）
    ↓
返回 Token 给前端
```

### 2.2 请求时 Token 验证

每个请求进入后端时，都会经过以下链路：

```
HTTP Request
    ↓
Tomcat 接收请求
    ↓
Spring Security 过滤器链
    ↓
JwtAuthenticationTokenFilter.doFilterInternal()
    ↓
TokenService.getLoginUser(request)
    ├── 从请求头读取 Authorization: Bearer <token>
    ├── 解析 JWT 获取 claims（userKey、userId、userName）
    └── 从 Redis 查询 loginUser 完整信息
    ↓
如果 loginUser 存在且 SecurityContext 为空
    ├── tokenService.verifyToken(loginUser)（检查是否过期，需要则刷新）
    ├── 创建 UsernamePasswordAuthenticationToken
    └── SecurityContextHolder.getContext().setAuthentication(token)
    ↓
chain.doFilter(request, response)（继续后续过滤器）
```

---

## 3. JwtAuthenticationTokenFilter 详解

```java
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain chain) {
        // 1. 从请求中解析登录用户信息
        LoginUser loginUser = tokenService.getLoginUser(request);
        
        // 2. 如果用户存在且当前上下文未认证
        if (loginUser != null && SecurityUtils.getAuthentication() == null) {
            // 3. 验证 Token 有效性（必要时刷新过期时间）
            tokenService.verifyToken(loginUser);
            
            // 4. 创建认证令牌
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    loginUser,           // 主体（principal）
                    null,                // 凭证（credentials）
                    loginUser.getAuthorities()  // 权限列表
                );
            
            // 5. 设置请求详情（IP、SessionId 等）
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            
            // 6. 将认证信息存入 Security 上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        // 7. 继续过滤器链
        chain.doFilter(request, response);
    }
}
```

### 3.1 OncePerRequestFilter

继承 `OncePerRequestFilter` 确保每个请求**只执行一次**过滤逻辑，避免在请求转发或包含时重复执行。

---

## 4. TokenService 核心方法

| 方法 | 职责 |
|------|------|
| `createToken(LoginUser)` | 生成 JWT Token，存入 Redis |
| `getLoginUser(HttpServletRequest)` | 从请求头解析 Token，查询 Redis 获取登录用户 |
| `verifyToken(LoginUser)` | 检查 Token 是否即将过期，如果是则刷新 Redis 过期时间 |
| `delLoginUser(String)` | 删除 Redis 中的 Token 记录（退出登录时调用） |

---

## 5. Spring Security 过滤器链

Spring Security 的过滤器链按以下顺序执行（部分关键过滤器）：

```
1. SecurityContextPersistenceFilter
   - 从 Session 中恢复 SecurityContext

2. LogoutFilter
   - 处理 /logout 请求

3. JwtAuthenticationTokenFilter（自定义）
   - 解析 JWT Token
   - 设置 SecurityContextHolder

4. RequestCacheAwareFilter
   - 缓存被拦截的请求

5. AnonymousAuthenticationFilter
   - 如果前面未认证，创建匿名认证

6. ExceptionTranslationFilter
   - 捕获 AccessDeniedException / AuthenticationException

7. FilterSecurityInterceptor
   - 最终权限校验（@PreAuthorize 等注解在此生效）
```

---

## 6. 权限注解的使用

在 Controller 方法上，使用 Spring Security 注解控制访问权限：

```java
@PreAuthorize("@ss.hasPermi('system:user:list')")
@GetMapping("/list")
public AjaxResult list(SysUser user) { ... }
```

- `@PreAuthorize`：方法执行前进行权限检查
- `@ss.hasPermi('system:user:list')`：自定义权限校验表达式，检查当前用户是否拥有 `system:user:list` 权限

权限数据来源：
- 用户登录时，后端查询 `sys_role` 和 `sys_menu` 表
- 将用户的权限标识符（如 `system:user:list`）存入 `LoginUser.authorities`
- JWT Token 中只携带用户标识，权限信息从 Redis 实时读取

---

## 7. 安全链路总结

```
【登录阶段】
前端提交账号密码
    ↓
后端验证 → 生成 JWT → 写入 Redis
    ↓
返回 Token 给前端 → 前端存入 Cookie

【请求阶段】
前端发送请求（Header: Authorization: Bearer <token>）
    ↓
JwtAuthenticationTokenFilter 拦截
    ↓
解析 JWT → 查 Redis → 获取 LoginUser
    ↓
验证 Token 有效性
    ↓
SecurityContextHolder.setAuthentication()
    ↓
Controller 方法执行
    ↓
@PreAuthorize 权限校验
    ↓
返回业务数据

【退出阶段】
前端调用 /logout
    ↓
后端删除 Redis 中的 Token 记录
    ↓
前端清除 Cookie 中的 Token
```
