# LingDoc 全链路调试手册

> **目标**：指导开发者从零开始，依次启动数据库、后端、AI 服务、前端，并完成端到端联调验证。  
> **适用场景**：本地开发环境搭建、功能调试、Bug 复现定位。  
> **预计耗时**：30~60 分钟（首次搭建）。  
> **文档原则**：每一步都有 **验证命令** 和 **预期输出**，不跳过任何检查点。

---

## 一、调试架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      浏览器 (Chrome/Firefox)                  │
│                         http://localhost:80                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│  前端 (Vue3 + Vite)                                         │
│  ruoyi-vue3-kk/                                             │
│  端口: 80 (dev server 代理)                                  │
│  职责: 用户界面、文件上传、展示 AI 分析结果                    │
└──────────────────────────────┬──────────────────────────────┘
                               │ API 请求
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  主后端 (Spring Boot + RuoYi)                               │
│  ling-doc/ruoyi-server/                                     │
│  端口: 8080                                                  │
│  职责: 用户认证、权限校验、文件存储、请求转发到 AI 服务         │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               │ 内部 HTTP (X-Internal-Token)   │ JDBC
               ▼                               ▼
┌──────────────────────────┐    ┌─────────────────────────────┐
│  AI 服务 (FastAPI)       │    │  数据库 (MySQL 8.x)         │
│  lingdoc-ai-service/     │    │  端口: 3306                 │
│  端口: 8000              │    │  库名: ruoyi                │
│  职责: OCR、LLM 分析      │    │  职责: 业务数据、用户权限   │
└──────────────────────────┘    └─────────────────────────────┘
                               │
                               │ 缓存
                               ▼
                    ┌─────────────────────────┐
                    │  Redis 6.x+             │
                    │  端口: 6379             │
                    │  职责: Session/Token/缓存 │
                    └─────────────────────────┘
```

**服务启动顺序**（必须严格遵守）：
1. MySQL 数据库 → 2. Redis → 3. 后端 Spring Boot → 4. AI 服务 FastAPI → 5. 前端 Vue3

---

## 二、前置环境检查清单

在启动任何服务之前，先确认以下依赖已就绪。

### 2.1 系统依赖验证

```bash
# 检查 Java 版本（需要 17+）
java -version
# 预期输出: openjdk version "17" 或更高

# 检查 Maven
mvn -version
# 预期输出: Apache Maven 3.8.x 或更高

# 检查 Node.js（需要 18+）
node -v
# 预期输出: v18.x 或 v20.x

# 检查 npm
npm -v
# 预期输出: 9.x 或更高

# 检查 Python（需要 3.10~3.12，3.13 不兼容 PaddleOCR）
python3 --version
# 预期输出: Python 3.10.x / 3.11.x / 3.12.x

# 检查 MySQL
mysql --version
# 预期输出: mysql Ver 8.x

# 检查 Redis
redis-cli --version
# 预期输出: redis-cli 6.x / 7.x
```

**⚠️ 如果任何一项缺失或版本不对，请先安装后再继续。**

### 2.2 端口占用检查

确保以下端口未被占用：

```bash
# Linux/macOS
netstat -tlnp | grep -E ':(3306|6379|8080|8000|80)'
# 或
lsof -i :3306,:6379,:8080,:8000,:80

# Windows
netstat -ano | findstr "3306 6379 8080 8000 80"
```

**如果端口被占用**，记下进程 PID，在确认安全后结束进程：
```bash
# Linux
kill -9 <PID>

# 或改用其他端口（需要同步修改配置）
```

### 2.3 项目代码确认

```bash
cd /path/to/lingdoc_trial

# 确认分支是 main（或你要调试的分支）
git branch
# 预期: * main

# 拉取最新代码
git pull origin main

# 确认目录结构完整
ls -la ling-doc/ ruoyi-vue3-kk/ lingdoc-ai-service/
# 预期: 三个目录均存在
```

---

## 三、数据库层调试（MySQL + Redis）

### 3.1 MySQL 启动与配置

#### 步骤 1：启动 MySQL 服务

```bash
# Ubuntu/Debian
sudo systemctl start mysql
sudo systemctl status mysql
# 预期: Active: active (running)

