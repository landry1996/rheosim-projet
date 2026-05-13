from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "RheoSim ML Service"
    app_version: str = "0.1.0"
    debug: bool = False

    grpc_port: int = 50052
    http_port: int = 8000

    model_dir: str = "models"
    onnx_classifier_path: str = "models/classifier.onnx"
    onnx_regressor_path: str = "models/param_estimator.onnx"

    mlflow_tracking_uri: str = "http://localhost:5000"
    mlflow_experiment_name: str = "rheosim-ml"

    retrain_batch_size: int = 1000
    retrain_min_samples: int = 500

    class Config:
        env_prefix = "RHEOSIM_ML_"


settings = Settings()
