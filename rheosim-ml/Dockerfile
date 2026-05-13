FROM python:3.11-slim AS base

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Generate proto files
RUN python -m grpc_tools.protoc \
    -I src/proto \
    --python_out=src/proto \
    --grpc_python_out=src/proto \
    src/proto/ml_service.proto

EXPOSE 8000 50052

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"

CMD ["python", "main.py"]