# macOS (Homebrew)
brew services start mysql

# Windows
# 通过服务管理器启动 MySQL80
```

**验证命令**：
```bash
mysql -u root -p -e "SELECT 1;"
# 输入密码后应输出: 1
```

#### 步骤 2：创建数据库

```bash
mysql -u root -p
# 进入 MySQL 命令行后执行:

CREATE DATABASE IF NOT EXISTS ruoyi 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

SHOW DATABASES;
# 预期: 列表中包含 ruoyi

EXIT;
```

#### 步骤 3：导入基础数据（关键）

```bash
cd lingdoc_trial/ling-doc/ruoyi-server/sql/

# 按顺序执行 SQL 脚本（重要！顺序错了会报错）
mysql -u root -p ruoyi < ry_20260321.sql          # 1. RuoYi 基础表
mysql -u root -p ruoyi < 08-form-module-mysql.sql  # 2. 表格填写模块
mysql -u root -p ruoyi < 09-form-menu-update.sql   # 3. 菜单更新
mysql -u root -p ruoyi < 10-paddleocr-module-mysql.sql  # 4. PaddleOCR 模块
mysql -u root -p ruoyi < update_menu_form_assistant.sql   # 5. 表单助手菜单
mysql -u root -p ruoyi < migration_20260418_fix_lingdoc_menu.sql  # 6. 菜单修复
```

**⚠️ 常见问题**：`ERROR 1049 (42000): Unknown database 'ruoyi'`
- **原因**：步骤 2 没执行，数据库不存在
- **解决**：先执行 `CREATE DATABASE ruoyi`

**⚠️ 常见问题**：`ERROR 1062 (23000): Duplicate entry`  
- **原因**：重复导入，主键冲突
- **解决**：先清空数据库重新导入，或跳过已执行过的脚本

#### 步骤 4：验证表结构

```bash
mysql -u root -p ruoyi -e "SHOW TABLES;"
# 预期输出包含:
# sys_user, sys_menu, sys_role, sys_dept       (RuoYi 基础表)
# lingdoc_form_task, lingdoc_form_field        (表格填写)
# paddle_ocr_task                              (OCR 任务)
```

#### 步骤 5：确认数据库连接配置

编辑后端数据库配置：
```bash
# 文件路径: ling-doc/ruoyi-server/ruoyi-admin/src/main/resources/application-druid.yml
```

确认以下配置正确：
```yaml
spring:
    datasource:
        druid:
            master:
                url: jdbc:mysql://localhost:3306/ruoyi?...
                username: root          # 改成你的 MySQL 用户名
                password: your_password # 改成你的 MySQL 密码
```

**⚠️ 如果 MySQL 密码不是 `zxcv`，必须修改此文件，否则后端启动失败。**

---

### 3.2 Redis 启动与配置

#### 步骤 1：启动 Redis

```bash
# Ubuntu/Debian
sudo systemctl start redis-server
sudo systemctl status redis-server

# macOS
brew services start redis

# Docker 方式（推荐，最稳定）
docker run -d --name redis-lingdoc \
  -p 6379:6379 \
  --restart unless-stopped \
  redis:7-alpine
```

#### 步骤 2：验证 Redis 连接

```bash
redis-cli ping
# 预期输出: PONG

redis-cli info server | grep redis_version
# 预期输出: redis_version:7.x.x
```

#### 步骤 3：确认后端 Redis 配置

编辑 `ling-doc/ruoyi-server/ruoyi-admin/src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      host: localhost    # 如果 Redis 在 Docker 里，改为 127.0.0.1
      port: 6379
      password:          # 如果没有密码，留空
      database: 0
```

---

## 四、后端层调试（Spring Boot）

### 4.1 编译后端代码

```bash
cd lingdoc_trial/ling-doc/ruoyi-server/

# 清理并编译
mvn clean compile

# 预期输出:
# [INFO] BUILD SUCCESS
# [INFO] Total time: 30-60s
```

**⚠️ 常见问题**：`Could not transfer artifact ... Connection reset`
- **原因**：Maven 仓库网络问题
- **解决**：配置阿里云镜像，编辑 `~/.m2/settings.xml`：
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

### 4.2 启动后端服务

```bash
# 方式 1: 命令行启动（推荐调试）
cd ruoyi-admin/target/classes
java -jar ../ruoyi-admin-3.9.2.jar

