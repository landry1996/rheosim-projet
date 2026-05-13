import threading

import structlog
import uvicorn

from src.config import settings
from src.inference.api_server import app
from src.inference.grpc_server import serve as grpc_serve

structlog.configure(
    processors=[
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),
    ]
)

logger = structlog.get_logger()


def main():
    logger.info(
        "Starting RheoSim ML Service",
        http_port=settings.http_port,
        grpc_port=settings.grpc_port,
    )

    grpc_thread = threading.Thread(target=grpc_serve, daemon=True)
    grpc_thread.start()

    uvicorn.run(app, host="0.0.0.0", port=settings.http_port)


if __name__ == "__main__":
    main()
