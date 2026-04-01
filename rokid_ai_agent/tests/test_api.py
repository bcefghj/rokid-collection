"""API 测试"""

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    from main import app
    return TestClient(app)


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "ok"
    assert len(data["available_tools"]) > 0


def test_list_tools(client):
    resp = client.get("/api/tools/list")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data["tools"]) >= 5
    tool_names = [t["name"] for t in data["tools"]]
    assert "calculator" in tool_names
    assert "translate" in tool_names


def test_execute_calculator(client):
    resp = client.post(
        "/api/tools/execute/calculator",
        json={"expression": "2+3"},
    )
    assert resp.status_code == 200
    assert "5" in resp.json()["result"]


def test_execute_datetime(client):
    resp = client.post(
        "/api/tools/execute/get_datetime",
        json={"action": "now"},
    )
    assert resp.status_code == 200
    assert "当前时间" in resp.json()["result"]


def test_list_sessions(client):
    resp = client.get("/api/conversation/sessions")
    assert resp.status_code == 200
    assert "sessions" in resp.json()
