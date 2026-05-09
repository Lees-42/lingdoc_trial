# LingDoc AI Service - 本地运行指南

> 给 Kimi Claw Desktop 的执行清单。按顺序跑，不要跳步。

---

## 1. 环境前提

| 要求 | 版本 | 检查命令 |
|---|---|---|
| Python | 3.10+ | `python3 --version` |
| pip | 最新 | `pip3 --version` |
| Git | 任意 | `git --version` |
| 网络 | 能访问 DashScope API | `curl https://dashscope.aliyuncs.com` |

**如果 Python 版本不够**，先装 pyenv 或直接从 python.org 下载。

---

## 2. 拉取代码

```bash
# 进入用户的工作目录（比如 ~/workspace 或 ~/Projects）
cd ~/workspace  # 或用户指定的目录

# 克隆仓库
git clone https://github.com/Lees-42/lingdoc_trial.git

cd lingdoc_trial

# 切换到 feature/ai-native 分支
git checkout feature/ai-native

# 确认当前分支
git branch --show-current
# 应该输出: feature/ai-native
```

**注意：代码直接在仓库根目录，没有 `lingdoc-ai-service/` 子目录。**

---

## 3. 创建虚拟环境 + 安装依赖

```bash
# 确保在仓库根目录（lingdoc_trial/）
cd ~/workspace/lingdoc_trial  # 根据实际情况调整路径

# 创建虚拟环境
python3 -m venv venv

# 激活（Linux/Mac）
source venv/bin/activate

# 激活（Windows PowerShell）
# venv\Scripts\Activate.ps1

# 安装依赖
pip install -r requirements.txt
```

**requirements.txt 包含：**
- fastapi, uvicorn
- python-docx, openpyxl
- httpx（LLM 调用）
- paddleocr, paddlepaddle（OCR）
- dashscope（阿里灵积 API）

**安装时间较长**（PaddleOCR 模型约 100MB），耐心等待。

---

## 4. 配置环境变量

```bash
# 复制示例配置
cp .env.example .env

# 编辑 .env，填入 DashScope API Key
nano .env  # 或 vim / 任意编辑器
```

**.env 必填项：**
```env
DASHSCOPE_API_KEY=sk-xxxxxxxxx   # 用户的 DashScope API Key
AI_INTERNAL_TOKEN=lingdoc-ai-2026-a7f3e9d2b8c1e4f5  # 内部认证令牌，不改也行
AI_SERVICE_PORT=8000
AI_SERVICE_HOST=0.0.0.0
AI_DEFAULT_MODEL=qwen3-max        # 或 qwen3-coder
```

**获取 DashScope API Key：**
1. 访问 https://dashscope.console.aliyun.com/
2. 登录 → API Key 管理 → 创建新 Key
3. 复制 `sk-` 开头的字符串

---

## 5. 启动服务

```bash
# 确保在仓库根目录，且虚拟环境已激活
source venv/bin/activate

# 启动服务
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**看到以下输出即成功：**
```
INFO:     Uvicorn running on http://0.0.0.0:8000
🚀 服务启动完成 | 监听: 0.0.0.0:8000
📖 API 文档: http://0.0.0.0:8000/docs
```

---

## 6. 验证测试

**测试 1：健康检查**
```bash
curl http://localhost:8000/api/ai/v1/health
```
预期返回：
```json
{"code":200,"msg":"服务运行中","data":{"status":"ok","ocr_ready":true,"llm_ready":true}}
```

**测试 2：端到端填表（需要准备测试文件）**

先准备 4 个文件放到某个目录，比如 `/tmp/lingdoc/test/`：
1. 参考-成绩单.docx
2. 参考-获奖证书.docx
3. 参考-申请理由.docx
4. 国家奖学金申请表_空白_.docx

然后执行：
```bash
curl -X POST http://localhost:8000/api/ai/v1/form/fill \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: lingdoc-ai-2026-a7f3e9d2b8c1e4f5" \
  -d '{
    "reference_paths": [
      "/tmp/lingdoc/test/参考-成绩单.docx",
      "/tmp/lingdoc/test/参考-获奖证书.docx",
      "/tmp/lingdoc/test/参考-申请理由.docx"
    ],
    "template_path": "/tmp/lingdoc/test/国家奖学金申请表_空白_.docx",
    "output_path": "/tmp/lingdoc/output/已填写.docx"
  }'
```

**预期结果：**
- 返回 `{"code": 200, "msg": "填表成功", "data": {"success": true, ...}}`
- `/tmp/lingdoc/output/已填写.docx` 文件存在且大小 > 0
- fill_rate > 50%

---

## 7. 常见问题

| 问题 | 解决 |
|---|---|
| `python: command not found` | 用 `python3` 代替 `python` |
| `paddleocr` 安装失败 | 先装 `pip install paddlepaddle`，再装 `paddleocr` |
| `DASHSCOPE_API_KEY` 无效 | 检查 Key 是否过期，或余额不足 |
| 端口 8000 被占用 | 改 `.env` 里的 `AI_SERVICE_PORT` 为 8001，启动时加 `--port 8001` |
| OCR 首次加载慢 | 正常，模型下载约 3-5 秒，后续请求秒级 |
| LLM 返回空 | 检查网络，或 DashScope 服务状态 |

---

## 8. 文件结构速览

```
lingdoc_trial/              ← 仓库根目录（feature/ai-native 分支）
├── app/
│   ├── main.py              # FastAPI 入口
│   ├── config.py            # 配置读取
│   ├── prompts/
│   │   └── form_prompts.py  # Prompt 模板（替代 Dify 节点）
│   ├── services/
│   │   ├── form_service.py  # 填表 Pipeline（extract → generate → render）
│   │   ├── llm_client.py    # 直连 DashScope
│   │   ├── ocr_engine.py    # PaddleOCR
│   │   ├── docx_renderer.py # Word 渲染
│   │   └── xlsx_renderer.py # Excel 渲染
│   └── routers/
│       ├── form.py          # /api/ai/v1/form/* 接口
│       └── doc.py           # /api/ai/v1/doc/* 接口
├── .env                     # 环境变量（用户填写）
├── requirements.txt         # Python 依赖
└── venv/                    # 虚拟环境
```

**注意：没有 `lingdoc-ai-service/` 子目录，所有操作都在仓库根目录执行。**

---

## 9. 关键接口速查

| 接口 | 方法 | 用途 |
|---|---|---|
| `/api/ai/v1/health` | GET | 健康检查 |
| `/api/ai/v1/form/fill` | POST | 端到端填表（核心） |
| `/api/ai/v1/form/extract` | POST | 单步：提取信息 |
| `/api/ai/v1/form/generate` | POST | 单步：生成填写值 |
| `/api/ai/v1/form/render` | POST | 单步：渲染文档 |
| `/api/ai/v1/doc/process` | POST | 文档 OCR + 分析 |

全部接口文档：`http://localhost:8000/docs`（Swagger UI）

---

> 有任何报错直接贴日志，不要猜。
