"""工具管理 API"""

from fastapi import APIRouter

from core.tool_registry import registry

router = APIRouter(prefix="/api/tools", tags=["工具管理"])


@router.get("/list")
async def list_tools():
    """列出所有可用工具"""
    return {"tools": registry.get_tool_info()}


@router.post("/execute/{tool_name}")
async def execute_tool(tool_name: str, arguments: dict = {}):
    """直接执行指定工具"""
    import json
    result = await registry.execute(tool_name, json.dumps(arguments))
    return {"tool": tool_name, "arguments": arguments, "result": result}