# 方式 2: IDEA 中直接运行 RuoYiApplication.java（开发推荐）
# 找到类: com.ruoyi.RuoYiApplication
# 右键 → Run
```

**首次启动日志关键检查点**：
```
# 检查点 1: 数据库连接
[main] c.a.d.p.DruidDataSource   : {dataSource-1} inited
# → 看到 inited 表示数据库连接成功

# 检查点 2: Redis 连接
[main] o.s.d.r.c.RedisConnectionFactory : Redis connection factory configured
# → Redis 连接成功

# 检查点 3: 服务启动完成
[main] c.r.RuoYiApplication      : Started RuoYiApplication in 15.234 seconds
# → 看到 Started 表示后端启动完成
```

### 4.3 后端独立验证

#### 验证 1：Swagger API 文档

浏览器访问：
```
http://localhost:8080/swagger-ui.html
```

预期：看到 RuoYi API 文档界面，左侧有控制器列表。

#### 验证 2：登录接口

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 预期输出: {"code":200,"msg":"操作成功","token":"..."}
```

**默认账号**：
- 用户名: `admin`
- 密码: `admin123`

#### 验证 3：文件上传接口（AI 模块前置）

```bash
# 先登录获取 Token（上一步返回的 token）
TOKEN="your-token-here"

# 测试文件上传
curl -X POST http://localhost:8080/lingdoc/ocr/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/test.txt"

# 预期: 返回文件存储路径
```

### 4.4 后端日志查看

```bash
# 实时查看日志
tail -f ling-doc/ruoyi-server/ruoyi-admin/logs/sys-info.log

# 错误日志
tail -f ling-doc/ruoyi-server/ruoyi-admin/logs/sys-error.log
```

**关键日志位置**：
- `sys-info.log`：常规信息
- `sys-error.log`：错误堆栈
- 控制台输出：启动日志、SQL 执行

---

## 五、AI 服务层调试（FastAPI）

### 5.1 环境准备

```bash
cd lingdoc_trial/lingdoc-ai-service/

# 1. 创建虚拟环境
python3 -m venv venv

# 2. 激活
source venv/bin/activate  # Linux/macOS
# venv\Scripts\activate   # Windows

# 3. 验证在虚拟环境中
which python
# 预期: /path/to/lingdoc-ai-service/venv/bin/python
```

### 5.2 安装依赖

```bash
# 基础依赖
pip install -r requirements.txt

# OCR 引擎（体积大，约 1.5GB，耗时 5-10 分钟）
pip install paddleocr paddlepaddle

# 验证安装
python -c "from paddleocr import PaddleOCR; print('PaddleOCR OK')"
python -c "import fastapi; print(f'FastAPI {fastapi.__version__}')"
```

**⚠️ 常见问题**：Python 3.13 安装 paddlepaddle 失败
- **原因**：PaddlePaddle 不支持 Python 3.13
- **解决**：降级到 Python 3.12
```bash
pyenv install 3.12.3
pyenv local 3.12.3
```

### 5.3 配置环境变量

```bash
# 复制模板
cp .env.example .env

# 编辑（必须填）
nano .env
```

必填项：
```bash
DASHSCOPE_API_KEY=sk-your-actual-key-here
AI_INTERNAL_TOKEN=lingdoc-ai-2026-your-secret-token
```

**⚠️ 如何获取 DASHSCOPE_API_KEY**：
1. 访问 https://dashscope.aliyun.com/
2. 登录 → API-KEY 管理 → 创建新 Key

### 5.4 启动 AI 服务

```bash
# 开发模式（热重载，推荐调试）
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 生产模式
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 2
```

**启动成功标志**：
```
============================================================
LingDoc AI Service 启动中...
============================================================
✅ 配置校验通过
🚀 服务启动完成 | 监听: 0.0.0.0:8000
📖 API 文档: http://0.0.0.0:8000/docs
```

