# 灵档项目性能优化与知识库升级 - 长任务计划

**分支**: `feature/performance-kb-upgrade`
**起点**: `feature/fullstack-ai-native` (2026-05-13)
**目标**: 解决后端同步阻塞、AI服务并发瓶颈、前端体验问题，并植入知识库扩展基础
**策略**: 按阶段独立交付，每阶段可编译运行，中断可恢复

---

## 一、改进总览（承接前次评审）

| 优先级 | 事项 | 所属阶段 | 预估耗时 |
|:---:|---|:---:|---|
| 🔴 P0 | AI调用异步化（Spring @Async + 前端轮询） | 阶段1 | 3-4h |
| 🔴 P0 | AI服务任务队列（Celery + Redis） | 阶段2 | 4-5h |
| 🟡 P1 | 统一异常分类 + 错误码体系 | 阶段3 | 2h |
| 🟡 P1 | 状态枚举替换魔法字符串 | 阶段3 | 1h |
| 🟡 P1 | 前端文档预览（pdf.js / mammoth.js） | 阶段4 | 3h |
| 🟢 P2 | Pinia 状态缓存 | 阶段4 | 2h |
| 🟢 P2 | LLM重试 + OCR结果缓存 | 阶段5 | 3h |
| 🔵 P3 | API版本号 | 阶段5 | 0.5h |
| 🔵 P3 | 知识库向量检索基础（pgvector） | 阶段6 | 5-6h |
| - | 全链路联调 + 测试文档 | 阶段7 | 3h |

---

## 二、阶段详细计划

### 阶段0：基础设施与文档准备（当前）
**目标**: 建立可追溯的工作基础
**产出**:
- [x] GitHub 分支 `feature/performance-kb-upgrade` 已创建
- [x] 本计划文档
- [ ] 阶段追踪记忆文档
- [ ] 各阶段检查点脚本（编译/启动验证）

**验收标准**: 能正常编译运行，分支干净

---

### 阶段1：后端异步化改造（核心！）
**目标**: 解决 Tomcat 线程被 AI 调用阻塞的问题

**具体任务**:
1. **启用 Spring 异步支持**
   - `@EnableAsync` + `AsyncConfig`（线程池配置）
   - 线程池参数: core=8, max=32, queue=100

2. **Service 层拆分**
   - `extractFields()` → `@Async`
   - `generateDocument()` → `@Async`
   - 新增 `FormTaskAsyncService` 专门处理异步 AI 调用

3. **任务状态机增强**
   - 新增状态枚举 `FormTaskStatusEnum`
   - 状态: PENDING_UPLOAD(0), EXTRACTING(1), AWAIT_CONFIRM(2), GENERATING(3), COMPLETED(4), FAILED(5), QUEUED(6), AI_PROCESSING(7)

4. **前端轮询对接**
   - 新增 `GET /lingdoc/form/progress/{taskId}` 接口
   - 前端 upload 成功后轮询进度

**文件清单**:
- `ruoyi-server/.../config/AsyncConfig.java` (新增)
- `ruoyi-server/.../enums/FormTaskStatusEnum.java` (新增)
- `ruoyi-server/.../service/lingdoc/impl/FormTaskAsyncService.java` (新增)
- `ruoyi-server/.../controller/lingdoc/LingdocFormController.java` (修改 upload/generate)
- `src/api/lingdoc/form.js` (新增 progress 接口)
- `src/views/lingdoc/form/index.vue` (修改 upload 后逻辑)

**验收标准**:
- [ ] 上传表格后接口立即返回（<500ms）
- [ ] 任务状态实时更新到前端
- [ ] AI 处理期间 Tomcat 线程不被占用
- [ ] 前后端能编译启动

---

### 阶段2：AI服务任务队列（核心！）
**目标**: AI 服务能扛并发，不 OOM

**具体任务**:
1. **Redis + Celery 集成**
   - `pip install celery[redis] redis`
   - `celeryconfig.py`: broker=redis://, backend=redis://
   - 并发限制: `celery worker --concurrency=2`

2. **任务定义**
   - `tasks.py`: `process_document_task()`, `fill_form_task()`
   - 任务 ID 映射到前端 task_id

3. **进度回调**
   - Celery task 更新 Redis 中的任务进度
   - Java 后端轮询 Redis 获取进度返回前端

4. **AI 服务改造**
   - `form.py` 的 `/form/fill` 改为提交 Celery 任务
   - 新增 `/form/status/{task_id}` 查询任务状态

