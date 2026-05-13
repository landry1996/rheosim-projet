import numpy as np
import structlog
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import cross_val_score
from xgboost import XGBClassifier

from src.training.data_generator import RheologicalDataGenerator
from src.training.feature_extractor import FeatureExtractor

logger = structlog.get_logger()

MODEL_TYPES = ["maxwell", "kelvin_voigt", "prony_2", "prony_3", "prony_4"]
LABEL_MAP = {name: idx for idx, name in enumerate(MODEL_TYPES)}


class ModelClassifierTrainer:
    """Trains an XGBoost classifier to predict constitutive model type from rheological features."""

    def __init__(self):
        self.feature_extractor = FeatureExtractor()
        self.classifier = XGBClassifier(
            n_estimators=200,
            max_depth=8,
            learning_rate=0.1,
            objective="multi:softprob",
            num_class=len(MODEL_TYPES),
            eval_metric="mlogloss",
            use_label_encoder=False,
            n_jobs=-1,
            random_state=42,
        )

    def prepare_features(self, dataset: dict) -> tuple[np.ndarray, np.ndarray]:
        """Extract features from generated curves."""
        X = []
        y = []

        for time_pts, values, label in zip(
            dataset["time_points"], dataset["values"], dataset["labels"]
        ):
            features = self.feature_extractor.extract(
                np.array(time_pts), np.array(values), "relaxation"
            )
            if all(np.isfinite(features)):
                X.append(features)
                y.append(LABEL_MAP[label])

        return np.array(X, dtype=np.float32), np.array(y, dtype=np.int32)

    def train(self, X: np.ndarray, y: np.ndarray, validation_split: float = 0.2) -> dict:
        """Train the classifier and return metrics."""
        n_val = int(len(X) * validation_split)
        X_train, X_val = X[n_val:], X[:n_val]
        y_train, y_val = y[n_val:], y[:n_val]

        logger.info("Training classifier", n_train=len(X_train), n_val=len(X_val))

        self.classifier.fit(
            X_train, y_train,
            eval_set=[(X_val, y_val)],
            verbose=False,
        )

        y_pred = self.classifier.predict(X_val)
        accuracy = accuracy_score(y_val, y_pred)

        cv_scores = cross_val_score(self.classifier, X, y, cv=5, scoring="accuracy")

        metrics = {
            "accuracy": float(accuracy),
            "cv_mean": float(cv_scores.mean()),
            "cv_std": float(cv_scores.std()),
            "report": classification_report(y_val, y_pred, target_names=MODEL_TYPES),
        }

        logger.info(
            "Classifier trained",
            accuracy=accuracy,
            cv_mean=cv_scores.mean(),
            cv_std=cv_scores.std(),
        )

        return metrics

    def get_model(self) -> XGBClassifier:
        return self.classifier
