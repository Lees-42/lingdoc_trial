# VSCode 一键编译指南

> 本配置让你在 VSCode 里按一个快捷键就能编译/启动整个项目。

---

## 快速开始

### 方式一：快捷键（推荐）

| 操作 | 快捷键 |
|---|---|
| **一键启动完整环境** | `Ctrl+Shift+B` |
| **运行任意任务** | `Ctrl+Shift+P` → 输入 `task` → 选择 `Tasks: Run Task` |

### 方式二：菜单

1. 按 `Ctrl+Shift+P`
2. 输入 `task`
3. 选择 `Tasks: Run Task`
4. 选择你想要的任务

---

## 任务列表

### 🚀 组合任务（常用）

| 任务名 | 说明 | 效果 |
|---|---|---|
| `🚀 一键启动完整开发环境` | 依次启动所有服务 | Redis → AI服务 → Celery → Java → 前端 |
| `🔨 一键全量编译` | 编译所有代码 | Java编译 + 前端依赖 + AI依赖检查 |
| `🛑 一键停止所有服务` | 停止所有后台进程 | 杀进程，清现场 |
| `📊 一键压测` | 运行负载测试 | 5并发填表压测 |

### 🔧 单独操作

| 任务名 | 说明 |
|---|---|
| `🔧 检查环境` | 检查 Java/Maven/Node/Python/Redis |
| `🔧 编译 Java 后端` | `mvn clean compile -pl ruoyi-admin -am` |
| `🟨 编译并启动 Java 后端` | 编译 + `mvn spring-boot:run` |
| `🟨 仅编译 Java 后端（不启动）` | 仅编译 |
| `🔧 安装前端依赖` | `npm install` |
| `🟪 启动前端` | `npm run dev` |
| `🟪 仅构建前端（生产）` | `npm run build` |
| `🔧 检查 AI 服务依赖` | 检查/创建 venv，安装依赖 |
| `🟦 启动 AI 服务 (后台)` | 后台启动 uvicorn |
| `🟦 启动 AI 服务 (前台调试)` | 前台启动，带 `--reload` |
| `🟩 启动 Celery Worker (后台)` | 后台启动 Worker |
| `🟩 启动 Celery Worker (前台调试)` | 前台启动 Worker |
| `📋 查看 AI 服务日志` | `tail -f /tmp/lingdoc-ai-service.log` |
| `📋 查看 Celery Worker 日志` | `tail -f /tmp/lingdoc-celery.log` |

---

## 调试配置

按 `F5` 或 `Ctrl+Shift+D` 打开调试面板，选择配置：

| 配置 | 用途 |
|---|---|
| `Debug Java Backend` | 调试 Java 后端，断点在 IntelliSense |
| `Debug Frontend (Chrome)` | 调试前端，自动打开 Chrome |
| `Debug AI Service` | 调试 Python AI 服务 |
| `Debug Load Test` | 调试压测脚本 |

---

## 推荐的 VSCode 扩展

打开 VSCode 后会自动提示安装以下扩展：

**必须安装：**
- `vscjava.vscode-java-pack` — Java 支持
- `Vue.volar` — Vue3 支持
- `ms-python.python` — Python 支持

**建议安装：**
- `eamodio.gitlens` — Git 历史
- `esbenp.prettier-vscode` — 代码格式化
- `dbaeumer.vscode-eslint` — JS/TS 检查

---

## 文件说明

```
.vscode/
├── tasks.json      ← 所有一键任务定义
├── settings.json   ← 工作区设置（格式/缩进/排除规则）
├── launch.json     ← 调试配置

scripts/
├── check-env.sh         ← 环境检查
├── check-redis.sh       ← Redis 检查+启动
├── check-ai-deps.sh     ← AI 依赖检查
├── start-ai-service.sh  ← 启动 AI 服务
├── start-celery.sh      ← 启动 Celery Worker
└── stop-all.sh          ← 停止所有服务
```

---

## 常见问题

### Q: `Ctrl+Shift+B` 不显示任务？
**解决：** 先按 `Ctrl+Shift+P` → `Tasks: Configure Default Build Task` → 选择 `🚀 一键启动完整开发环境`。

### Q: Java 编译报错 "Cannot run program git"？
**解决：** 终端执行一次 `git config --global user.name "Your Name"` 和 `git config --global user.email "you@example.com"`。

### Q: 任务执行后终端显示乱码？
**解决：** 这是颜色代码问题，不影响功能。或在 `settings.json` 里 `"terminal.integrated.enableProposedApi": false`。

### Q: 一键启动后前端端口被占用？
**解决：** 先运行 `🛑 一键停止所有服务`，再重新启动。

---

*生成时间: 2026-05-14*
