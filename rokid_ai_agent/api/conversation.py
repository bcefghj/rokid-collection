"""会话管理 API"""

from fastapi import APIRouter

from core.memory import memory
from models.schemas import SessionInfo

router = APIRouter(prefix="/api/conversation", tags=["会话管理"])


@router.get("/sessions")
async def list_sessions():
    """列出所有活跃会话"""
    session_ids = memory.list_sessions()
    sessions = [memory.get_session_info(sid) for sid in session_ids]
    return {"sessions": sessions, "total": len(sessions)}


@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    """获取会话详情"""
    info = memory.get_session_info(session_id)
    history = memory.get_history(session_id, last_n=50)
    return {"info": info, "messages": history}


@router.delete("/sessions/{session_id}")
async def clear_session(session_id: str):
    """清除会话"""
    memory.clear_session(session_id)
    return {"message": f"会话 {session_id} 已清除"}
