"""Agent 对话 API"""

import json
import logging

from fastapi import APIRouter, HTTPException
from sse_starlette.sse import EventSourceResponse

from core.agent_executor import agent_executor
from core.llm_client import llm_client
from core.memory import memory
from models.schemas import AgentChatRequest, AgentChatResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/agent", tags=["Agent对话"])


@router.post("/chat", response_model=AgentChatResponse)
async def agent_chat(req: AgentChatRequest):
    """与 AI Agent 对话，Agent 会根据需要自动调用工具"""
    try:
        result = await agent_executor.run(
            user_message=req.message,
            session_id=req.session_id,
            scene_context=req.scene_context,
        )
        return AgentChatResponse(**result)
    except Exception as e:
        logger.exception("Agent 对话失败")
        raise HTTPException(500, f"Agent 处理失败: {e}")


@router.post("/chat/stream")
async def agent_chat_stream(req: AgentChatRequest):
    """流式对话（不使用工具，直接 LLM 流式输出）"""
    memory.add_message(req.session_id, "user", req.message)
    messages = [
        {
            "role": "system",
            "content": "你是 Rokid AI 眼镜助手，回答简洁准确。",
        },
    ]
    messages.extend(memory.get_history_as_messages(req.session_id, last_n=6))

    async def generate():
        full_response = []
        async for chunk in llm_client.chat_stream(messages):
            full_response.append(chunk)
            yield {"event": "message", "data": json.dumps({"content": chunk})}
        final = "".join(full_response)
        memory.add_message(req.session_id, "assistant", final)
        yield {"event": "done", "data": json.dumps({"full_response": final})}

    return EventSourceResponse(generate())
