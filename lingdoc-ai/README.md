# LingDoc AI 模块（独立仓库）

> 本仓库包含灵档（LingDoc）个人版的所有 AI/灵档业务代码，与主项目骨架完全解耦。

## 仓库信息

| 项目 | 地址 |
|------|------|
| **GitHub 仓库** | `https://github.com/Lees-42/lingdoc_trial` |
| **主分支** | `main` |
| **开发分支** | `fast` |
| **Gitee 主仓库（骨架）** | `https://gitee.com/magician336/ling-doc` |

## 背景

- **Gitee 主仓库（骨架）**：`https://gitee.com/magician336/ling-doc`
  - 包含前端架构、后端基础设施、系统管理模块。
  - 前端架构和后端功能架构设计随时从该仓库拉取更新。
- **GitHub AI 仓库（本库）**：`https://github.com/Lees-42/lingdoc_trial`
  - 包含灵档所有 AI 业务功能的前后端代码、数据库脚本、设计文档。
  - AI 部分独立开发、独立上传。

## 目录结构

```
lingdoc-ai/
├── frontend/          # 前端 AI 代码
│   ├── api/ai/        # AI 基础设施 API（对话、知识库、模型、检索）
│   ├── api/lingdoc/   # 灵档业务 API（表格、规整、检索、版本）
│   └── views/lingdoc/ # AI 业务页面（6 个核心功能页）
├── backend/           # 后端 AI 代码
│   ├── controller/lingdoc/   # Controller 层
│   ├── domain/lingdoc/       # Entity 实体
│   ├── mapper/lingdoc/       # Mapper 接口
│   ├── service/lingdoc/      # Service 接口与实现
│   └── resources/mapper/lingdoc/  # MyBatis XML
├── sql/               # AI 模块数据库脚本
├── docs/              # AI 模块设计文档与规范
│   ├── ai/            # AI 前端 API 规范、现状分析
│   ├── fast/          # 表格填写助手、Vault 等需求与设计方案
│   └── spec/          # AI 模块架构设计与开发规范
├── INTEGRATE.md       # 联调集成指南
└── integrate.ps1      # 一键集成脚本
```

## 核心功能

| 功能 | 前端位置 | 后端位置 | 状态 |
|------|---------|---------|------|
| 表格填写助手 | `frontend/views/lingdoc/form/` | `backend/controller/lingdoc/LingdocFormController.java` | 前后端已落地，AI 核心待实现 |
| 一键自动规整 | `frontend/views/lingdoc/organize/` | 后端 Controller 缺失 | 前端 Mock |
| 灵犀问答（自然语言检索） | `frontend/views/lingdoc/search/` | 后端 Controller 缺失 | 前端 Mock |
| 知识库管理 | `frontend/views/lingdoc/knowledge/` | 后端 Controller 缺失 | 前端已联调结构 |
| 知识图谱 | `frontend/views/lingdoc/graph/` | 后端 Controller 缺失 | 待实现 |
| 版本溯源 | `frontend/views/lingdoc/version/` | 后端 Controller 缺失 | 前端 Mock |
| AI 对话（基础设施） | `frontend/api/ai/chat.js` | 后端 Controller 缺失 | 前端已封装 |
| 向量/全文/混合检索 | `frontend/api/ai/search.js` | 后端 Controller 缺失 | 前端已封装 |
| 模型管理 | `frontend/api/ai/model.js` | 后端 Controller 缺失 | 前端已封装 |

## 使用方式

### 方式一：独立查看/编辑
直接在 `lingdoc-ai/` 内修改代码，提交到本仓库 `main` 分支。

```bash
git checkout main
git add .
git commit -m "feat: xxx"
git push origin main
```

### 方式二：与主项目联调
执行一键集成脚本，将 AI 代码复制回 `ling-doc/`：

```powershell
# 在 lingdoc-ai/ 目录下执行
.\integrate.ps1
```

详细步骤见 [INTEGRATE.md](./INTEGRATE.md)。

## 注意事项

- `ling-doc/` 是 Gitee 主仓库的纯净 clone，**不要在其中直接修改 AI 代码**。
- Gitee 主仓库更新后，重新 `git pull`，再执行 `integrate.ps1` 合并 AI 代码即可联调。
- 后端 Swagger 扫描包配置需要扩展，详见 `INTEGRATE.md`。
- **注意**：AI 模块开发仅在 `lingdoc-ai/` 内进行，`ling-doc/` 作为联调基线不直接修改。详见 [AGENTS.md](./AGENTS.md)。
