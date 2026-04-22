# LingDoc AI Service - 本地部署指南

> **本文档目标**：帮助 AI 工程师和主项目工程师在本地环境快速部署 `lingdoc-ai-service`，并解决常见部署问题。
>
> **适用场景**：本地开发调试、单机演示、Docker 测试
>
> **预计耗时**：10-20 分钟（含依赖下载）

---

## 一、环境准备

### 1.1 系统要求

| 项目 | 要求 | 说明 |
|------|------|------|
| **操作系统** | Linux / macOS / Windows WSL | 推荐 Ubuntu 22.04+ |
| **Python** | 3.10 ~ 3.12 | **必须**，3.13 暂不兼容 PaddleOCR |
| **内存** | ≥ 4GB | PaddleOCR + 模型加载需要 |
| **磁盘** | ≥ 3GB 空闲 | venv + 模型文件约 1.5GB |
| **网络** | 可访问阿里云 | DashScope API 需要外网 |

### 1.2 验证 Python 版本

```bash
python3 --version
# 应输出: Python 3.10.x / 3.11.x / 3.12.x

# 如果版本不对，建议使用 pyenv 安装
pyenv install 3.12.3
pyenv local 3.12.3
```

**⚠️ 常见问题**：Python 3.13 安装 paddlepaddle 会失败，请降级到 3.12。

---

## 二、获取代码

### 2.1 克隆仓库

```bash
git clone https://github.com/Lees-42/lingdoc_trial.git
cd lingdoc_trial/lingdoc-ai-service
```

### 2.2 创建虚拟环境（强烈推荐）

```bash
# 创建虚拟环境
python3 -m venv venv

# 激活（Linux/macOS）
source venv/bin/activate

# 激活（Windows CMD）
venv\Scripts\activate.bat

# 激活（Windows PowerShell）
venv\Scripts\Activate.ps1
```

**为什么用虚拟环境？**
- 隔离项目依赖，避免与系统 Python 冲突
- PaddleOCR 依赖 numpy<2.0，可能与系统其他项目冲突
- 方便删除重装：直接删 `venv/` 文件夹即可

---

## 三、安装依赖

### 3.1 基础依赖（FastAPI + HTTP 客户端）

```bash
pip install -r requirements.txt
```

requirements.txt 内容：
```
fastapi>=0.104.0
uvicorn>=0.24.0
pydantic>=2.5.0
httpx>=0.25.0
python-dotenv>=1.0.0
pytest>=7.4.0
pytest-asyncio>=0.21.0
```

### 3.2 OCR 引擎（PaddleOCR + PaddlePaddle）

```bash
pip install paddleocr paddlepaddle
```

**⚠️ 常见问题 1：版本不匹配**

错误示例：
```
ERROR: Could not find a version that satisfies the requirement paddlepaddle==2.6.1
```

**解决方案**：不指定版本，让 pip 自动匹配：
```bash
pip install paddleocr paddlepaddle
# 当前测试通过版本：paddleocr==2.8.1, paddlepaddle==3.3.1
```

**⚠️ 常见问题 2：numpy 版本冲突**

错误示例：
```
RuntimeError: module compiled against API version 0x10 but this version of numpy is 0xf
```

**解决方案**：
```bash
pip install numpy==1.26.4
pip install paddleocr paddlepaddle
```

**⚠️ 常见问题 3：Linux 安装 PaddlePaddle GPU 版失败**

如果你只有 CPU，不要装 GPU 版：
```bash
# CPU 版本（推荐，无需 CUDA）
pip install paddlepaddle

# GPU 版本（需要 CUDA 11.x + cuDNN）
pip install paddlepaddle-gpu
```

### 3.3 验证安装

```bash
python -c "import fastapi; print(f'FastAPI: {fastapi.__version__}')"
python -c "import paddle; print(f'PaddlePaddle: {paddle.__version__}')"
python -c "from paddleocr import PaddleOCR; print('PaddleOCR: OK')"
```

全部输出正常即可继续。

---

## 四、配置环境变量

### 4.1 创建 .env 文件

```bash
# 复制模板
cp .env.example .env

# 编辑文件
nano .env  # 或 vim / VS Code
```

### 4.2 必填配置

