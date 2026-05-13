import asyncio
from concurrent import futures

import grpc
import numpy as np
import structlog

from src.config import settings
from src.inference.onnx_predictor import OnnxPredictor
from src.proto import ml_service_pb2, ml_service_pb2_grpc

logger = structlog.get_logger()


class MLServiceServicer(ml_service_pb2_grpc.MLServiceServicer):
    def __init__(self):
        self.predictor: OnnxPredictor | None = None
        try:
            self.predictor = OnnxPredictor(
                classifier_path=settings.onnx_classifier_path,
                regressor_path=settings.onnx_regressor_path,
            )
        except FileNotFoundError:
            logger.warning("ONNX models not found for gRPC server")

    def PredictModel(self, request, context):
        if self.predictor is None:
            context.set_code(grpc.StatusCode.UNAVAILABLE)
            context.set_details("Models not loaded")
            return ml_service_pb2.PredictionResponse()

        result = self.predictor.predict(
            time_points=list(request.time_points),
            values=list(request.values),
            experiment_type=request.experiment_type,
        )

        alternatives = [
            ml_service_pb2.ModelAlternative(
                model_type=alt["model_type"],
                confidence=alt["confidence"],
                initial_parameters=alt["initial_parameters"],
            )
            for alt in result["alternatives"]
        ]

        return ml_service_pb2.PredictionResponse(
            recommended_model=result["recommended_model"],
            confidence=result["confidence"],
            initial_parameters=result["initial_parameters"],
            alternatives=alternatives,
        )

    def EstimateParameters(self, request, context):
        if self.predictor is None:
            context.set_code(grpc.StatusCode.UNAVAILABLE)
            context.set_details("Models not loaded")
            return ml_service_pb2.EstimationResponse()

        result = self.predictor.predict(
            time_points=list(request.time_points),
            values=list(request.values),
            experiment_type=request.experiment_type,
        )

        return ml_service_pb2.EstimationResponse(
            parameters=result["initial_parameters"],
            confidence=result["confidence"],
        )

    def RetrainModel(self, request, context):
        logger.info("Retrain requested", min_samples=request.min_samples, force=request.force)
        return ml_service_pb2.RetrainResponse(
            success=False,
            message="Retrain not yet implemented in production mode",
            new_accuracy=0.0,
            samples_used=0,
        )

    def HealthCheck(self, request, context):
        return ml_service_pb2.HealthResponse(
            status="healthy",
            models_loaded=self.predictor is not None,
            version=settings.app_version,
        )


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    ml_service_pb2_grpc.add_MLServiceServicer_to_server(MLServiceServicer(), server)
    server.add_insecure_port(f"[::]:{settings.grpc_port}")
    server.start()
    logger.info("gRPC server started", port=settings.grpc_port)
    server.wait_for_termination()
