"""网络搜索工具 - 通过 DashScope/OpenAI 实现联网搜索"""

import logging

import httpx

from config import settings, LLMBackend

logger = logging.getLogger(__name__)


async def web_search(query: str, max_results: int = 3) -> str:
    """搜索互联网获取最新信息

    Args:
        query: 搜索关键词
        max_results: 最大结果数
    """
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            if settings.LLM_BACKEND == LLMBackend.DASHSCOPE:
                resp = await client.post(
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                    headers={
                        "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": settings.DASHSCOPE_MODEL,
                        "messages": [
                            {
                                "role": "system",
                                "content": "你是一个搜索助手。请根据用户的搜索请求，提供准确、最新的信息。",
                            },
                            {
                                "role": "user",
                                "content": f"请搜索并总结以下内容的最新信息：{query}",
                            },
                        ],
                        "enable_search": True,
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"]
            if settings.LLM_BACKEND == LLMBackend.MINIMAX:
                h = {
                    "Authorization": f"Bearer {settings.MINIMAX_API_KEY}",
                    "Content-Type": "application/json",
                }
                if settings.MINIMAX_GROUP_ID:
                    h["Group-Id"] = settings.MINIMAX_GROUP_ID
                resp = await client.post(
                    f"{settings.MINIMAX_BASE_URL.rstrip('/')}"
                    "/v1/text/chatcompletion_v2",
                    headers=h,
                    json={
                        "model": settings.MINIMAX_MODEL,
                        "messages": [
                            {
                                "role": "system",
                                "content": "你是搜索助手，请根据用户问题给出最新、准确的简要信息。",
                            },
                            {
                                "role": "user",
                                "content": f"关于「{query}」的最新信息是什么？请简要回答。",
                            },
                        ],
                        "max_completion_tokens": 1024,
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"]
            else:
                resp = await client.post(
                    (settings.OPENAI_BASE_URL or "https://api.openai.com/v1")
                    + "/chat/completions",
                    headers={
                        "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": settings.OPENAI_MODEL,
                        "messages": [
                            {
                                "role": "system",
                                "content": "你是搜索助手，提供准确信息。",
                            },
                            {
                                "role": "user",
                                "content": f"关于 '{query}' 的最新信息是什么？请简要回答。",
                            },
                        ],
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"]
    except Exception as e:
        logger.exception("搜索失败")
        return f"搜索失败: {e}"


TOOL_DEFINITION = {
    "name": "web_search",
    "description": "搜索互联网获取最新信息，适用于需要实时数据、新闻、天气、百科知识等场景",
    "parameters": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "搜索关键词",
            },
            "max_results": {
                "type": "integer",
                "description": "最大结果数",
                "default": 3,
            },
        },
        "required": ["query"],
    },
    "handler": web_search,
}
