"""API 端点测试"""

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
    assert "version" in data
    assert "llm_backend" in data


def test_knowledge_count(client):
    resp = client.get("/api/knowledge/count")
    assert resp.status_code == 200
    data = resp.json()
    assert "count" in data


def test_analyze_no_image(client):
    resp = client.post("/api/vision/analyze")
    assert resp.status_code == 422


def test_rag_query_no_api_key(client):
    """无 API Key 时 RAG 查询应返回错误或空结果"""
    resp = client.post(
        "/api/rag/query",
        json={"query": "测试查询", "top_k": 3},
    )
    # 无 API key 时会返回 500（无法生成 embedding）或 200（空知识库）
    assert resp.status_code in (200, 500)
