"""翻译工具 - 多语言翻译"""

import logging

import httpx

from config import settings, LLMBackend

logger = logging.getLogger(__name__)


async def translate(
    text: str,
    target_language: str = "中文",
    source_language: str = "auto",
) -> str:
    """翻译文本到目标语言

    Args:
        text: 待翻译文本
        target_language: 目标语言（如：中文、英文、日文、韩文、法文等）
        source_language: 源语言（auto 表示自动检测）
    """
    source_hint = (
        f"从{source_language}" if source_language != "auto" else ""
    )
    prompt = (
        f"请将以下文本{source_hint}翻译成{target_language}，只返回翻译结果：\n\n{text}"
    )

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            if settings.LLM_BACKEND == LLMBackend.DASHSCOPE:
                url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                headers = {
                    "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                    "Content-Type": "application/json",
                }
                model = settings.DASHSCOPE_MODEL
            elif settings.LLM_BACKEND == LLMBackend.MINIMAX:
                url = (
                    f"{settings.MINIMAX_BASE_URL.rstrip('/')}"
                    "/v1/text/chatcompletion_v2"
                )
                headers = {
                    "Authorization": f"Bearer {settings.MINIMAX_API_KEY}",
                    "Content-Type": "application/json",
                }
                if settings.MINIMAX_GROUP_ID:
                    headers["Group-Id"] = settings.MINIMAX_GROUP_ID
                model = settings.MINIMAX_MODEL
            else:
                url = (
                    settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
                ) + "/chat/completions"
                headers = {
                    "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                    "Content-Type": "application/json",
                }
                model = settings.OPENAI_MODEL

            body = {
                "model": model,
                "messages": [
                    {
                        "role": "system",
                        "content": "你是专业翻译，只返回翻译结果，不添加解释。",
                    },
                    {"role": "user", "content": prompt},
                ],
            }
            if settings.LLM_BACKEND == LLMBackend.MINIMAX:
                body["max_completion_tokens"] = 1024

            resp = await client.post(url, headers=headers, json=body)
            resp.raise_for_status()
            data = resp.json()
            return data["choices"][0]["message"]["content"]
    except Exception as e:
        logger.exception("翻译失败")
        return f"翻译失败: {e}"


TOOL_DEFINITION = {
    "name": "translate",
    "description": "多语言翻译工具，支持中文、英文、日文、韩文、法文等主流语言互译",
    "parameters": {
        "type": "object",
        "properties": {
            "text": {
                "type": "string",
                "description": "待翻译的文本",
            },
            "target_language": {
                "type": "string",
                "description": "目标语言（如：中文、英文、日文）",
                "default": "中文",
            },
            "source_language": {
                "type": "string",
                "description": "源语言，auto表示自动检测",
                "default": "auto",
            },
        },
        "required": ["text"],
    },
    "handler": translate,
}