```bash
# ==========================================
# 必填项（不填服务无法启动）
# ==========================================

# 阿里云 DashScope API Key（Qwen3-Max 调用）
DASHSCOPE_API_KEY=sk-your-key-here

# AI 服务与主后端之间的共享密钥（认证用）
AI_INTERNAL_TOKEN=lingdoc-ai-2026-change-me

# ==========================================
# 可选项（有默认值，一般不用改）
# ==========================================

# 服务监听端口
AI_SERVICE_PORT=8000

# 服务监听地址（0.0.0.0 允许外部访问，127.0.0.1 仅限本地）
AI_SERVICE_HOST=0.0.0.0

# 默认 LLM 模型
AI_DEFAULT_MODEL=qwen3-max

# LLM 请求超时（秒）
AI_LLM_TIMEOUT=60

# 日志级别（DEBUG / INFO / WARNING / ERROR）
AI_LOG_LEVEL=INFO

# ==========================================
# 开发调试用（生产环境保持默认）
# ==========================================

# PDF 渲染 DPI（越高越清晰，越慢）
AI_PDF_DPI=150

# OCR 置信度阈值（0.0 ~ 1.0，低于此值的识别结果丢弃）
AI_OCR_THRESHOLD=0.5

# OCR 语言（ch=中文+英文，en=纯英文）
AI_OCR_LANGUAGE=ch
```

### 4.3 获取阿里云 API Key

