# AI 前端 API 接口规范

> 本文档描述前端 `src/api/ai/` 模块的设计与使用方式，以及 `src/utils/request.js` 为支持 AI 场景所做的扩展。

---

## 1. 概述

为支持后端 AI 模块（对话、知识库、检索、模型管理）的接口调用，前端新建了 `src/api/ai/` 目录，按业务子域划分为 4 个 API 文件，并统一从 `index.js` 集中导出。

```
src/api/ai/
├── chat.js          # AI 对话：会话管理、消息历史、流式/非流式对话
├── knowledge.js     # 知识库：知识库 CRUD、文档上传、解析状态、索引重建
├── search.js        # 检索：向量检索、全文检索、混合检索
├── model.js         # 模型：模型列表、配置读取、连通性测试
└── index.js         # 集中导出
```

---

## 2. request.js 扩展

文件位置：`src/utils/request.js`

### 2.1 超时时间支持按请求覆盖

**变更前**：`axios.create()` 中硬编码 `timeout: 10000`，所有请求固定 10 秒。

**变更后**：`axios.create()` 不再设置全局 `timeout`，改在请求拦截器中读取 `config.timeout`，未配置时回退到 10 秒。

```javascript
service.interceptors.request.use(config => {
  // 超时时间支持按请求覆盖，默认10s
  config.timeout = config.timeout || 10000
  // ...
})
```

**影响**：现有 `src/api/` 下所有文件无需修改，行为完全一致；新增 AI 接口可独立传 `timeout: 120000` 覆盖默认值。

### 2.2 SSE 流式请求函数 `streamRequest`

AI 对话需要「打字机效果」的流式输出。Axios 在浏览器端对 `text/event-stream` 的流式消费支持不佳，因此新增基于浏览器原生 `fetch` + `ReadableStream` 的封装函数。

```javascript
export function streamRequest(url, data, onMessage, onError, onDone)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `url` | `string` | 请求路径（不含 baseURL，自动拼接 `VITE_APP_BASE_API`） |
| `data` | `object` | POST 请求体（JSON） |
| `onMessage` | `(data: string) => void` | 收到每条 SSE 数据时的回调 |
| `onError` | `(error: Error) => void` | 发生错误时的回调 |
| `onDone` | `() => void` | 流结束时的回调 |

**特性**：
- 自动携带 `Bearer Token`（从 Cookie 读取）
- 自动设置 `Accept: text/event-stream`
- 解析 SSE 格式 `data: ...` 行，过滤 `[DONE]` 结束标记
- 返回 `Promise<void>`，便于 `await` 等待流结束

---

## 3. API 模块清单

### 3.1 `src/api/ai/chat.js` —— AI 对话

| 函数 | 方法 | URL | 超时 | 说明 |
|------|------|-----|------|------|
| `listSession(query)` | GET | `/lingdoc/ai/chat/session/list` | 10s | 查询会话列表 |
| `getSession(sessionId)` | GET | `/lingdoc/ai/chat/session/{sessionId}` | 10s | 获取会话详情 |
| `addSession(data)` | POST | `/lingdoc/ai/chat/session` | 10s | 新建会话 |
| `updateSession(data)` | PUT | `/lingdoc/ai/chat/session` | 10s | 更新会话（重命名/置顶） |
| `delSession(sessionId)` | DELETE | `/lingdoc/ai/chat/session/{sessionId}` | 10s | 删除会话 |
| `listMessage(sessionId, query)` | GET | `/lingdoc/ai/chat/message/list/{sessionId}` | 10s | 获取消息历史 |
| `chat(data)` | POST | `/lingdoc/ai/chat/send` | 120s | 非流式对话（等待完整响应） |
| `streamChat(data, onMessage, onError, onDone)` | POST | `/lingdoc/ai/chat/stream` | — | 流式对话（SSE） |
| `stopChat(sessionId)` | POST | `/lingdoc/ai/chat/stop/{sessionId}` | 10s | 终止生成 |

### 3.2 `src/api/ai/knowledge.js` —— 知识库

| 函数 | 方法 | URL | 超时 | 说明 |
|------|------|-----|------|------|
| `listKb(query)` | GET | `/lingdoc/ai/knowledge/list` | 10s | 知识库列表 |
| `getKb(kbId)` | GET | `/lingdoc/ai/knowledge/{kbId}` | 10s | 知识库详情 |
| `addKb(data)` | POST | `/lingdoc/ai/knowledge` | 10s | 新增知识库 |
| `updateKb(data)` | PUT | `/lingdoc/ai/knowledge` | 10s | 修改知识库 |
| `delKb(kbId)` | DELETE | `/lingdoc/ai/knowledge/{kbId}` | 10s | 删除知识库 |
| `listAccessibleKb()` | GET | `/lingdoc/ai/knowledge/accessible` | 10s | 用户可访问知识库（下拉） |
| `listDoc(kbId, query)` | GET | `/lingdoc/ai/knowledge/doc/list` | 10s | 文档列表 |
| `uploadDoc(kbId, data)` | POST | `/lingdoc/ai/knowledge/doc/upload` | 60s | 文档上传（multipart） |
| `delDoc(docId)` | DELETE | `/lingdoc/ai/knowledge/doc/{docId}` | 10s | 删除文档 |
| `getDocStatus(docId)` | GET | `/lingdoc/ai/knowledge/doc/status/{docId}` | 10s | 解析状态查询（轮询） |
| `reindexKb(kbId)` | POST | `/lingdoc/ai/knowledge/reindex/{kbId}` | 60s | 重建索引 |

### 3.3 `src/api/ai/search.js` —— 检索

| 函数 | 方法 | URL | 超时 | 说明 |
|------|------|-----|------|------|
| `vectorSearch(data)` | POST | `/lingdoc/ai/search/vector` | 30s | 向量检索 |
| `fullTextSearch(data)` | POST | `/lingdoc/ai/search/fulltext` | 30s | 全文检索 |
| `hybridSearch(data)` | POST | `/lingdoc/ai/search/hybrid` | 30s | 混合检索 |

### 3.4 `src/api/ai/model.js` —— 模型

| 函数 | 方法 | URL | 超时 | 说明 |
|------|------|-----|------|------|
| `listModel()` | GET | `/lingdoc/ai/model/list` | 10s | 可用模型列表 |
| `getModelConfig(modelName)` | GET | `/lingdoc/ai/model/config/{modelName}` | 10s | 模型配置详情 |
| `testModelConnection(data)` | POST | `/lingdoc/ai/model/test` | 15s | 连通性测试 |

---

## 4. 使用示例

### 4.1 标准非流式请求

```javascript
import { chat, listSession } from '@/api/ai/chat'