**文件清单**:
- `lingdoc-ai-service/app/celery_app.py` (新增)
- `lingdoc-ai-service/app/tasks.py` (新增)
- `lingdoc-ai-service/app/routers/form.py` (修改)
- `lingdoc-ai-service/requirements.txt` (新增 celery, redis)
- `docker-compose.yml` (新增 redis 服务)

**验收标准**:
- [ ] 并发上传 3 个表格，AI 服务逐个处理不崩溃
- [ ] 任务进度可查询
- [ ] Celery worker 能正常启动

---

### 阶段3：异常处理 + 枚举规范化
**目标**: 安全 + 可维护

**具体任务**:
1. **统一异常体系**
   - `LingdocException` 基类
   - `BizException`（业务错误，返回 200 + 错误码）
   - `SysException`（系统错误，返回 500 + 通用提示）
   - 全局异常拦截器 `@ControllerAdvice`

2. **错误码枚举**
   - `LingdocErrorCode`: `FORM_UPLOAD_FAILED`, `AI_TIMEOUT`, `FILE_NOT_FOUND`

3. **魔法字符串替换**
   - Controller/Service/前端所有 `status = "1"` → `FormTaskStatusEnum.EXTRACTING.getCode()`
   - 前端 `statusText` computed 改用枚举映射

**文件清单**:
- `ruoyi-server/.../exception/LingdocException.java` (新增)
- `ruoyi-server/.../enums/LingdocErrorCode.java` (新增)
- `ruoyi-server/.../controller/GlobalExceptionHandler.java` (新增)
- 多处 Controller/Service 修改
- `src/views/lingdoc/form/index.vue` (修改状态映射)

**验收标准**:
- [ ] 后端抛异常前端收到统一格式错误码
- [ ] 没有魔法字符串残留（全局搜索 `"1"` `"2"` `"3"` 在 form 相关代码中）

---

### 阶段4：前端体验优化
**目标**: 文档预览 + 状态管理

**具体任务**:
1. **文档预览改造**
   - `npm install mammoth pdfjs-dist`
   - Word 预览: mammoth.js 转 HTML
   - PDF 预览: pdf.js
   - 替代 iframe 方案

2. **Pinia 状态管理**
   - `src/stores/formStore.js`: 缓存当前任务、字段列表
   - `src/stores/vaultStore.js`: 缓存 Vault 文件列表
   - 页面切换后状态保留

3. **进度条 + 步骤指示**
   - `el-steps` 组件: 上传 → 识别中 → 待确认 → 生成中 → 完成
   - AI 处理时显示具体步骤和耗时

**文件清单**:
- `src/stores/formStore.js` (新增)
- `src/stores/vaultStore.js` (新增)
- `src/components/DocPreview/index.vue` (新增)
- `src/views/lingdoc/form/index.vue` (修改预览区、步骤条)
- `package.json` (新增 mammoth, pdfjs-dist, pinia)

**验收标准**:
- [ ] Word/PDF 文件前端直接预览（无需下载插件）
- [ ] 刷新页面后任务状态不丢失
- [ ] AI 处理时有进度感和步骤指示

---

### 阶段5：AI服务稳定性
**目标**: LLM 调用稳定、OCR 结果可复用

**具体任务**:
1. **LLM 重试机制**
   - `pip install tenacity`
   - `LLMClient.call()` 加 `@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))`
   - 限流时返回特定错误码

2. **OCR 结果缓存**
   - Redis 缓存: key = `ocr:{file_md5}`, TTL = 24h
   - 相同文件重复处理时直接取缓存

3. **并发限制**
   - `asyncio.Semaphore(2)` 限制同时 OCR 数
   - Celery worker `--concurrency=2`

4. **API 版本号**
   - 路由前缀改为 `/api/v1/ai/`
   - 保留 `/api/ai/v1/` 做兼容（redirect 或 alias）

**文件清单**:
- `lingdoc-ai-service/app/services/llm_client.py` (修改，加重试)
- `lingdoc-ai-service/app/services/ocr_engine.py` (修改，加缓存)
- `lingdoc-ai-service/app/routers/` (路由前缀调整)

**验收标准**:
- [ ] 网络抖动时 LLM 自动重试成功
- [ ] 同一文件二次 OCR 毫秒级返回
- [ ] API 带版本号且旧路径兼容

---

### 阶段6：知识库模块基础
**目标**: 向量检索基础设施就绪

**具体任务**:
1. **向量数据库选型**: pgvector（PostgreSQL 扩展）
   - 理由: 团队已有 PostgreSQL 经验，无需额外运维
   - `CREATE EXTENSION vector;`

