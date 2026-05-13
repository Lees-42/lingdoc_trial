# 灵档项目本地部署手册

> 分支: `feature/performance-kb-upgrade`  
> 适用场景: 本地开发/测试环境部署  
> 预期时间: 15-30 分钟

---

## 一、前提条件检查

在开始之前，确认以下环境已安装：

| 组件 | 最低版本 | 检查命令 |
|---|---|---|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| Python | 3.10+ | `python3 --version` |
| Redis | 6+ | `redis-cli ping` |
| Git | 任意 | `git --version` |

如果 Redis 没装：
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install redis-server
sudo systemctl start redis

# 或用 Docker
docker run -d -p 6379:6379 --name redis redis:7-alpine
```

---

## 二、获取代码

```bash
# 克隆仓库
git clone https://github.com/Lees-42/lingdoc_trial.git
cd lingdoc_trial

# 切换到本次开发的分支
git checkout feature/performance-kb-upgrade
```

---

## 三、部署 AI 服务（Python 后端）

这是整个链路的核心，**必须先启动**。

### 3.1 创建虚拟环境

```bash
cd lingdoc-ai-service
python3 -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
```

### 3.2 安装依赖

```bash
pip install -r requirements.txt
```

### 3.3 配置环境变量

创建 `.env` 文件：

```bash
cat > .env << 'EOF'
# DashScope API Key（用于 AI 调用）
DASHSCOPE_API_KEY=sk-1786cb3f70ef4291a5514430cde941f8

# 内部通信 Token（Java 后端调用 AI 服务时携带）
AI_INTERNAL_TOKEN=lingdoc-ai-2026-a7f3e9d2b8c1e4f5

# 模型配置
LLM_MODEL=qwen3-max

# Redis
REDIS_URL=redis://localhost:6379/0
EOF
```

> ⚠️ **注意**：`AI_INTERNAL_TOKEN` 必须与 Java 后端 `application.yml` 中的配置一致。

### 3.4 启动 AI 服务

```bash
# 终端 1：启动 FastAPI 服务
nohup ./venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > /tmp/lingdoc-ai-service.log 2>&1 &

# 验证
sleep 2
curl http://localhost:8000/api/ai/v1/health
# 应返回: {"status":"ok"}
```

### 3.5 启动 Celery Worker（**关键步骤**）

```bash
# 终端 2：启动 Celery Worker
# ⚠️ 必须指定 -Q form,document,celery，否则任务不会被消费
nohup ./venv/bin/celery -A app.celery_app worker --loglevel=info --concurrency=2 -Q form,document,celery > /tmp/lingdoc-celery.log 2>&1 &

# 验证 Worker 状态
./venv/bin/celery -A app.celery_app inspect active
# 应看到: 1 node online
```

> 🔥 **踩坑记录**：之前 Worker 没加 `-Q` 参数，导致 `fill_form_task` 任务提交后一直 PENDING，Worker 完全不消费。原因是 `celery_app.py` 里 `task_routes` 把任务路由到了 `form` 队列，但默认 Worker 只监听 `celery` 队列。

---

## 四、部署 Java 后端

### 4.1 进入后端目录

```bash
cd ../ruoyi-server
```

### 4.2 配置数据库

编辑 `ruoyi-admin/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:lingdoc.db
    driver-class-name: org.sqlite.JDBC
  
  redis:
    host: localhost
    port: 6379
    database: 0

# AI 服务配置
lingdoc:
  ai:
    service-url: http://localhost:8000
    internal-token: lingdoc-ai-2026-a7f3e9d2b8c1e4f5
```

> ⚠️ `internal-token` 必须与 AI 服务的 `.env` 中 `AI_INTERNAL_TOKEN` 完全一致。

### 4.3 设置 Git 用户信息（首次编译必需）

```bash
git config --global user.name "Developer"
git config --global user.email "dev@example.com"
```

### 4.4 编译

```bash
mvn clean compile -pl ruoyi-admin -am
```

> 如果报错 `Cannot run program "git"`，通常是 Maven 插件尝试获取 git commit 信息失败。已通过在 `pom.xml` 中配置 `failOnNoGitDirectory=false` 解决，如果仍报错，可尝试 `mvn compile -DskipTests`。

### 4.5 启动

```bash
cd ruoyi-admin
mvn spring-boot:run
```

后端启动后会监听 `http://localhost:8080`。

---

## 五、部署前端

### 5.1 进入前端目录

```bash
cd ../../src   # 从 ruoyi-server 回到项目根，再进入 src/
```

### 5.2 安装依赖

```bash
npm install
```

### 5.3 配置 API 代理