**⚠️ 如果显示配置缺失**：
```
❌ 配置校验失败：必填配置缺失
  - DASHSCOPE_API_KEY
  - AI_INTERNAL_TOKEN
```
→ 检查 `.env` 文件是否存在且内容正确

### 5.5 AI 服务独立验证

#### 验证 1：健康检查（无需认证）

```bash
curl http://localhost:8000/api/ai/v1/health

# 预期输出:
{
  "code": 200,
  "msg": "服务运行中",
  "data": {
    "status": "ok",
    "ocr_ready": true,
    "llm_ready": true,
    "config": {
      "model": "qwen3-max",
      "ocr_threshold": 0.5,
      "pdf_dpi": 150
    }
  }
}
```

**检查点**：
- `ocr_ready: true` → PaddleOCR 正常
- `llm_ready: true` → DashScope API Key 有效

#### 验证 2：认证机制

```bash
# 未带 Token，应返回 401
curl -X POST http://localhost:8000/api/ai/v1/doc/qa \
  -H "Content-Type: application/json" \
  -d '{"question":"test","file_path":"test.txt"}'

# 预期: HTTP/1.1 401 Unauthorized
```

#### 验证 3：AI 分析（带 Token）

```bash
# 准备测试文件
echo "电路实验报告：验证基尔霍夫电压定律。测量数据：R1=3.2V, R2=4.8V, 电源=10V。结论：KVL成立。" > /tmp/test_ai.txt

# 调用处理接口
curl -X POST http://localhost:8000/api/ai/v1/doc/process \
  -H "X-Internal-Token: lingdoc-ai-2026-your-secret-token" \
  -F "file=@/tmp/test_ai.txt" \
  -F "user_id=1"

# 预期: 返回 JSON，包含 ocr_text, ai_result（摘要、关键词、分类）
```

**⚠️ 常见问题**：`ocr_ready: false`
- **原因**：PaddleOCR 未安装或初始化失败
- **解决**：`pip install paddleocr paddlepaddle`，重启服务

**⚠️ 常见问题**：`llm_ready: false`
- **原因**：DashScope API Key 无效或额度用完
- **解决**：检查 `.env` 中的 Key，在阿里云控制台确认额度

### 5.6 AI 服务日志查看

```bash
# 控制台实时输出（默认）
# 或配置文件中指定日志文件路径

# 查看详细日志级别（调试 OCR 问题时）
export AI_LOG_LEVEL=DEBUG
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

---

## 六、前端层调试（Vue3）

### 6.1 安装前端依赖

```bash
cd lingdoc_trial/ruoyi-vue3-kk/

# 使用 npm（推荐）
npm install

# 或使用 yarn
yarn install
```

**⚠️ 常见问题**：`npm install` 卡住或报错
- **原因**：网络问题或 Node 版本不兼容
- **解决 1**：换淘宝镜像
  ```bash
  npm config set registry https://registry.npmmirror.com
  npm install
  ```
- **解决 2**：Node 版本不对时，使用 nvm 切换
  ```bash
  nvm use 18
  ```

### 6.2 配置前端代理

编辑 `ruoyi-vue3-kk/vite.config.js`，确认代理配置指向正确后端端口：

```javascript
server: {
  port: 80,
  proxy: {
    // 代理到后端 8080
    '/dev-api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/dev-api/, '')
    }
  }
}
```

**⚠️ 如果后端端口不是 8080，必须修改此处。**

### 6.3 启动前端

```bash
npm run dev

# 预期输出:
# VITE v6.x.x  ready in 500 ms
# ➜  Local:   http://localhost:80/
```

### 6.4 前端独立验证

浏览器访问：
```
http://localhost/
```

预期：看到 RuoYi 登录页面。

**登录测试**：
- 用户名: `admin`
- 密码: `admin123`

登录成功后应看到管理后台首页。

---

## 七、全链路联调

### 7.1 启动顺序检查

确保所有服务按顺序启动并运行：

```bash
# 检查点 1: MySQL
mysql -u root -p -e "SELECT 1;"
# → 输出 1

# 检查点 2: Redis
redis-cli ping
# → PONG