2. **数据库表设计**
   - `lingdoc_kb_document`: 文档块表（id, file_id, chunk_index, content, embedding, metadata）
   - `lingdoc_kb_index_task`: 索引任务表（id, file_id, status, progress, error_msg）

3. **Embedding 服务**
   - 新增 `/api/v1/ai/kb/embed` 接口
   - 接入 DashScope text-embedding 模型
   - 维度: 1536 (text-embedding-v3)

4. **文档分块策略**
   - 固定长度 512 tokens + overlap 128
   - 按段落优先（换行符分割，不足 512 的合并）

5. **检索接口**
   - `POST /api/v1/ai/kb/search` {query, top_k=5}
   - 向量相似度检索 + 元数据过滤

6. **Java 后端接口**
   - `LingdocKbController`: 知识库 CRUD、检索
   - 文件上传后异步触发索引任务

**文件清单**:
- `ruoyi-server/.../controller/lingdoc/LingdocKbController.java` (新增)
- `ruoyi-server/.../domain/lingdoc/LingdocKbDocument.java` (新增)
- `ruoyi-server/.../mapper/lingdoc/LingdocKbDocumentMapper.java` (新增)
- `lingdoc-ai-service/app/routers/kb.py` (新增)
- `lingdoc-ai-service/app/services/kb_service.py` (新增)
- `lingdoc-ai-service/app/services/chunker.py` (新增)
- `lingdoc-ai-service/requirements.txt` (新增 pgvector, tiktoken)

**验收标准**:
- [ ] 上传文档后能触发索引任务
- [ ] 向量检索返回相关文本块
- [ ] pgvector 在 SQLite fallback 方案中可降级

---

### 阶段7：全链路联调 + 测试文档
**目标**: 所有模块协同工作，文档交付

**具体任务**:
1. **端到端测试**
   - 上传表格 → AI识别 → 确认字段 → AI填表 → 下载结果
   - 知识库: 上传参考文档 → 索引 → 检索 → 填表引用

2. **性能基线**
   - 单用户: 上传→完成 < 30s
   - 并发 5 用户: 全部完成 < 60s
   - AI 服务内存 < 2GB

3. **文档交付**
   - `docs/UPGRADE-CHANGELOG.md`: 本次升级改动清单
   - `docs/DEPLOY.md`: 部署指南（含 Redis、Celery、pgvector）
   - `docs/API-CHANGELOG.md`: 接口变更说明

4. **代码提交**
   - 全部提交到 `feature/performance-kb-upgrade`
   - 推送 GitHub + Gitee

**验收标准**:
- [ ] 完整流程无报错跑通
- [ ] 性能达到基线
- [ ] 文档完整

---

## 三、检查点与兜底方案

### 每阶段检查清单
```bash
# 后端
mvn clean compile -pl ruoyi-server/ruoyi-admin -am

# 前端
npm run build

# AI 服务
python -m app.main --dry-run

# Celery
celery -A app.celery_app worker --loglevel=info

# Redis
redis-cli ping  # 应返回 PONG
```

### 中断恢复机制
1. 每完成一个阶段 → `git commit -m "阶段X: 描述"`
2. 每完成一个阶段 → compact 上下文
3. 记忆文档记录: 当前阶段、已改文件、待办项
4. 恢复时先 `git status` 确认工作区状态

### 风险预案
| 风险 | 影响 | 兜底 |
|---|---|---|
| Celery 装不上 | AI 队列失效 | 降级用 `BackgroundTasks` |
| pgvector 不支持 SQLite | 知识库无法本地测试 | SQLite 版跳过向量检索，仅做文本匹配 |
| mammoth.js 打包太大 | 前端体积增加 | 按需加载 (dynamic import) |
| 异步化引入并发 Bug | 状态竞争 | 加 `synchronized` 或乐观锁 |

---

## 四、阶段追踪

| 阶段 | 状态 | 完成时间 | 提交 hash |
|:---:|---|:---:|---|
| 0 准备 | 🟡 进行中 | - | - |
| 1 后端异步化 | ⬜ 未开始 | - | - |
| 2 AI队列 | ⬜ 未开始 | - | - |
| 3 异常+枚举 | ⬜ 未开始 | - | - |
| 4 前端优化 | ⬜ 未开始 | - | - |
| 5 AI稳定性 | ⬜ 未开始 | - | - |
| 6 知识库 | ⬜ 未开始 | - | - |
| 7 联调+文档 | ⬜ 未开始 | - | - |

---

*计划文档创建时间: 2026-05-13*
*负责人: claw gong*