编辑 `vite.config.js`，确认代理配置：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

### 5.4 启动开发服务器

```bash
npm run dev
```

前端默认在 `http://localhost:5173`，会自动打开浏览器。

---

## 六、完整启动顺序（建议按此顺序）

```
终端 1: Redis
  redis-server

终端 2: AI 服务
  cd lingdoc-ai-service
  source venv/bin/activate
  ./venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000

终端 3: Celery Worker
  cd lingdoc-ai-service
  source venv/bin/activate
  ./venv/bin/celery -A app.celery_app worker --loglevel=info --concurrency=2 -Q form,document,celery

终端 4: Java 后端
  cd ruoyi-server/ruoyi-admin
  mvn spring-boot:run

终端 5: 前端
  cd src
  npm run dev
```

---

## 七、验证步骤

### 7.1 服务健康检查

```bash
# AI 服务
curl http://localhost:8000/api/ai/v1/health

# Java 后端
curl http://localhost:8080

# Redis
redis-cli ping
```

### 7.2 端到端填表测试

```bash
# 提交异步填表任务
curl -X POST http://localhost:8000/api/ai/v1/form/fill/async \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: lingdoc-ai-2026-a7f3e9d2b8c1e4f5" \
  -d '{
    "task_id": "test_001",
    "reference_paths": ["/tmp/lingdoc/test_doc.docx"],
    "template_path": "/tmp/lingdoc/test_form.docx",
    "output_path": "/tmp/lingdoc/output/test_001.docx"
  }'

# 查询任务进度（每 2 秒执行一次）
curl http://localhost:8000/api/ai/v1/form/status/test_001 \
  -H "X-Internal-Token: lingdoc-ai-2026-a7f3e9d2b8c1e4f5"
```

### 7.3 查看 Celery Worker 日志

```bash
tail -f /tmp/lingdoc-celery.log
```

正常应看到：
```
Task app.tasks.fill_form_task[xxx] received
[Celery] fill_form_task 开始, task_id=test_001
[E2E:test_001] 开始端到端填表
...
[Celery] fill_form_task 完成, task_id=test_001, duration=21805ms
```

---

## 八、常见问题排查

### Q1: Celery Worker 启动后任务一直 PENDING
**原因**: Worker 没监听正确的队列。  
**解决**: 确保启动命令包含 `-Q form,document,celery`。

### Q2: `AttributeError: 'coroutine' object has no attribute 'get'`
**原因**: Celery 任务里调用了异步函数，但没有 `await` 或 `asyncio.run()`。  
**解决**: 已在 `tasks.py` 中修复，`fill_form_task` 里使用 `asyncio.run(form_service.fill_form_end_to_end(...))`。

### Q3: Java 后端启动报错 `Cannot run program "git"`
**原因**: Maven 的 git-commit-id 插件尝试获取 git 信息失败。  
**解决**: 设置 git user.name 和 user.email，或添加 `-DskipTests`。

### Q4: 前端页面空白，控制台报 404
**原因**: 前端 API 代理未配置正确，或 Java 后端未启动。  
**解决**: 检查 `vite.config.js` 代理目标是否为 `http://localhost:8080`，确认 Java 后端已启动。

### Q5: AI 服务返回 403 Unauthorized
**原因**: `X-Internal-Token` 不匹配。  
**解决**: 检查 AI 服务 `.env` 和 Java 后端 `application.yml` 中的 `internal-token` 是否一致。

---

## 九、关闭服务

```bash
# AI 服务
pkill -f "uvicorn app.main:app"

# Celery Worker
pkill -f "celery -A app.celery_app worker"

# Java 后端
pkill -f "ruoyi-admin"

# 前端 (Ctrl+C 即可)

# Redis
sudo systemctl stop redis
# 或: docker stop redis
```

---

## 十、快速重启脚本（可选）

创建一个 `start-all.sh`：

```bash
#!/bin/bash
cd "$(dirname "$0")"

# Redis
redis-server --daemonize yes

# AI Service
cd lingdoc-ai-service
source venv/bin/activate
nohup ./venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > /tmp/lingdoc-ai.log 2>&1 &

# Celery Worker
nohup ./venv/bin/celery -A app.celery_app worker --loglevel=info --concurrency=2 -Q form,document,celery > /tmp/lingdoc-celery.log 2>&1 &

echo "AI 服务和 Celery Worker 已启动"
echo "请手动启动 Java 后端: cd ruoyi-server/ruoyi-admin && mvn spring-boot:run"
echo "请手动启动前端: cd src && npm run dev"
```

---

*文档生成时间: 2026-05-14*  
*对应分支: feature/performance-kb-upgrade*
