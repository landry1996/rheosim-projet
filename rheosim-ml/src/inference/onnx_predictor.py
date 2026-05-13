import numpy as np
import onnxruntime as ort
import structlog

from src.training.feature_extractor import FeatureExtractor

logger = structlog.get_logger()

MODEL_TYPES = ["maxwell", "kelvin_voigt", "prony_2", "prony_3", "prony_4"]


class OnnxPredictor:
    def __init__(self, classifier_path: str, regressor_path: str):
        self.classifier_session = ort.InferenceSession(classifier_path)
        self.regressor_session = ort.InferenceSession(regressor_path)
        self.feature_extractor = FeatureExtractor()
        logger.info("ONNX sessions initialized", classifier=classifier_path, regressor=regressor_path)

    def predict(self, time_points: list[float], values: list[float], experiment_type: str) -> dict:
        t = np.array(time_points, dtype=np.float64)
        y = np.array(values, dtype=np.float64)

        features = self.feature_extractor.extract(t, y, experiment_type)
        features_array = np.array([features], dtype=np.float32)

        classifier_input = {self.classifier_session.get_inputs()[0].name: features_array}
        class_probs = self.classifier_session.run(None, classifier_input)[0][0]

        top_idx = int(np.argmax(class_probs))
        recommended_model = MODEL_TYPES[top_idx]
        confidence = float(class_probs[top_idx])

        regressor_input = {self.regressor_session.get_inputs()[0].name: features_array}
        params = self.regressor_session.run(None, regressor_input)[0][0]
        initial_parameters = [float(p) for p in params if not np.isnan(p)]

        sorted_indices = np.argsort(class_probs)[::-1]
        alternatives = []
        for idx in sorted_indices[1:4]:
            if class_probs[idx] > 0.05:
                alternatives.append({
                    "model_type": MODEL_TYPES[idx],
                    "confidence": float(class_probs[idx]),
                    "initial_parameters": initial_parameters,
                })

        return {
            "recommended_model": recommended_model,
            "confidence": confidence,
            "initial_parameters": initial_parameters,
            "alternatives": alternatives,
        }
