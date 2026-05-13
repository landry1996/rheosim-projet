import numpy as np
import structlog
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, TensorDataset

from src.training.data_generator import RheologicalDataGenerator
from src.training.feature_extractor import FeatureExtractor

logger = structlog.get_logger()

MAX_PARAMS = 15


class ParameterEstimatorNetwork(nn.Module):
    """Neural network for estimating initial parameters of constitutive models."""

    def __init__(self, n_features: int = 12, n_outputs: int = MAX_PARAMS):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(n_features, 128),
            nn.ReLU(),
            nn.BatchNorm1d(128),
            nn.Dropout(0.2),
            nn.Linear(128, 256),
            nn.ReLU(),
            nn.BatchNorm1d(256),
            nn.Dropout(0.2),
            nn.Linear(256, 128),
            nn.ReLU(),
            nn.BatchNorm1d(128),
            nn.Linear(128, n_outputs),
        )

    def forward(self, x):
        return self.network(x)


class ParameterEstimatorTrainer:
    """Trains a neural network to estimate initial parameters from rheological features."""

    def __init__(self, device: str = "cpu"):
        self.feature_extractor = FeatureExtractor()
        self.device = torch.device(device)
        self.model = ParameterEstimatorNetwork().to(self.device)

    def prepare_data(self, dataset: dict) -> tuple[np.ndarray, np.ndarray]:
        """Extract features and flatten parameters."""
        generator = RheologicalDataGenerator()
        X = []
        y = []

        for time_pts, values, label, params in zip(
            dataset["time_points"],
            dataset["values"],
            dataset["labels"],
            dataset["params"],
        ):
            features = self.feature_extractor.extract(
                np.array(time_pts), np.array(values), "relaxation"
            )
            flat_params = generator.flatten_params(params, label, MAX_PARAMS)

            if all(np.isfinite(features)):
                X.append(features)
                y.append(flat_params)

        return np.array(X, dtype=np.float32), np.array(y, dtype=np.float32)

    def train(
        self,
        X: np.ndarray,
        y: np.ndarray,
        epochs: int = 100,
        batch_size: int = 256,
        lr: float = 1e-3,
        validation_split: float = 0.2,
    ) -> dict:
        """Train the parameter estimator network."""
        n_val = int(len(X) * validation_split)
        X_train, X_val = X[n_val:], X[:n_val]
        y_train, y_val = y[n_val:], y[:n_val]

        # Replace NaN targets with 0 and create mask
        mask_train = ~np.isnan(y_train)
        mask_val = ~np.isnan(y_val)
        y_train_clean = np.nan_to_num(y_train, nan=0.0)
        y_val_clean = np.nan_to_num(y_val, nan=0.0)

        train_dataset = TensorDataset(
            torch.from_numpy(X_train),
            torch.from_numpy(y_train_clean),
            torch.from_numpy(mask_train.astype(np.float32)),
        )
        train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)

        optimizer = torch.optim.Adam(self.model.parameters(), lr=lr)
        scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, patience=10, factor=0.5)

        best_val_loss = float("inf")
        history = []

        for epoch in range(epochs):
            self.model.train()
            train_loss = 0.0
            n_batches = 0

            for batch_x, batch_y, batch_mask in train_loader:
                batch_x = batch_x.to(self.device)
                batch_y = batch_y.to(self.device)
                batch_mask = batch_mask.to(self.device)

                optimizer.zero_grad()
                pred = self.model(batch_x)
                loss = ((pred - batch_y) ** 2 * batch_mask).sum() / batch_mask.sum()
                loss.backward()
                optimizer.step()

                train_loss += loss.item()
                n_batches += 1

            # Validation
            self.model.eval()
            with torch.no_grad():
                val_x = torch.from_numpy(X_val).to(self.device)
                val_y = torch.from_numpy(y_val_clean).to(self.device)
                val_mask = torch.from_numpy(mask_val.astype(np.float32)).to(self.device)
                val_pred = self.model(val_x)
                val_loss = ((val_pred - val_y) ** 2 * val_mask).sum() / val_mask.sum()

            avg_train = train_loss / n_batches
            scheduler.step(val_loss.item())

            if val_loss.item() < best_val_loss:
                best_val_loss = val_loss.item()

            if (epoch + 1) % 20 == 0:
                logger.info(
                    "Training progress",
                    epoch=epoch + 1,
                    train_loss=avg_train,
                    val_loss=val_loss.item(),
                )

            history.append({"epoch": epoch + 1, "train_loss": avg_train, "val_loss": val_loss.item()})

        # Compute RMSE on validation
        self.model.eval()
        with torch.no_grad():
            val_x = torch.from_numpy(X_val).to(self.device)
            val_pred = self.model(val_x).cpu().numpy()

        valid_mask = mask_val
        rmse = np.sqrt(np.mean((val_pred[valid_mask] - y_val_clean[valid_mask]) ** 2))

        metrics = {
            "best_val_loss": best_val_loss,
            "final_rmse": float(rmse),
            "epochs_trained": epochs,
        }

        logger.info("Parameter estimator trained", rmse=rmse, best_val_loss=best_val_loss)
        return metrics

    def get_model(self) -> ParameterEstimatorNetwork:
        return self.model
