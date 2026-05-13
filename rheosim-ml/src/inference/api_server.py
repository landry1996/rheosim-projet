from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from src.config import settings
from src.inference.onnx_predictor import OnnxPredictor

logger = structlog.get_logger()

predictor: OnnxPredictor | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global predictor
    try:
        predictor = OnnxPredictor(
            classifier_path=settings.onnx_classifier_path,
            regressor_path=settings.onnx_regressor_path,
        )
        logger.info("ML models loaded successfully")
    except FileNotFoundError:
        logger.warning("ONNX models not found, prediction endpoints will return 503")
        predictor = None
    yield
    predictor = None


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)


class PredictionRequest(BaseModel):
    time_points: list[float]
    values: list[float]
    experiment_type: str = "relaxation"


class ModelAlternative(BaseModel):
    model_type: str
    confidence: float
    initial_parameters: list[float]


class PredictionResponse(BaseModel):
    recommended_model: str
    confidence: float
    initial_parameters: list[float]
    alternatives: list[ModelAlternative]


class HealthResponse(BaseModel):
    status: str
    models_loaded: bool
    version: str


@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        models_loaded=predictor is not None,
        version=settings.app_version,
    )


@app.post("/predict", response_model=PredictionResponse)
async def predict_model(request: PredictionRequest):
    if predictor is None:
        raise HTTPException(status_code=503, detail="Models not loaded")

    if len(request.time_points) != len(request.values):
        raise HTTPException(status_code=400, detail="time_points and values must have same length")

    if len(request.time_points) < 10:
        raise HTTPException(status_code=400, detail="Need at least 10 data points")

    result = predictor.predict(
        time_points=request.time_points,
        values=request.values,
        experiment_type=request.experiment_type,
    )
    return result


@app.post("/predict/batch", response_model=list[PredictionResponse])
async def predict_batch(requests: list[PredictionRequest]):
    if predictor is None:
        raise HTTPException(status_code=503, detail="Models not loaded")

    results = []
    for req in requests:
        if len(req.time_points) != len(req.values):
            raise HTTPException(status_code=400, detail="time_points and values must have same length")
        result = predictor.predict(
            time_points=req.time_points,
            values=req.values,
            experiment_type=req.experiment_type,
        )
        results.append(result)
    return results