// 列表查询（默认 10s 超时）
const res = await listSession({ pageNum: 1, pageSize: 10 })

// 非流式对话（120s 超时，等待完整响应）
const response = await chat({
  sessionId: 'sess_xxx',
  message: '你好，请介绍一下自己'
})
console.log(response.data.content)
```

### 4.2 流式请求（打字机效果）

```javascript
import { streamChat } from '@/api/ai/chat'

let fullContent = ''

await streamChat(
  { sessionId: 'sess_xxx', message: '你好' },
  (chunk) => {
    // 逐条收到 AI 输出的片段
    fullContent += chunk
    console.log('当前内容:', fullContent)
  },
  (err) => {
    console.error('流式出错:', err)
  },
  () => {
    console.log('流结束')
  }
)
```

### 4.3 集中导入

```javascript
// 方式一：按需导入（推荐）
import { chat, streamChat } from '@/api/ai/chat'
import { listKb, uploadDoc } from '@/api/ai/knowledge'

// 方式二：批量导入
import * as aiApi from '@/api/ai'
await aiApi.chat(data)
await aiApi.streamChat(data, onMessage, onError, onDone)
```

---

## 5. URL 路径约定

AI 模块接口统一采用三级路径：

```
/lingdoc/ai/{子模块}/{操作}
```

| 子模块 | 职责 | 示例 |
|--------|------|------|
| `chat` | 对话与会话 | `/lingdoc/ai/chat/send`、`/lingdoc/ai/chat/stream` |
| `knowledge` | 知识库与文档 | `/lingdoc/ai/knowledge/list`、`/lingdoc/ai/knowledge/doc/upload` |
| `search` | 检索 | `/lingdoc/ai/search/vector`、`/lingdoc/ai/search/hybrid` |
| `model` | 模型管理 | `/lingdoc/ai/model/list`、`/lingdoc/ai/model/test` |

> 注：此约定与 `docs/spec/09-AI模块架构设计.md` 中定义的 URL 规范保持一致。

---

## 6. 超时配置参考表

| 场景 | 推荐超时 | 原因 |
|------|----------|------|
| 普通 CRUD | 10s（默认） | 数据库查询，响应快 |
| 模型连通性测试 | 15s | 需等待模型服务端响应 |
| 检索（向量/全文/混合） | 30s | 可能涉及复杂计算 |
| 文档上传 | 60s | 大文件上传 + 初步处理 |
| 索引重建 | 60s | 批量 Embedding 计算 |
| 非流式对话 | 120s | 等待 LLM 完整生成 |
| 流式对话 | — | 使用 `streamRequest`，无超时概念，依赖连接保持 |

---

## 附录：与现有文档的关系

| 本文档 | 对应文档 | 关系 |
|--------|----------|------|
| 本规范 | `docs/spec/10-AI模块开发规范.md` | 互补：本文档聚焦前端 API 封装实际落地细节；现有文档是前后端通用编码规范 |
