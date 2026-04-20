# LingDoc AI 模块 — AI 编码代理须知

> 本文档面向 AI 编码代理。在操作 `lingdoc-ai/` 目录前，必须先阅读本文件。

---

## 🔴 绝对禁区（用户强制规则）

**违反以下任何一条，必须立即停止并报告用户：**

1. **只操作 `lingdoc-ai/` 目录**。`ling-doc/` 下的任何文件（前端源码 `src/`、`public/`、后端源码 `ruoyi-server/` 等）**绝对禁止直接修改**。
2. **前端代码禁止改动**。`ling-doc/src/` 下的 Vue 组件、API、路由、Store 等一律不动。
3. **禁止擅自删除 `ling-doc/` 下的任何代码**。如需清理或调整，必须先征得用户同意。
4. **如需联调**，仅通过本目录下的 `integrate.ps1` 将 `lingdoc-ai/` 的代码合并到 `ling-doc/`，执行前必须征得用户同意。
5. **用户只负责 AI 模块的开发**，其余系统管理、监控等基础设施模块不在用户维护范围内，不得擅自改动。

---

## 目录结构

```
lingdoc-ai/
├── frontend/           # AI 前端代码（Vue 组件、API）
│   ├── api/ai/         # AI 基础设施 API
│   ├── api/lingdoc/    # 灵档业务 API
│   └── views/lingdoc/  # AI 业务页面
├── backend/            # AI 后端代码（Spring Boot）
│   ├── controller/lingdoc/
│   ├── domain/lingdoc/
│   ├── mapper/lingdoc/
│   ├── service/lingdoc/
│   └── resources/mapper/lingdoc/
├── sql/                # 数据库脚本
├── docs/               # AI 设计文档
├── integrate.ps1       # 联调集成脚本
├── INTEGRATE.md        # 联调手册
└── AGENTS.md           # 本文件
```

## 与主项目的关系

- `lingdoc-ai/` 是 AI 模块的**独立开发空间**
- `ling-doc/` 是 Gitee 主仓库 `fast` 分支的克隆，是**联调基线**
- AI 代码在此开发成熟后，通过 `integrate.ps1` 合并回 `ling-doc/` 进行联调
- 开发完成后，将 `lingdoc-ai/` 的变更推送到 GitHub 独立仓库
