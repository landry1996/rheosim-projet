import pytest
from httpx import ASGITransport, AsyncClient

from src.inference.api_server import app


@pytest.fixture
async def client():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest.mark.asyncio
async def test_health_endpoint(client):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "version" in data


@pytest.mark.asyncio
async def test_predict_without_models(client):
    response = await client.post("/predict", json={
        "time_points": [0.01, 0.1, 1.0, 10.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0],
        "values": [1e6, 9e5, 5e5, 1e5, 1e4, 8e3, 5e3, 3e3, 2e3, 1e3],
        "experiment_type": "relaxation",
    })
    # Models not loaded in test, should return 503
    assert response.status_code == 503


@pytest.mark.asyncio
async def test_predict_validation_length_mismatch(client):
    response = await client.post("/predict", json={
        "time_points": [1.0, 2.0],
        "values": [1.0],
        "experiment_type": "relaxation",
    })
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_predict_validation_too_few_points(client):
    response = await client.post("/predict", json={
        "time_points": [1.0, 2.0, 3.0],
        "values": [1.0, 2.0, 3.0],
        "experiment_type": "relaxation",
    })
    assert response.status_code == 400
