"""Rokid AI 智能场景 Agent - FastAPI 服务入口"""

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

from api import agent, conversation, tools as tools_api
from config import settings
from core.memory import memory
from core.tool_registry import registry
from models.schemas import HealthResponse

from tools import (
    search_tool,
    translate_tool,
    calculator_tool,
    knowledge_tool,
    datetime_tool,
    glasses_tool,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
logger = logging.getLogger(__name__)


def register_tools():
    tool_modules = [
        search_tool,
        translate_tool,
        calculator_tool,
        knowledge_tool,
        datetime_tool,
        glasses_tool,
    ]
    for module in tool_modules:
        defn = module.TOOL_DEFINITION
        registry.register(
            name=defn["name"],
            description=defn["description"],
            parameters=defn["parameters"],
            handler=defn["handler"],
        )
    logger.info("已注册 %d 个工具", len(registry.list_tools()))


register_tools()

app = FastAPI(
    title="Rokid AI 智能场景 Agent",
    description=(
        "基于 ReAct 模式的 AI Agent 系统，集成多种工具（搜索、翻译、计算、知识库、"
        "眼镜控制），为 Rokid AI 眼镜提供智能场景交互能力。"
    ),
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(agent.router)
app.include_router(conversation.router)
app.include_router(tools_api.router)


@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        version="1.0.0",
        llm_backend=settings.LLM_BACKEND.value,
        available_tools=registry.list_tools(),
        active_sessions=len(memory.list_sessions()),
    )


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.SERVER_HOST,
        port=settings.SERVER_PORT,
        reload=True,
    )
