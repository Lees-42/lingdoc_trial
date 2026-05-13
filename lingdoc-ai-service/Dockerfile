# LingDoc AI Service Docker Image
# =============================================================================
# 构建: docker build -t lingdoc-ai-service .
# 运行: docker run -p 8000:8000 -e DASHSCOPE_API_KEY=xxx lingdoc-ai-service
# =============================================================================

FROM python:3.12-slim

WORKDIR /app

# 安装系统依赖（PaddleOCR 需要）
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY app/ ./app/
COPY .env .

# 暴露端口
EXPOSE 8000

# 启动命令（可被覆盖）
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
