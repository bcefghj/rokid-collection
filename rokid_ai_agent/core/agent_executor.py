"""Agent 执行引擎 - ReAct 模式的 Agent 核心"""

from __future__ import annotations

import json
import logging
from typing import Optional

from config import settings
from core.llm_client import llm_client
from core.memory import memory
from core.tool_registry import registry
from models.schemas import ToolCall

logger = logging.getLogger(__name__)

AGENT_SYSTEM_PROMPT = """你是 Rokid AI 眼镜的智能助手 Agent。你可以调用工具来帮助用户完成任务。

你的能力包括：
1. 联网搜索最新信息
2. 多语言翻译
3. 数学计算
4. 查询 Rokid 产品知识库
5. 查询日期时间
6. 控制 Rokid 眼镜硬件（拍照、截图、查电量等）

规则：
- 回答简洁明了，适合 AR 眼镜语音播报（控制在 100 字以内，除非用户要求详细）
- 如果需要最新信息，使用搜索工具
- 需要翻译时使用翻译工具
- 涉及 Rokid 产品信息时先查知识库
- 用中文回答，除非用户要求其他语言
- 如果任务需要多步操作，请逐步执行"""


class AgentExecutor:
    """ReAct Agent 执行器 - 支持多轮工具调用"""

    async def run(
        self,
        user_message: str,
        session_id: str = "default",
        scene_context: str = "",
    ) -> dict:
        memory.add_message(session_id, "user", user_message)

        messages = [{"role": "system", "content": AGENT_SYSTEM_PROMPT}]

        if scene_context:
            messages.append({
                "role": "system",
                "content": f"当前场景上下文: {scene_context}",
            })

        history = memory.get_history_as_messages(session_id, last_n=6)
        messages.extend(history)

        tools = registry.get_openai_tools()
        tool_calls_log: list[ToolCall] = []
        thinking_parts: list[str] = []

        for iteration in range(settings.MAX_AGENT_ITERATIONS):
            logger.info(
                "Agent 迭代 %d/%d (session=%s)",
                iteration + 1,
                settings.MAX_AGENT_ITERATIONS,
                session_id,
            )

            choice = await llm_client.chat_with_tools(messages, tools)
            msg = choice["message"]
            finish_reason = choice.get("finish_reason", "")

            if finish_reason == "tool_calls" or msg.get("tool_calls"):
                messages.append(msg)

                for tc in msg["tool_calls"]:
                    func = tc["function"]
                    tool_name = func["name"]
                    tool_args = func.get("arguments", "{}")

                    logger.info(
                        "调用工具: %s(%s)", tool_name, tool_args[:100]
                    )
                    thinking_parts.append(
                        f"→ 调用 {tool_name}: {tool_args}"
                    )

                    result = await registry.execute(tool_name, tool_args)

                    tool_calls_log.append(
                        ToolCall(
                            tool_name=tool_name,
                            tool_input=tool_args,
                            tool_output=result[:500],
                        )
                    )

                    messages.append({
                        "role": "tool",
                        "tool_call_id": tc["id"],
                        "content": result,
                    })

                    thinking_parts.append(f"  结果: {result[:200]}")
            else:
                final_answer = msg.get("content", "抱歉，我无法处理这个请求。")
                memory.add_message(session_id, "assistant", final_answer)

                await self._store_important_memory(
                    user_message, final_answer, session_id
                )

                return {
                    "answer": final_answer,
                    "session_id": session_id,
                    "tool_calls": tool_calls_log,
                    "thinking": "\n".join(thinking_parts),
                }

        final = await llm_client.chat(messages)
        memory.add_message(session_id, "assistant", final)

        return {
            "answer": final,
            "session_id": session_id,
            "tool_calls": tool_calls_log,
            "thinking": "\n".join(thinking_parts),
        }

    async def _store_important_memory(
        self, question: str, answer: str, session_id: str
    ):
        """将重要对话存入长期记忆"""
        try:
            summary = f"Q: {question[:100]} -> A: {answer[:200]}"
            await memory.store_to_long_term(
                summary,
                {"session_id": session_id, "type": "conversation"},
            )
        except Exception:
            pass


agent_executor = AgentExecutor()
