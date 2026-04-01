"""工具注册表 - 管理 Agent 可用的所有工具"""

from __future__ import annotations

import json
import logging
from typing import Callable, Awaitable, Optional

logger = logging.getLogger(__name__)


class ToolDefinition:
    """工具定义"""

    def __init__(
        self,
        name: str,
        description: str,
        parameters: dict,
        handler: Callable[..., Awaitable[str]],
    ):
        self.name = name
        self.description = description
        self.parameters = parameters
        self.handler = handler

    def to_openai_tool(self) -> dict:
        """转换为 OpenAI function calling 格式"""
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters,
            },
        }


class ToolRegistry:
    """工具注册中心"""

    def __init__(self):
        self._tools: dict[str, ToolDefinition] = {}

    def register(
        self,
        name: str,
        description: str,
        parameters: dict,
        handler: Callable[..., Awaitable[str]],
    ):
        tool = ToolDefinition(name, description, parameters, handler)
        self._tools[name] = tool
        logger.info("注册工具: %s", name)

    def get_tool(self, name: str) -> Optional[ToolDefinition]:
        return self._tools.get(name)

    async def execute(self, name: str, arguments: str) -> str:
        tool = self._tools.get(name)
        if not tool:
            return f"错误: 未知工具 '{name}'"

        try:
            args = json.loads(arguments) if arguments else {}
            result = await tool.handler(**args)
            return result
        except json.JSONDecodeError:
            return f"错误: 参数格式无效 - {arguments}"
        except Exception as e:
            logger.exception("工具 %s 执行失败", name)
            return f"工具执行失败: {e}"

    def get_openai_tools(self) -> list[dict]:
        return [t.to_openai_tool() for t in self._tools.values()]

    def list_tools(self) -> list[str]:
        return list(self._tools.keys())

    def get_tool_info(self) -> list[dict]:
        return [
            {
                "name": t.name,
                "description": t.description,
                "parameters": t.parameters,
            }
            for t in self._tools.values()
        ]


registry = ToolRegistry()
