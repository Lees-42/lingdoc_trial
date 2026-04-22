# LingDoc AI Service

灵档 AI 服务 - 文档智能处理后端 API

## 功能

- **OCR 文档识别**：PDF / Word / 图片 → 文本提取
- **AI 内容分析**：自动摘要、关键词提取、分类
- **智能标签生成**：基于规则 + LLM 双层标签体系
- **统一认证**：`X-Internal-Token` 共享密钥
- **文档问答**（预留）：基于 RAG 的问答系统
- **表格填写**（预留）：自动填写表格模板

## 技术栈

| 组件 | 用途 |
|---|---|
| FastAPI | Web 框架 |
| PaddleOCR | OCR 引擎（百度开源） |
| DashScope | 阿里云大模型 SDK（Qwen3-Max） |
| PyMuPDF | PDF 处理 |
| python-docx | Word 处理 |

## 快速开始

### 1. 安装依赖

```bash
cd lingdoc-ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入你的 DASHSCOPE_API_KEY 和 AI_INTERNAL_TOKEN
```

### 3. 启动服务

```bash
# 开发模式（自动重载）
uvicorn app.main:app --reload --port 8000

# 生产模式（多进程）
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 4. 查看文档

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API 接口

### 认证

所有 `/api/ai/v1/*` 接口需要在 Header 中携带：

```
X-Internal-Token: {AI_INTERNAL_TOKEN}
```

### 核心接口

#### POST /api/ai/v1/doc/process

文档处理（OCR + AI分析 + 标签生成）

**请求体**：
```json
{
  "task_id": "t_xxx",
  "file_path": "/uploads/lingdoc/1/1745376123_report.pdf",
  "user_id": 1,
  "vault_id": "vault_default",
  "options": {
    "enable_ai_analysis": true,
    "enable_tagging": true,
    "model": "qwen3-max"
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "msg": "处理成功",
  "data": {
    "file_id": "vault_default_abc123",
    "file_name": "report.pdf",
    "file_type": "pdf",
    "ocr_text": "提取的完整文本...",
    "ai_summary": "这是一份电路实验报告...",
    "ai_keywords": ["电路", "实验报告"],
    "auto_tags": [
      {"name": "电路", "color": "#409EFF", "confidence": 0.95, "source": "ai_auto"}
    ],
    "trace": {
      "request_id": "req_xxx",
      "token_usage": 1250,
      "process_time_ms": 3200
    }
  }
}
```

#### GET /api/ai/v1/health

健康检查（无需认证）

**响应**：
```json
{
  "code": 200,
  "msg": "服务运行中",
  "data": {
    "status": "ok",
    "version": "1.0.0",
    "ocr_ready": true,
    "llm_ready": true
  }
}
```

## 项目结构

```
lingdoc-ai-service/
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置管理
│   ├── middleware/
│   │   └── auth.py          # X-Internal-Token 认证
│   ├── routers/
│   │   └── doc.py           # API 路由（文档处理）
│   ├── services/
│   │   ├── ocr_engine.py    # PaddleOCR 封装
│   │   ├── llm_client.py    # Qwen3-Max 调用
│   │   └── tag_generator.py # 标签生成
│   ├── models/
│   │   └── schemas.py       # Pydantic 数据模型
│   └── utils/
│       └── logger.py        # 日志工具
├── tests/                   # 测试文件
├── requirements.txt         # Python 依赖
├── .env.example            # 环境变量模板
└── README.md               # 本文档
```

## 与主后端对接

### 部署架构

```
用户上传文件
    ↓
主后端（Spring Boot）保存文件到共享目录
    ↓
主后端调用 AI 服务（HTTP + X-Internal-Token）
    ↓
AI 服务处理文件，返回元数据
    ↓
主后端保存元数据到数据库
```

### 共享目录

主后端和 AI 服务必须挂载同一个目录（Docker Volume）：

```yaml
# docker-compose.yml
volumes:
  - /data/lingdoc/uploads:/uploads/lingdoc
```

### 主后端 Java 调用示例

```java
HttpHeaders headers = new HttpHeaders();
headers.set("X-Internal-Token", aiServiceToken);

DocProcessRequest request = new DocProcessRequest();
request.setTaskId("t_xxx");
request.setFilePath("/uploads/lingdoc/1/report.pdf");

HttpEntity<DocProcessRequest> entity = new HttpEntity<>(request, headers);
ResponseEntity<DocProcessResponse> response = restTemplate.exchange(
    aiServiceUrl + "/api/ai/v1/doc/process",
    HttpMethod.POST,
    entity,
    DocProcessResponse.class
);
```

## 性能参考

| 文档类型 | 页数 | 预估耗时 |
|---|---|---|
| PDF（OCR） | 1 页 | 2-5 秒 |
| PDF（OCR） | 10 页 | 10-20 秒 |
| Word（直接提取） | - | 1-3 秒 |
| 图片 | 1 张 | 2-5 秒 |

## 安全

- AI 服务不直接面向外网用户，只接收来自主后端的内部请求
- 通过 `X-Internal-Token` 共享密钥认证
- 建议内网部署，不暴露 8000 端口到外网
- 生产环境 Token 应定期轮换

## 开发计划

- [x] OCR 文档识别
- [x] AI 内容分析（摘要/关键词/分类）
- [x] 智能标签生成
- [ ] 文档问答（RAG）
- [ ] 表格自动填写
- [ ] 向量数据库集成（Milvus / Pinecone）

## License

MIT
