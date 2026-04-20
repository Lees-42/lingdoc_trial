# LingDoc Workspace

> 灵档（LingDoc）个人版开发工作区 —— 数据不出本地，算力按需借用。

## 仓库信息

| 项目 | 地址 |
|------|------|
| **GitHub 仓库** | `https://github.com/Lees-42/lingdoc_trial` |
| **主分支** | `main` |
| **Gitee 主仓库（骨架来源）** | `https://gitee.com/magician336/ling-doc` |

## 工作区结构

```
D:\lingdoc_trail
├── ling-doc/           # 主项目（Electron + Vue3 + Spring Boot）
│   ├── src/            # 前端源码（Vue3 + Vite + Element Plus）
│   ├── ruoyi-server/   # 后端源码（Spring Boot + MyBatis + Druid）
│   └── docs/           # 项目文档（SRS、架构设计、开发规范）
├── lingdoc-ai/         # AI 模块独立开发空间
│   ├── frontend/       # AI 前端代码
│   ├── backend/        # AI 后端代码
│   ├── sql/            # AI 模块数据库脚本
│   └── docs/           # AI 设计文档
├── ruoyi-vue/          # RuoYi-Vue 3.9.2 原始后端参考项目
├── ruoyi-vue3-kk/      # RuoYi-Vue3 前端参考项目
├── jdk17/              # 本地 JDK 17 环境
├── maven/              # 本地 Maven 3.9.6 环境
├── mysql-config/       # 本地 MySQL 配置
└── mysql-data/         # 本地 MySQL 数据
```

## 技术栈

**前端（`ling-doc/`）：**
- Vue 3.5.26（组合式 API + `<script setup>`）
- Vite 6.4.1（开发服务器端口 3000）
- Element Plus 2.13.1
- Pinia 3.0.4
- Vue Router 4.6.4
- Axios 1.13.2

**后端（`ling-doc/ruoyi-server/`）：**
- Spring Boot 4.0.3（JDK 17）
- MyBatis 4.0.1
- Druid 1.2.28
- MySQL 8.0+（本地，端口 3306，数据库 `ruoyi`）
- Redis 5.0+（本地，端口 6379）

## 联调链路

```
浏览器 → http://localhost:3000
  → Vite Dev Server (ling-doc 前端, 端口 3000)
  → 代理 /dev-api → http://localhost:8080
  → Spring Boot (ruoyi-admin, 端口 8080)
  → MySQL / Redis
```

## 快速启动

### 1. 启动 MySQL 和 Redis

确保本地 MySQL（端口 3306）和 Redis（端口 6379）已运行，数据库 `ruoyi` 已导入 `ling-doc/ruoyi-server/sql/ry_20260321.sql`。

### 2. 启动后端

```bash
cd ling-doc/ruoyi-server
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 3. 启动前端

```bash
cd ling-doc
npm install
npm run dev
```

### 4. 访问

打开 `http://localhost:3000`，默认账号 `admin` / `admin123`。

## AI 模块开发

AI 模块代码位于 `lingdoc-ai/` 目录，与主项目解耦：

- 独立开发 AI 前后端代码
- 联调时通过 `lingdoc-ai/integrate.ps1` 合并到 `ling-doc/`
- 详见 `lingdoc-ai/AGENTS.md` 和 `lingdoc-ai/INTEGRATE.md`

## 核心功能模块

1. **一键自动规整**：拖拽文件由 AI 自动命名并归类
2. **灵犀问答（自然语言对话）**：AI 从文档中提取答案
3. **数据本地化**：断网可用，数据仅存储本地
4. **历史版本溯源**：自动存档，可一键撤回
5. **智能表格填写助手**
6. **算力计费**：计量每次 AI 操作 Token
7. **审计溯源**：不可篡改的操作日志

## 默认账号

- 用户名：`admin`
- 密码：`admin123`
