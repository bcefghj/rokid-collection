"""数据模型定义"""

from __future__ import annotations

from typing import Optional
from pydantic import BaseModel, Field


class AgentChatRequest(BaseModel):
    message: str = Field(description="用户消息")
    session_id: str = Field(default="default", description="会话ID")
    image_path: Optional[str] = Field(
        default=None, description="图片路径（可选，用于视觉分析）"
    )
    scene_context: str = Field(
        default="",
        description="场景上下文（如位置、时间等额外信息）",
    )


class ToolCall(BaseModel):
    tool_name: str
    tool_input: str
    tool_output: str


class AgentChatResponse(BaseModel):
    answer: str = Field(description="Agent 最终回答")
    session_id: str
    tool_calls: list[ToolCall] = Field(
        default_factory=list, description="工具调用记录"
    )
    thinking: str = Field(default="", description="推理过程")


class ConversationMessage(BaseModel):
    role: str
    content: str
    timestamp: Optional[str] = None


class SessionInfo(BaseModel):
    session_id: str
    message_count: int
    created_at: str
    last_active: str


class ToolInfo(BaseModel):
    name: str
    description: str
    parameters: dict = Field(default_factory=dict)


class HealthResponse(BaseModel):
    status: str
    version: str
    llm_backend: str
    available_tools: list[str]
    active_sessions: int
