import numpy as np
from scipy import signal
from scipy.interpolate import UnivariateSpline


class FeatureExtractor:
    """Extracts rheological features from experimental time-domain or frequency-domain curves."""

    N_FEATURES = 12

    def extract(self, t: np.ndarray, y: np.ndarray, experiment_type: str = "relaxation") -> list[float]:
        t = np.asarray(t, dtype=np.float64)
        y = np.asarray(y, dtype=np.float64)

        if experiment_type == "relaxation":
            return self._extract_relaxation_features(t, y)
        elif experiment_type == "creep":
            return self._extract_creep_features(t, y)
        elif experiment_type == "oscillation":
            return self._extract_oscillation_features(t, y)
        else:
            return self._extract_relaxation_features(t, y)

    def _extract_relaxation_features(self, t: np.ndarray, y: np.ndarray) -> list[float]:
        features = []

        # 1. Initial slope (log-log)
        log_t = np.log10(t[t > 0])
        log_y = np.log10(np.abs(y[t > 0]) + 1e-30)
        n_init = max(3, len(log_t) // 10)
        if len(log_t) >= 3:
            slope_init = np.polyfit(log_t[:n_init], log_y[:n_init], 1)[0]
        else:
            slope_init = 0.0
        features.append(slope_init)

        # 2. Terminal slope (log-log)
        n_term = max(3, len(log_t) // 10)
        if len(log_t) >= 3:
            slope_term = np.polyfit(log_t[-n_term:], log_y[-n_term:], 1)[0]
        else:
            slope_term = 0.0
        features.append(slope_term)

        # 3. Number of inflection points
        if len(y) > 5:
            try:
                spline = UnivariateSpline(t, y, s=len(t) * np.var(y) * 0.01)
                d2 = spline.derivative(n=2)(t)
                sign_changes = np.where(np.diff(np.sign(d2)))[0]
                inflection_count = len(sign_changes)
            except Exception:
                inflection_count = 0
        else:
            inflection_count = 0
        features.append(float(inflection_count))

        # 4. Approximate relaxation time (time at which y drops to 1/e of initial)
        y_max = np.max(np.abs(y))
        threshold = y_max / np.e
        below_threshold = np.where(np.abs(y) <= threshold)[0]
        if len(below_threshold) > 0:
            tau_approx = t[below_threshold[0]]
        else:
            tau_approx = t[-1]
        features.append(np.log10(tau_approx + 1e-30))

        # 5. Plateau modulus (asymptotic value)
        plateau = np.mean(y[-max(3, len(y) // 20):])
        features.append(np.log10(np.abs(plateau) + 1e-30))

        # 6. Ratio G_inf / G_0
        g0 = np.abs(y[0]) if len(y) > 0 else 1.0
        g_inf = np.abs(plateau)
        ratio = g_inf / (g0 + 1e-30)
        features.append(ratio)

        # 7. Spectral width (FFT-based)
        if len(y) > 8:
            fft_vals = np.abs(np.fft.rfft(y - np.mean(y)))
            if np.sum(fft_vals) > 0:
                freqs = np.fft.rfftfreq(len(y), d=(t[1] - t[0]) if len(t) > 1 else 1.0)
                weighted_freq = np.sum(freqs * fft_vals) / np.sum(fft_vals)
                spectral_width = np.sqrt(
                    np.sum((freqs - weighted_freq) ** 2 * fft_vals) / np.sum(fft_vals)
                )
            else:
                spectral_width = 0.0
        else:
            spectral_width = 0.0
        features.append(spectral_width)

        # 8. Noise level (residual std from smooth fit)
        if len(y) > 10:
            try:
                spline = UnivariateSpline(t, y, s=len(t) * np.var(y) * 0.1)
                residuals = y - spline(t)
                noise_level = np.std(residuals) / (y_max + 1e-30)
            except Exception:
                noise_level = 0.0
        else:
            noise_level = 0.0
        features.append(noise_level)

        # 9. Decay rate (exponential fit quality)
        if len(log_y) >= 5 and np.all(np.isfinite(log_y)):
            poly = np.polyfit(t[t > 0][:len(log_y)], log_y, 1)
            decay_rate = poly[0]
        else:
            decay_rate = 0.0
        features.append(decay_rate)

        # 10. Log-time span
        if t[-1] > t[0] > 0:
            time_span = np.log10(t[-1] / t[0])
        else:
            time_span = 0.0
        features.append(time_span)

        # 11. Curvature at midpoint (normalized)
        mid_idx = len(y) // 2
        if mid_idx > 1 and mid_idx < len(y) - 1:
            dt_local = t[mid_idx + 1] - t[mid_idx - 1]
            if dt_local > 0:
                curvature = (y[mid_idx + 1] - 2 * y[mid_idx] + y[mid_idx - 1]) / (dt_local ** 2)
                curvature_norm = curvature / (y_max + 1e-30)
            else:
                curvature_norm = 0.0
        else:
            curvature_norm = 0.0
        features.append(curvature_norm)

        # 12. Number of decades of relaxation
        if y_max > 0 and np.abs(plateau) > 0:
            decades = np.log10(y_max / (np.abs(plateau) + 1e-30))
        else:
            decades = 0.0
        features.append(decades)

        return features

    def _extract_creep_features(self, t: np.ndarray, y: np.ndarray) -> list[float]:
        # For creep compliance J(t), similar features but inverted behavior
        return self._extract_relaxation_features(t, y)

    def _extract_oscillation_features(self, t: np.ndarray, y: np.ndarray) -> list[float]:
        # For oscillatory data (frequency domain G', G'')
        return self._extract_relaxation_features(t, y)