# 检查点 3: 后端
curl http://localhost:8080/swagger-ui.html
# → HTTP 200

# 检查点 4: AI 服务
curl http://localhost:8000/api/ai/v1/health
# → JSON 响应，ocr_ready=true

# 检查点 5: 前端
# 浏览器访问 http://localhost
# → 登录页面
```

### 7.2 端到端测试：完整 AI 文档处理流程

#### 场景：用户上传实验报告 → AI 分析 → 前端展示结果

**步骤 1：前端登录**
1. 浏览器访问 `http://localhost`
2. 输入 admin / admin123 登录
3. 确认登录成功，进入首页

**步骤 2：进入 AI 功能页面**
1. 左侧菜单找到「灵档 AI」→「文档处理」
2. 或「灵档 AI」→「OCR 识别」（根据实际菜单）

**步骤 3：上传文件**
1. 点击「上传文件」按钮
2. 选择一个测试文件（PDF / Word / 图片 / 文本）
3. 点击「开始分析」

**步骤 4：观察后端日志**
```bash
tail -f ling-doc/ruoyi-server/ruoyi-admin/logs/sys-info.log
```

预期看到：
```
[接收文件] user=1, filename=test_report.pdf, size=1024000
[转发 AI 服务] POST http://localhost:8000/api/ai/v1/doc/process
[AI 返回] task_id=xxx, status=success, tokens=800
[保存结果] task_id=xxx, user_id=1
```

**步骤 5：观察 AI 服务日志**
```
[OCR:xxx] 开始处理: /path/to/test_report.pdf
[OCR:xxx] 完成 | 页数=3 | 字符=1500 | 耗时=1200ms
[LLM:xxx] 文档分析完成 | tokens=800 | 耗时=5000ms
```

**步骤 6：前端展示结果**
- 页面应显示：
  - 文档摘要（AI 生成）
  - 关键词标签（可点击）
  - 分类（实验报告 / 课程笔记 / 试卷等）
  - 学科（电路理论 / 数据结构等）

### 7.3 联调问题排查速查表

| 现象 | 定位方法 | 可能原因 | 解决方案 |
|------|----------|----------|----------|
| 前端登录失败 | 浏览器 F12 → Network | 后端未启动 / 跨域问题 | 检查后端端口、vite 代理配置 |
| 上传文件失败 | 后端日志 `sys-error.log` | 文件大小超限 / 权限不足 | 修改 `application.yml` multipart 配置 |
| 后端报 500 | 后端日志搜索 Exception | AI 服务未启动 / Token 错误 | 检查 AI 服务状态、Token 一致性 |
| AI 服务报 401 | AI 服务控制台 | X-Internal-Token 不匹配 | 对比 `.env` 和后端配置中的 Token |
| AI 分析结果为空 | AI 服务日志 | API Key 无效 / 额度用完 | 检查 DashScope Key、余额 |
| OCR 识别乱码 | AI 服务日志 | 系统缺少中文字体 | `apt install fonts-noto-cjk` |
| 前端页面空白 | 浏览器 F12 → Console | Vue 编译错误 / API 404 | 检查 `npm run dev` 输出、路由配置 |
| 数据库连接失败 | 后端启动日志 | MySQL 未启动 / 密码错误 | 检查 MySQL 服务、application-druid.yml |
| Redis 连接失败 | 后端启动日志 | Redis 未启动 / 配置错误 | 检查 Redis 服务、application.yml |

---

## 八、配置一致性检查清单

### 8.1 Token 配置（最容易出错）

确保以下 **三个地方** 的 `AI_INTERNAL_TOKEN` 完全一致：

| 位置 | 文件 | 配置项 |
|------|------|--------|
| AI 服务 | `lingdoc-ai-service/.env` | `AI_INTERNAL_TOKEN=xxx` |
| 后端（Java） | `ling-doc/.../application.yml` | `ai.service.token=xxx`（如已添加） |
| 后端转发代码 | `PaddleOcrServiceImpl.java` | HTTP Header `X-Internal-Token: xxx` |

**验证方法**：
```bash
# 查看 AI 服务的 Token
grep AI_INTERNAL_TOKEN lingdoc-ai-service/.env

# 查看后端配置（如果有）
grep ai.service.token ling-doc/ruoyi-server/.../application.yml
```

