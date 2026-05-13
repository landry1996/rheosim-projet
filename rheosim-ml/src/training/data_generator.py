import numpy as np
import structlog

logger = structlog.get_logger()


class RheologicalDataGenerator:
    """Generates synthetic rheological curves for ML model training."""

    def __init__(self, seed: int = 42):
        self.rng = np.random.default_rng(seed)

    def generate_dataset(self, n_samples: int = 100_000, noise_level: float = 0.02) -> dict:
        """Generate a full training dataset with multiple model types."""
        samples_per_type = {
            "maxwell": n_samples // 5,
            "kelvin_voigt": n_samples // 5,
            "prony_2": n_samples // 5,
            "prony_3": n_samples // 5,
            "prony_4": n_samples - 4 * (n_samples // 5),
        }

        all_time_points = []
        all_values = []
        all_labels = []
        all_params = []

        for model_type, count in samples_per_type.items():
            logger.info("Generating samples", model_type=model_type, count=count)
            for _ in range(count):
                t, y, params = self._generate_single(model_type, noise_level)
                all_time_points.append(t)
                all_values.append(y)
                all_labels.append(model_type)
                all_params.append(params)

        indices = self.rng.permutation(len(all_labels))
        return {
            "time_points": [all_time_points[i] for i in indices],
            "values": [all_values[i] for i in indices],
            "labels": [all_labels[i] for i in indices],
            "params": [all_params[i] for i in indices],
        }

    def _generate_single(self, model_type: str, noise_level: float) -> tuple:
        n_points = self.rng.integers(50, 500)
        t_max_log = self.rng.uniform(1, 5)
        t = np.logspace(-2, t_max_log, n_points)

        if model_type == "maxwell":
            params = self._maxwell_params()
            y = self._maxwell_relaxation(t, params)
        elif model_type == "kelvin_voigt":
            params = self._kelvin_voigt_params()
            y = self._kelvin_voigt_creep(t, params)
        elif model_type == "prony_2":
            params = self._prony_params(2)
            y = self._prony_relaxation(t, params)
        elif model_type == "prony_3":
            params = self._prony_params(3)
            y = self._prony_relaxation(t, params)
        elif model_type == "prony_4":
            params = self._prony_params(4)
            y = self._prony_relaxation(t, params)
        else:
            raise ValueError(f"Unknown model type: {model_type}")

        noise = self.rng.normal(0, noise_level * np.max(np.abs(y)), size=len(y))
        y_noisy = y + noise

        return t, y_noisy, params

    def _maxwell_params(self) -> dict:
        G = self.rng.uniform(1e3, 1e9)
        tau = 10 ** self.rng.uniform(-2, 4)
        return {"G": G, "tau": tau}

    def _maxwell_relaxation(self, t: np.ndarray, params: dict) -> np.ndarray:
        return params["G"] * np.exp(-t / params["tau"])

    def _kelvin_voigt_params(self) -> dict:
        J0 = 10 ** self.rng.uniform(-9, -3)
        tau = 10 ** self.rng.uniform(-2, 4)
        return {"J0": J0, "tau": tau}

    def _kelvin_voigt_creep(self, t: np.ndarray, params: dict) -> np.ndarray:
        return params["J0"] * (1 - np.exp(-t / params["tau"]))

    def _prony_params(self, n_branches: int) -> dict:
        G_inf = 10 ** self.rng.uniform(2, 6)
        taus = np.sort(10 ** self.rng.uniform(-2, 4, size=n_branches))
        weights = self.rng.dirichlet(np.ones(n_branches))
        G_total = 10 ** self.rng.uniform(3, 9)
        Gs = weights * G_total
        return {"G_inf": G_inf, "G": Gs.tolist(), "tau": taus.tolist(), "n_branches": n_branches}

    def _prony_relaxation(self, t: np.ndarray, params: dict) -> np.ndarray:
        result = np.full_like(t, params["G_inf"])
        for g, tau in zip(params["G"], params["tau"]):
            result += g * np.exp(-t / tau)
        return result

    def flatten_params(self, params: dict, model_type: str, max_params: int = 15) -> list[float]:
        """Flatten parameter dict to fixed-size vector for ML training."""
        flat = []
        if model_type == "maxwell":
            flat = [np.log10(params["G"]), np.log10(params["tau"])]
        elif model_type == "kelvin_voigt":
            flat = [np.log10(params["J0"]), np.log10(params["tau"])]
        elif model_type.startswith("prony"):
            flat = [np.log10(params["G_inf"])]
            for g in params["G"]:
                flat.append(np.log10(g))
            for tau in params["tau"]:
                flat.append(np.log10(tau))

        # Pad to fixed size
        flat.extend([float("nan")] * (max_params - len(flat)))
        return flat[:max_params]
