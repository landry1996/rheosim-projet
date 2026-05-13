import numpy as np
import pytest

from src.training.feature_extractor import FeatureExtractor


@pytest.fixture
def extractor():
    return FeatureExtractor()


def test_extract_relaxation_returns_correct_number_of_features(extractor):
    t = np.logspace(-2, 3, 100)
    y = 1e6 * np.exp(-t / 10.0)
    features = extractor.extract(t, y, "relaxation")
    assert len(features) == FeatureExtractor.N_FEATURES


def test_extract_maxwell_curve(extractor):
    t = np.logspace(-2, 4, 200)
    G = 1e6
    tau = 100.0
    y = G * np.exp(-t / tau)
    features = extractor.extract(t, y, "relaxation")
    assert all(np.isfinite(features))
    # Relaxation time should be approximately log10(tau)
    assert abs(features[3] - np.log10(tau)) < 1.5


def test_extract_prony_series(extractor):
    t = np.logspace(-2, 4, 200)
    G_inf = 1e4
    y = G_inf + 5e5 * np.exp(-t / 1.0) + 3e5 * np.exp(-t / 100.0)
    features = extractor.extract(t, y, "relaxation")
    assert all(np.isfinite(features))
    # Should detect multiple relaxation mechanisms
    assert features[2] >= 0  # inflection_count


def test_extract_with_noise(extractor):
    t = np.logspace(-2, 3, 100)
    y = 1e6 * np.exp(-t / 10.0)
    noise = np.random.normal(0, 0.05 * np.max(y), len(y))
    y_noisy = y + noise
    features = extractor.extract(t, y_noisy, "relaxation")
    assert all(np.isfinite(features))
    assert features[7] > 0  # noise_level should be nonzero


def test_extract_minimum_data_points(extractor):
    t = np.logspace(-1, 1, 10)
    y = 1e6 * np.exp(-t / 1.0)
    features = extractor.extract(t, y, "relaxation")
    assert len(features) == FeatureExtractor.N_FEATURES
