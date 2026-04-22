# =============================================================================
# LingDoc AI Service - Docker Image
# =============================================================================
# 说明：生产环境 Docker 镜像构建文件
# 构建命令：docker build -t lingdoc-ai-service .
# 运行命令：docker run -p 8000:8000 --env-file .env lingdoc-ai-service
# =============================================================================

FROM python:3.11-slim

# 设置工作目录
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

# 复制依赖文件并安装
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app/ ./app/

# 创建上传目录
RUN mkdir -p /uploads/lingdoc /tmp/lingdoc/output

# 暴露端口
EXPOSE 8000

# 启动命令（生产模式，4 个 worker）
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
