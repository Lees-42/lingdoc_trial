# LingDoc Workspace — AI Agent Onboarding

> 本文档面向 AI 编码代理。每次进入本工作区时，请优先阅读本文件及引用文档，建立项目上下文。

---

## 🔴 绝对禁区（用户强制规则）

**违反以下任何一条，必须立即停止并报告用户：**

1. **只操作 `lingdoc-ai/` 目录**。`ling-doc/` 下的任何文件（前端源码 `src/`、`public/`、后端源码 `ruoyi-server/` 等）**绝对禁止直接修改**。
2. **前端代码禁止改动**。`ling-doc/src/` 下的 Vue 组件、API、路由、Store 等一律不动。
3. **禁止擅自删除 `ling-doc/` 下的任何代码**。如需清理或调整，必须先征得用户同意。
4. **如需联调**，仅通过 `lingdoc-ai/integrate.ps1` 将 `lingdoc-ai/` 的代码合并到 `ling-doc/`，执行前必须征得用户同意。
5. **用户只负责 AI 模块的开发**，其余系统管理、监控等基础设施模块不在用户维护范围内，不得擅自改动。

---

## 1. 工作区总体结构

`D:\lingdoc_trail` 是灵档（LingDoc）项目的开发工作区，采用 Monorepo 风格，包含以下核心目录：

| 目录 | 说明 | 重要性 |
|------|------|--------|
| `ling-doc/` | **主项目** — 灵档个人版（Electron + Vue3 + Spring Boot） | ⭐ 核心 |
| `ruoyi-server/` (在 `ling-doc/` 内) | 后端完整代码（RuoYi-Vue 3.9.2） | ⭐ 核心 |
| Gitee 主仓库 | `https://gitee.com/magician336/ling-doc` | ⭐ 骨架来源 |
| `ruoyi-vue/` | RuoYi-Vue 3.9.2 原始后端参考项目 | 参考 |
| `ruoyi-vue3-kk/` | RuoYi-Vue3 前端参考项目 | 参考 |
| `jdk17/` | 本地 JDK 17 环境 | 运行时 |
| `maven/` | 本地 Maven 3.9.6 环境 | 构建时 |
| `mysql-config/`, `mysql-data/` | 本地 MySQL 数据和配置 | 运行时 |

---

## 2. 主项目 `ling-doc/` 概况

### 2.1 项目定位
灵档（LingDoc）个人版是一款基于 **Electron + RuoYi-Vue3 + Spring Boot** 的跨平台个人文档管理工具，核心理念为"**数据不出本地，算力按需借用**"。

### 2.2 技术栈

**前端（根目录 `ling-doc/`）：**
- Vue 3.5.26（组合式 API + `<script setup>`）
- Vite 6.4.1（开发服务器端口 **3000**）
- Element Plus 2.13.1
- Pinia 3.0.4
- Vue Router 4.6.4
- Axios 1.13.2

**后端（`ling-doc/ruoyi-server/`）：**
- Spring Boot 4.0.3（JDK 17）
- MyBatis 4.0.1
- Druid 1.2.28
- MySQL 8.0+（本地，端口 3306，数据库 `ruoyi`，账号 `root/zxcv`）
- Redis 5.0+（本地，端口 6379，无密码）
- Maven 3.8+

### 2.3 联调链路

```
浏览器 → http://localhost:3000
  → Vite Dev Server (ling-doc 前端, 端口 3000)
  → 代理 /dev-api → http://localhost:8080
  → Spring Boot (ruoyi-admin, 端口 8080)
  → MySQL / Redis
```

### 2.4 核心功能模块
1. **一键自动规整**：拖拽文件由 AI 自动命名并归类
2. **灵犀问答（自然语言对话）**：AI 从文档中提取答案
3. **数据本地化**：断网可用，数据仅存储本地
4. **历史版本溯源**：自动存档，可一键撤回
5. **智能表格填写助手**
6. **算力计费**：计量每次 AI 操作 Token
7. **审计溯源**：不可篡改的操作日志

### 2.5 前后端联调步骤

1. 确保 MySQL 和 Redis 已启动（本地数据在 `mysql-data/`）
2. 数据库 `ruoyi` 已存在并导入 `ling-doc/ruoyi-server/sql/ry_20260321.sql`
3. 启动后端：`ling-doc/ruoyi-server/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`
4. 启动前端：`cd ling-doc && npm run dev`
5. 访问 `http://localhost:3000`，默认账号 `admin` / `admin123`

### 2.6 关键目录结构

