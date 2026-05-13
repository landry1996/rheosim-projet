import numpy as np
import pytest

from src.training.data_generator import RheologicalDataGenerator


@pytest.fixture
def generator():
    return RheologicalDataGenerator(seed=42)


def test_generate_dataset_correct_count(generator):
    dataset = generator.generate_dataset(n_samples=100)
    assert len(dataset["time_points"]) == 100
    assert len(dataset["values"]) == 100
    assert len(dataset["labels"]) == 100
    assert len(dataset["params"]) == 100


def test_generate_dataset_all_model_types(generator):
    dataset = generator.generate_dataset(n_samples=100)
    types = set(dataset["labels"])
    assert "maxwell" in types
    assert "kelvin_voigt" in types
    assert "prony_2" in types
    assert "prony_3" in types
    assert "prony_4" in types


def test_maxwell_relaxation_positive(generator):
    t = np.logspace(-2, 3, 100)
    params = {"G": 1e6, "tau": 10.0}
    y = generator._maxwell_relaxation(t, params)
    assert np.all(y > 0)
    assert y[0] > y[-1]


def test_prony_relaxation_converges(generator):
    t = np.logspace(-2, 5, 200)
    params = {"G_inf": 1e4, "G": [5e5, 3e5], "tau": [1.0, 100.0], "n_branches": 2}
    y = generator._prony_relaxation(t, params)
    assert np.isclose(y[-1], params["G_inf"], rtol=0.01)


def test_flatten_params_maxwell(generator):
    params = {"G": 1e6, "tau": 10.0}
    flat = generator.flatten_params(params, "maxwell")
    assert len(flat) == 15
    assert np.isclose(flat[0], np.log10(1e6))
    assert np.isclose(flat[1], np.log10(10.0))
    assert all(np.isnan(f) for f in flat[2:])


def test_flatten_params_prony(generator):
    params = {"G_inf": 1e4, "G": [5e5, 3e5], "tau": [1.0, 100.0], "n_branches": 2}
    flat = generator.flatten_params(params, "prony_2")
    assert len(flat) == 15
    assert np.isclose(flat[0], np.log10(1e4))


def test_reproducibility(generator):
    gen1 = RheologicalDataGenerator(seed=123)
    gen2 = RheologicalDataGenerator(seed=123)
    d1 = gen1.generate_dataset(n_samples=10)
    d2 = gen2.generate_dataset(n_samples=10)
    assert d1["labels"] == d2["labels"]
