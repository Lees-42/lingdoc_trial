# 前端 HTTP 请求与响应拦截

本文档解读 `src/utils/request.js` 的运行逻辑，这是前端所有 HTTP 通信的核心，基于 Axios 封装，包含请求拦截、响应拦截、下载和 SSE 流式请求。

---

## 1. 核心文件

```
src/utils/request.js
```

---

## 2. Axios 实例创建

```javascript
axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API  // '/dev-api'
})
```

- **baseURL**：从环境变量读取，开发模式下为 `/dev-api`
- 所有通过 `service` 发送的请求都会自动拼接此前缀

---

## 3. 请求拦截器（Request Interceptor）

在请求发出前执行，按以下顺序处理：

```javascript
service.interceptors.request.use(config => {
    ↓
  设置超时时间（默认 10s，可按请求覆盖）
    ↓
  读取 isToken 标记 → 决定是否跳过 Token
    ↓
  读取 repeatSubmit 标记 → 决定是否跳过防重复提交
    ↓
  如果存在 Token 且需要 Token →
      config.headers['Authorization'] = 'Bearer ' + getToken()
    ↓
  如果是 GET 请求且有 params →
      将 params 序列化为 URL 查询字符串
    ↓
  如果是 POST/PUT 且需要防重 →
      检查 sessionStorage 中最近一次的请求（URL + 数据 + 时间）
      如果 1000ms 内重复 → Promise.reject('数据正在处理，请勿重复提交')
      否则 → 记录本次请求到 sessionStorage
    ↓
  return config
})
```

### 3.1 Token 附加规则

```javascript
if (getToken() && !isToken) {
  config.headers['Authorization'] = 'Bearer ' + getToken()
}
```

- Token 存储在 Cookie 中（`src/utils/auth.js` 管理）
- 每个请求默认携带 `Authorization: Bearer <token>`
- 特殊请求可通过设置 `headers: { isToken: false }` 跳过

### 3.2 防重复提交机制

```javascript
const requestObj = {
  url: config.url,
  data: JSON.stringify(config.data),
  time: new Date().getTime()
}

// 间隔时间默认 1000ms
if (s_data === requestObj.data && 
    requestObj.time - s_time < interval && 
    s_url === requestObj.url) {
  return Promise.reject(new Error('数据正在处理，请勿重复提交'))
}
```

- 使用 `sessionStorage` 存储最近请求
- 通过比对 URL、请求体和发起时间判断是否重复
- 超过 5MB 的请求跳过防重校验（避免占用过多存储）

---

## 4. 响应拦截器（Response Interceptor）

在收到后端响应后执行：

```javascript
service.interceptors.response.use(
  res => { /* 成功响应处理 */ },
  error => { /* 错误响应处理 */ }
)
```

### 4.1 成功响应处理流程

```javascript
const code = res.data.code || 200
const msg = errorCode[code] || res.data.msg || errorCode['default']

if (code === 401) → 登录过期弹窗 → 确认后退出登录
if (code === 500) → ElMessage.error(msg) → reject
if (code === 601) → ElMessage.warning(msg) → reject
if (code !== 200) → ElNotification.error(title: msg) → reject
if (code === 200) → Promise.resolve(res.data)
```

### 4.2 错误响应处理流程

```javascript
error => {
    ↓
  判断 message 类型：
    - "Network Error" → "后端接口连接异常"
    - 包含 "timeout" → "系统接口请求超时"
    - 包含 "Request failed with status code" → "系统接口 xxx 异常"
    ↓
  ElMessage.error(message)
    ↓
  Promise.reject(error)
}
```

---

## 5. 下载方法

```javascript
export function download(url, params, filename, config)
```

- 显示全屏 loading（"正在下载数据，请稍候"）
- 发送 POST 请求，responseType 设为 `'blob'`
- 使用 `file-saver` 库触发浏览器下载
- 如果返回的是 JSON（非 Blob），解析并显示错误信息

---

## 6. SSE 流式请求（streamRequest）

```javascript
export function streamRequest(url, data, onMessage, onError, onDone)
```

用于 AI 对话等流式输出场景，**不使用 Axios**，而是使用浏览器原生 `fetch` + `ReadableStream`：

```
fetch(fullUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify(data)
})
    ↓
response.body.getReader() 获取流读取器
    ↓
循环读取 chunks：
    - 使用 TextDecoder 解码 UTF-8
    - 按行分割，解析 data: 前缀
    - 遇到 [DONE] 或 done=true 时结束
    ↓
逐条调用 onMessage(data) 回调
```

### 与常规请求的区别

| 特性 | Axios 请求 | streamRequest |
|------|-----------|---------------|
| 底层 API | Axios | 原生 fetch |
| 响应类型 | 完整 JSON | ReadableStream 逐块读取 |
| 适用场景 | CRUD 接口 | AI 流式输出、Server-Sent Events |
| 拦截器 | 经过 request/response 拦截器 | 独立实现，手动附加 Token |

---

## 7. 请求生命周期总结

```
业务代码调用 API 函数（如 getInfo()）
    ↓
Axios 发送请求
    ↓
【请求拦截器】
    - 附加 Token
    - GET 参数序列化
    - 防重复提交校验
    ↓
Vite 代理 /dev-api → localhost:8080
    ↓
后端接收请求
    ↓
后端返回响应
    ↓
【响应拦截器】
    - 解析 code
    - 401 → 登录过期处理
    - 500 → 错误提示
    - 200 → 返回 res.data
    ↓
业务代码拿到数据，更新 UI
```