### 8.2 端口配置

| 服务 | 默认端口 | 配置文件 | 修改影响 |
|------|----------|----------|----------|
| MySQL | 3306 | 系统服务 | 后端 `application-druid.yml` 需同步 |
| Redis | 6379 | 系统服务 / Docker | 后端 `application.yml` 需同步 |
| 后端 | 8080 | `application.yml` | 前端 `vite.config.js` 代理需同步 |
| AI 服务 | 8000 | `.env` `AI_SERVICE_PORT` | 后端转发 URL 需同步 |
| 前端 | 80 | `vite.config.js` | 浏览器访问地址 |

### 8.3 文件路径配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ruoyi.profile` | `D:/ruoyi/uploadPath` | 上传文件存储路径 |
| `AI_UPLOAD_DIR` | `/tmp/lingdoc/uploads` | AI 服务接收文件路径 |

**Linux 环境下必须修改**：
```yaml
# application.yml
ruoyi:
  profile: /home/ruoyi/uploadPath   # Linux 路径
```

---

## 九、调试工具推荐

### 9.1 后端调试

- **IDEA Debugger**：在 `PaddleOcrServiceImpl.java` 打断点，逐行查看文件上传和转发逻辑
- **Druid 监控**：`http://localhost:8080/druid/index.html` 查看 SQL 执行情况
- **Swagger UI**：`http://localhost:8080/swagger-ui.html` 直接调用 API 测试

### 9.2 AI 服务调试

- **Swagger UI**：`http://localhost:8000/docs` 交互式测试 API
- **日志级别**：修改 `.env` 中 `AI_LOG_LEVEL=DEBUG` 查看详细日志
- **curl 命令**：命令行快速测试（见 5.5 节）

### 9.3 前端调试

- **浏览器 F12**：Network 面板查看 API 请求/响应
- **Vue DevTools**：Chrome 插件，查看组件状态和 Vuex/Pinia
- **Vite HMR**：代码修改后自动刷新，无需手动重启

---

## 十、一键启动脚本（参考）

```bash
#!/bin/bash
# save as: start-all.sh
# 用途：一键启动全链路服务（适合本地开发）

echo "=== LingDoc 全链路启动 ==="

# 1. MySQL
echo "[1/5] 启动 MySQL..."
sudo systemctl start mysql || echo "⚠️ MySQL 启动失败，请手动检查"

# 2. Redis
echo "[2/5] 启动 Redis..."
sudo systemctl start redis-server || docker start redis-lingdoc || echo "⚠️ Redis 启动失败"

# 3. 后端
echo "[3/5] 启动后端..."
cd ling-doc/ruoyi-server/
mvn spring-boot:run &
BACKEND_PID=$!
cd ../..

# 4. AI 服务
echo "[4/5] 启动 AI 服务..."
cd lingdoc-ai-service/
source venv/bin/activate
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 &
AI_PID=$!
cd ..

# 5. 前端
echo "[5/5] 启动前端..."
cd ruoyi-vue3-kk/
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "=== 所有服务已启动 ==="
echo "MySQL:     localhost:3306"
echo "Redis:     localhost:6379"
echo "后端:      http://localhost:8080"
echo "AI 服务:   http://localhost:8000"
echo "前端:      http://localhost"
echo ""
echo "查看日志:"
echo "  后端:   tail -f ling-doc/ruoyi-server/ruoyi-admin/logs/sys-info.log"
echo "  AI:     前台输出（或配置日志文件）"
echo ""
echo "停止所有服务: kill $BACKEND_PID $AI_PID $FRONTEND_PID"
```

---

## 十一、文档版本与维护

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-04-22 | 初始版本，覆盖全链路调试 |

**反馈渠道**：在 GitHub Issues 提交问题，附上：
1. 执行的步骤编号
2. 实际输出（截图或复制文本）
3. 预期输出（根据本文档）
4. 环境信息（OS、Java 版本、Node 版本、Python 版本）

---

*本文档配套代码版本: main 分支 760740c 及之后*  
*最后更新: 2026-04-22*
