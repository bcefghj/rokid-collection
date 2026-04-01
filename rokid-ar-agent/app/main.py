"""FastAPI 服务入口：提供 RESTful API 和 SSE 流式接口"""

import uuid
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from app.agent import chat, chat_stream, clear_session
from app.rag.knowledge_base import add_documents, query_knowledge
from app.config import get_settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Rokid AR Agent 服务启动")
    yield
    logger.info("Rokid AR Agent 服务关闭")


app = FastAPI(
    title="Rokid AR Agent",
    description="基于 ReAct 的 AR 眼镜智能助手 API",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    session_id: str | None = None
    message: str
    stream: bool = False


class ChatResponse(BaseModel):
    session_id: str
    reply: str


class KnowledgeAddRequest(BaseModel):
    texts: list[str]


class KnowledgeQueryRequest(BaseModel):
    query: str
    top_k: int = 3


@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(req: ChatRequest):
    """同步对话接口"""
    session_id = req.session_id or str(uuid.uuid4())
    logger.info(f"[{session_id}] 用户: {req.message}")

    try:
        reply = chat(session_id, req.message)
        logger.info(f"[{session_id}] 助手: {reply}")
        return ChatResponse(session_id=session_id, reply=reply)
    except Exception as e:
        logger.error(f"[{session_id}] 错误: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chat/stream")
async def chat_stream_endpoint(req: ChatRequest):
    """SSE 流式对话接口"""
    session_id = req.session_id or str(uuid.uuid4())
    logger.info(f"[{session_id}] 流式请求: {req.message}")

    async def event_generator():
        async for chunk in chat_stream(session_id, req.message):
            yield {"event": "message", "data": chunk}
        yield {"event": "done", "data": "[DONE]"}

    return EventSourceResponse(event_generator())


@app.delete("/session/{session_id}")
async def delete_session(session_id: str):
    """清除会话历史"""
    clear_session(session_id)
    return {"status": "ok", "message": f"会话 {session_id} 已清除"}


@app.post("/knowledge/add")
async def add_knowledge(req: KnowledgeAddRequest):
    """向知识库添加文档"""
    count = add_documents(req.texts)
    return {"status": "ok", "added": count}


@app.post("/knowledge/query")
async def query_knowledge_endpoint(req: KnowledgeQueryRequest):
    """检索知识库"""
    results = query_knowledge(req.query, n_results=req.top_k)
    return {"results": results}


@app.get("/health")
async def health():
    return {"status": "ok", "service": "rokid-ar-agent"}