1. 访问 [阿里云 DashScope 控制台](https://dashscope.aliyun.com/)
2. 登录阿里云账号
3. 进入「API-KEY 管理」
4. 创建新的 API Key
5. 复制 Key 填入 `.env` 的 `DASHSCOPE_API_KEY`

**⚠️ 安全提示**：
- `.env` 文件已加入 `.gitignore`，不会上传到 GitHub
- 不要把 API Key 截图发到公开群聊
- 如果 Key 泄露，立即在阿里云控制台删除并重新创建

---

## 五、启动服务

### 5.1 开发模式（热重载）

```bash
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

特点：
- 代码修改后自动重启（适合开发调试）
- 日志输出到控制台
- 性能稍低（可接受）

### 5.2 生产模式

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 2
```

特点：
- 多进程并行处理
- 无热重载
- 性能更高

### 5.3 首次启动日志

正常启动应显示：
```
============================================================
LingDoc AI Service 启动中...
============================================================
✅ 配置校验通过
日志级别: INFO
🚀 服务启动完成 | 监听: 0.0.0.0:8000
📖 API 文档: http://0.0.0.0:8000/docs
```

如果看到错误：
```
❌ 配置校验失败：必填配置缺失
  - DASHSCOPE_API_KEY
  - AI_INTERNAL_TOKEN
```
→ 检查 `.env` 文件是否正确配置。

---

## 六、验证部署

### 6.1 浏览器访问 API 文档

打开浏览器访问：
```
http://localhost:8000/docs
```

应看到 Swagger UI 界面，包含：
- `POST /api/ai/v1/doc/process`（文档处理）
- `POST /api/ai/v1/doc/qa`（文档问答）
- `GET /api/ai/v1/health`（健康检查）

### 6.2 命令行快速测试

```bash
# 1. 健康检查（无需认证）
curl http://localhost:8000/api/ai/v1/health

# 应返回：
# {"code":200,"msg":"服务运行中","data":{"status":"ok","ocr_ready":true,"llm_ready":true}}

# 2. 测试认证（应返回 401）
curl -X POST http://localhost:8000/api/ai/v1/doc/qa \
  -H "Content-Type: application/json" \
  -d '{"question":"测试","file_path":"test.txt"}'

# 应返回：401 Unauthorized

# 3. 正确调用（携带 Token）
curl -X POST http://localhost:8000/api/ai/v1/doc/qa \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: lingdoc-ai-2026-change-me" \
  -d '{"question":"测试","file_path":"test.txt"}'
```

### 6.3 上传文件测试

```bash
# 准备测试文件
echo "这是一份电路实验报告，验证了基尔霍夫电压定律。" > test.txt

# 调用处理接口
curl -X POST http://localhost:8000/api/ai/v1/doc/process \
  -H "X-Internal-Token: lingdoc-ai-2026-change-me" \
  -F "file=@test.txt" \
  -F "user_id=1"
```

---

## 七、常见问题排查

### 7.1 启动失败："ModuleNotFoundError: No module named 'paddle'"

**原因**：PaddlePaddle 未安装或安装失败

**解决**：
```bash
pip install paddlepaddle
# 如果失败，尝试指定源
pip install paddlepaddle -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 7.2 启动失败："Permission denied"（端口 8000）

**原因**：端口被占用（常见：另一个 uvicorn 实例、或其他程序）

**解决**：
```bash
# 查找占用端口的进程
lsof -i :8000

# 杀掉进程
kill -9 <PID>

# 或改用其他端口
python -m uvicorn app.main:app --port 8001
```

### 7.3 OCR 识别中文乱码

**原因**：系统缺少中文字体

**解决**：
```bash
# Ubuntu/Debian
sudo apt-get install fonts-noto-cjk

# CentOS
sudo yum install google-noto-sans-cjk-fonts

# 重新启动服务
```

### 7.4 AI 分析返回空内容

**原因**：DashScope API Key 无效或额度用完

**排查**：
```bash
# 1. 检查 Key 是否有效
curl -X POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation \
  -H "Authorization: Bearer sk-your-key" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen3-max","input":{"messages":[{"role":"user","content":"你好"}]}}'

# 2. 检查阿里云控制台额度
# 访问 https://dashscope.aliyun.com/ 查看剩余额度
```

### 7.5 Docker 部署问题

如果使用 Docker：

```bash
# 构建镜像
docker build -t lingdoc-ai-service .

# 运行容器（映射端口，传入环境变量）
docker run -d \
  -p 8000:8000 \
  -e DASHSCOPE_API_KEY=sk-your-key \
  -e AI_INTERNAL_TOKEN=your-token \
  --name lingdoc-ai \
  lingdoc-ai-service
```

**常见问题**：容器内 OCR 中文乱码 → 在 Dockerfile 中安装字体：
```dockerfile
RUN apt-get update && apt-get install -y fonts-noto-cjk
```

### 7.6 虚拟环境激活失败

**PowerShell 错误**：
```
venv\Scripts\Activate.ps1 cannot be loaded because running scripts is disabled
```

**解决**：
```powershell
# 以管理员身份运行 PowerShell，执行
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## 八、目录结构说明

```
lingdoc-ai-service/
├── app/
│   ├── main.py              # FastAPI 入口（不要直接改，除非你知道在做什么）
│   ├── config.py            # 配置读取（.env 文件定义在这里读取）
│   ├── routers/
│   │   └── doc.py           # API 路由（对外接口定义）
│   ├── services/
│   │   ├── ocr_engine.py    # PaddleOCR 封装（OCR 核心）
│   │   ├── llm_client.py    # DashScope 调用（AI 分析核心）
│   │   └── tag_generator.py # 标签生成（规则 + LLM）
│   ├── middleware/
│   │   └── auth.py          # Token 认证（X-Internal-Token 校验）
│   ├── models/
│   │   └── schemas.py       # 数据模型（请求/响应格式）
│   └── utils/
│       └── logger.py        # 日志工具
├── Dockerfile               # 容器构建（需要 Docker 时用）
├── docker-compose.yml       # 编排配置（多服务部署时用）
├── requirements.txt         # Python 依赖清单
├── .env                     # 环境变量（你自己创建的，不上传 Git）
├── .env.example             # 环境变量模板（参考用）
└── README.md                # 项目总览
```

---

## 九、与主后端对接

### 9.1 主后端需要做什么

| 步骤 | 操作 | 示例代码 |
|------|------|----------|
| 1 | 保存用户上传的文件 | Java `FileUpload` |
| 2 | 构造 HTTP 请求 | `POST http://localhost:8000/api/ai/v1/doc/process` |
| 3 | 添加认证头 | `X-Internal-Token: lingdoc-ai-2026-change-me` |
| 4 | 上传文件 | `multipart/form-data` |
| 5 | 接收响应 | 解析 JSON |
| 6 | 保存结果 | 写入数据库 |

### 9.2 最小 Java 调用示例

```java
// 使用 RestTemplate 或 HttpClient
HttpHeaders headers = new HttpHeaders();
headers.set("X-Internal-Token", "lingdoc-ai-2026-change-me");
headers.setContentType(MediaType.MULTIPART_FORM_DATA);

MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new FileSystemResource("/path/to/file.pdf"));
body.add("user_id", userId);

HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

ResponseEntity<String> response = restTemplate.postForEntity(
    "http://localhost:8000/api/ai/v1/doc/process",
    request,
    String.class
);

// 解析 JSON 结果
JSONObject result = JSON.parseObject(response.getBody());
```

### 9.3 配置检查清单

部署前确认：
- [ ] Python 3.10 ~ 3.12
- [ ] 虚拟环境已激活（`which python` 显示 venv 路径）
- [ ] `pip list` 包含 paddleocr、fastapi、uvicorn
- [ ] `.env` 文件存在且 `DASHSCOPE_API_KEY` 已填写
- [ ] `.env` 文件存在且 `AI_INTERNAL_TOKEN` 已填写
- [ ] 端口 8000 未被占用
- [ ] 防火墙允许 8000 端口（如需外部访问）
- [ ] 阿里云 DashScope 有可用额度

---

## 十、获取帮助

如果本文档未能解决你的问题：

1. **查看日志**：运行目录下的 `lingdoc-ai.log`（如有配置）或控制台输出
2. **检查版本**：`python --version`、`pip list | grep paddle`
3. **测试最小环境**：先不装 PaddleOCR，只启动 FastAPI 框架（`ocr_ready=false` 但服务可启动）
4. **联系维护**：在 GitHub Issues 提问，附上完整错误日志

---

*文档版本: 1.0*  
*更新日期: 2026-04-22*  
*适用代码版本: main 分支 c0bd570 及之后*