```
ling-doc/
├── src/                    # 前端源码（精简版 RuoYi-Vue3 UI）
│   ├── api/                # HTTP 接口封装
│   │   ├── ai/             # AI 相关接口
│   │   ├── lingdoc/        # 灵档业务接口
│   │   ├── system/         # 系统管理接口
│   │   └── monitor/        # 监控接口
│   ├── views/              # 页面视图
│   │   ├── lingdoc/        # 灵档业务页面（organize, search, knowledge, form, version, analysis）
│   │   ├── system/         # 系统页面（config, dict, user/profile）
│   │   └── monitor/        # 监控页面（operlog）
│   ├── components/         # 全局组件
│   ├── store/modules/      # Pinia Store
│   └── utils/              # 工具函数
├── ruoyi-server/           # 后端源码（完整 RuoYi-Vue 3.9.2）
│   ├── ruoyi-admin/        # Web 入口 + Controller
│   ├── ruoyi-system/       # 系统管理 Service/Mapper/Domain
│   ├── ruoyi-framework/    # 框架核心（AOP、Security、拦截器）
│   ├── ruoyi-common/       # 公共工具
│   ├── ruoyi-generator/    # 代码生成
│   ├── ruoyi-quartz/       # 定时任务
│   └── sql/                # 数据库初始化脚本
├── docs/                   # 项目文档（规范、SRS、开发指南）
│   ├── ruoyi/              # RuoYi 框架开发规范
│   ├── spec/               # 产品需求规格说明书
│   └── ai/                 # AI 模块规范
├── vite.config.js          # Vite 配置 + 代理
└── package.json            # 前端依赖
```

### 2.7 开发规范速查

- **前端组件**：`PascalCase.vue`
- **API 函数**：`verb-noun` 模式，如 `listConfig`, `getXxx`, `addXxx`, `updateXxx`, `delXxx`
- **后端分层**：Controller → Service → Mapper → Domain
- **返回格式**：统一 `AjaxResult`
- **按钮权限**：`v-hasPermi="['system:example:add']"`
- **编码**：全链路 UTF-8

### 2.8 安全与敏感信息

- JWT Token 有效期 30 分钟，密钥在 `application.yml`
- `.env.development`、`.env.production`、`.env.staging` 含敏感变量
- `application-druid.yml` 含数据库密码 `root/zxcv`
- 生产环境务必替换强密码

---

## 3. 其他项目

### `ruoyi-vue/`
- RuoYi-Vue 3.9.2 原始后端完整项目（Maven 多模块）
- 用作参考或备份，主项目后端在 `ling-doc/ruoyi-server/`

### `ruoyi-vue3-kk/`
- RuoYi-Vue3 前端参考项目（Vue3 + Vite + Element Plus）
- 与 `ling-doc/` 前端技术栈基本一致

---

## 4. 每次启动必做检查

1. **确认当前工作目录**：`D:\lingdoc_trail`
2. **确认主项目位置**：`ling-doc/`
3. **确认后端配置**：检查 `ling-doc/ruoyi-server/ruoyi-admin/src/main/resources/application-druid.yml` 的数据库连接
4. **确认前端代理**：`ling-doc/vite.config.js` 中的 `baseUrl` 是否为 `http://localhost:8080`
5. **阅读规范**：任何操作前优先查看 `ling-doc/docs/` 下对应规范文件

---

## 5. 参考文档索引

| 文档路径 | 内容 |
|---------|------|
| `ling-doc/AGENTS.md` | 灵档项目详细的 AI 编码代理须知 |
| `ling-doc/README.md` | 项目快速启动与功能简介 |
| `ling-doc/docs/ruoyi/01-当前项目结构说明.md` | 单体仓库骨架详解 |
| `ling-doc/docs/ruoyi/02-核心模块开发指南.md` | 保留模块的扩展规范 |
| `ling-doc/docs/ruoyi/03-新增页面与接口规范.md` | 新增页面、API、Store 的标准模板 |
| `ling-doc/docs/ruoyi/04-定制变更与兼容记录.md` | 历史变更记录 |
| `ling-doc/docs/ruoyi/05-后端迁移与启动指南.md` | 后端环境配置与启动方式 |
| `ling-doc/docs/spec/01-SRS-需求说明书.md` | 灵档产品需求规格说明书 |
| `ling-doc/docs/spec/09-AI模块架构设计.md` | AI 模块架构设计 |
| `ling-doc/docs/spec/10-AI模块开发规范.md` | AI 模块编码规范 |
