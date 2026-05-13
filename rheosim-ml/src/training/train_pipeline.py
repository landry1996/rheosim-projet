import os

import numpy as np
import onnx
import structlog
import torch
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

from src.config import settings
from src.training.data_generator import RheologicalDataGenerator
from src.training.feature_extractor import FeatureExtractor
from src.training.model_classifier import ModelClassifierTrainer
from src.training.param_estimator import MAX_PARAMS, ParameterEstimatorTrainer

logger = structlog.get_logger()


def run_training_pipeline(
    n_samples: int = 100_000,
    noise_level: float = 0.02,
    output_dir: str | None = None,
) -> dict:
    """Full training pipeline: generate data, train models, export ONNX."""

    output_dir = output_dir or settings.model_dir
    os.makedirs(output_dir, exist_ok=True)

    logger.info("Starting training pipeline", n_samples=n_samples)

    # Step 1: Generate synthetic data
    generator = RheologicalDataGenerator(seed=42)
    dataset = generator.generate_dataset(n_samples=n_samples, noise_level=noise_level)
    logger.info("Dataset generated", total_samples=len(dataset["labels"]))

    # Step 2: Train classifier
    classifier_trainer = ModelClassifierTrainer()
    X_clf, y_clf = classifier_trainer.prepare_features(dataset)
    logger.info("Classifier features prepared", n_samples=len(X_clf), n_features=X_clf.shape[1])

    clf_metrics = classifier_trainer.train(X_clf, y_clf)
    logger.info("Classifier trained", accuracy=clf_metrics["accuracy"])

    # Step 3: Export classifier to ONNX
    classifier = classifier_trainer.get_model()
    n_features = X_clf.shape[1]
    onnx_classifier = convert_sklearn(
        classifier,
        "rheosim_classifier",
        initial_types=[("features", FloatTensorType([None, n_features]))],
        options={id(classifier): {"zipmap": False}},
    )
    classifier_path = os.path.join(output_dir, "classifier.onnx")
    onnx.save_model(onnx_classifier, classifier_path)
    logger.info("Classifier exported to ONNX", path=classifier_path)

    # Step 4: Train parameter estimator
    estimator_trainer = ParameterEstimatorTrainer()
    X_reg, y_reg = estimator_trainer.prepare_data(dataset)
    logger.info("Estimator features prepared", n_samples=len(X_reg))

    reg_metrics = estimator_trainer.train(X_reg, y_reg, epochs=100)
    logger.info("Parameter estimator trained", rmse=reg_metrics["final_rmse"])

    # Step 5: Export parameter estimator to ONNX
    model = estimator_trainer.get_model()
    model.eval()
    dummy_input = torch.randn(1, n_features)
    regressor_path = os.path.join(output_dir, "param_estimator.onnx")
    torch.onnx.export(
        model,
        dummy_input,
        regressor_path,
        input_names=["features"],
        output_names=["parameters"],
        dynamic_axes={"features": {0: "batch_size"}, "parameters": {0: "batch_size"}},
        opset_version=17,
    )
    logger.info("Parameter estimator exported to ONNX", path=regressor_path)

    return {
        "classifier_metrics": clf_metrics,
        "estimator_metrics": reg_metrics,
        "classifier_path": classifier_path,
        "regressor_path": regressor_path,
        "n_samples_used": len(X_clf),
    }


if __name__ == "__main__":
    import structlog

    structlog.configure(
        processors=[
            structlog.stdlib.add_log_level,
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.dev.ConsoleRenderer(),
        ]
    )
    results = run_training_pipeline(n_samples=10_000)
    print(f"\nTraining complete!")
    print(f"  Classifier accuracy: {results['classifier_metrics']['accuracy']:.3f}")
    print(f"  Estimator RMSE: {results['estimator_metrics']['final_rmse']:.4f}")
