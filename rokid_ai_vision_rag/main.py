"""Rokid AI 视觉知识助手 - FastAPI 服务入口"""

from pathlib import Path

try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).resolve().parent / ".env")
except ImportError:
    pass

import logging

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api import vision, rag, knowledge
from config import settings
from db.vector_store import vector_store
from models.schemas import HealthResponse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)

app = FastAPI(
    title="Rokid AI 视觉知识助手",
    description=(
        "基于多模态 LLM 和 RAG 的智能视觉问答系统，"
        "为 Rokid AI 眼镜提供拍照识物、知识增强问答等能力。"
    ),
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(vision.router)
app.include_router(rag.router)
app.include_router(knowledge.router)


@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        version="1.0.0",
        llm_backend=settings.LLM_BACKEND.value,
        knowledge_count=vector_store.count(),
    )


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.SERVER_HOST,
        port=settings.SERVER_PORT,
        reload=True,
    )
