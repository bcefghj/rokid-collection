"""LLM 客户端 - Agent 专用，支持 DashScope / OpenAI / MiniMax + Function Calling"""

from __future__ import annotations

import json
import logging
from typing import Optional, AsyncGenerator

import httpx

from config import settings, LLMBackend

logger = logging.getLogger(__name__)


def _minimax_headers() -> dict:
    h = {
        "Authorization": f"Bearer {settings.MINIMAX_API_KEY}",
        "Content-Type": "application/json",
    }
    if settings.MINIMAX_GROUP_ID:
        h["Group-Id"] = settings.MINIMAX_GROUP_ID
    return h


class AgentLLMClient:
    """支持工具调用的 LLM 客户端"""

    def __init__(self):
        self.backend = settings.LLM_BACKEND
        self._client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(timeout=120.0)
        return self._client

    async def close(self):
        if self._client and not getattr(self._client, "is_closed", True):
            await self._client.aclose()

    def _minimax_url(self) -> str:
        return (
            f"{settings.MINIMAX_BASE_URL.rstrip('/')}"
            "/v1/text/chatcompletion_v2"
        )

    def _api_url(self) -> str:
        if self.backend == LLMBackend.DASHSCOPE:
            return (
                "https://dashscope.aliyuncs.com/compatible-mode/v1/"
                "chat/completions"
            )
        if self.backend == LLMBackend.MINIMAX:
            return self._minimax_url()
        return (
            settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
        ) + "/chat/completions"

    def _api_key(self) -> str:
        if self.backend == LLMBackend.DASHSCOPE:
            return settings.DASHSCOPE_API_KEY
        if self.backend == LLMBackend.MINIMAX:
            return settings.MINIMAX_API_KEY
        return settings.OPENAI_API_KEY

    def _headers(self) -> dict:
        if self.backend == LLMBackend.MINIMAX:
            return _minimax_headers()
        return {
            "Authorization": f"Bearer {self._api_key()}",
            "Content-Type": "application/json",
        }

    def _model(self) -> str:
        if self.backend == LLMBackend.DASHSCOPE:
            return settings.DASHSCOPE_MODEL
        if self.backend == LLMBackend.MINIMAX:
            return settings.MINIMAX_MODEL
        return settings.OPENAI_MODEL

    async def chat_with_tools(
        self,
        messages: list[dict],
        tools: list[dict],
    ) -> dict:
        client = await self._get_client()
        body: dict = {
            "model": self._model(),
            "messages": messages,
            "max_completion_tokens": 2048,
        }
        if tools:
            body["tools"] = tools
            body["tool_choice"] = "auto"

        resp = await client.post(
            self._api_url(),
            headers=self._headers(),
            json=body,
        )
        resp.raise_for_status()
        data = resp.json()
        if self.backend == LLMBackend.MINIMAX:
            br = data.get("base_resp") or {}
            if br.get("status_code") not in (0, None):
                raise RuntimeError(br.get("status_msg", str(data)))
        return data["choices"][0]

    async def chat(self, messages: list[dict]) -> str:
        client = await self._get_client()
        body = {
            "model": self._model(),
            "messages": messages,
            "max_completion_tokens": 2048,
        }
        resp = await client.post(
            self._api_url(),
            headers=self._headers(),
            json=body,
        )
        resp.raise_for_status()
        data = resp.json()
        if self.backend == LLMBackend.MINIMAX:
            br = data.get("base_resp") or {}
            if br.get("status_code") not in (0, None):
                raise RuntimeError(br.get("status_msg", str(data)))
        return data["choices"][0]["message"]["content"]

    async def chat_stream(
        self, messages: list[dict]
    ) -> AsyncGenerator[str, None]:
        client = await self._get_client()
        body = {
            "model": self._model(),
            "messages": messages,
            "stream": True,
            "max_completion_tokens": 2048,
        }
        async with client.stream(
            "POST",
            self._api_url(),
            headers=self._headers(),
            json=body,
        ) as resp:
            async for line in resp.aiter_lines():
                if not line:
                    continue
                if line.startswith("data: "):
                    line = line[6:]
                if line.strip() in ("[DONE]", ""):
                    continue
                try:
                    chunk = json.loads(line)
                except json.JSONDecodeError:
                    continue
                ch0 = chunk.get("choices", [{}])[0]
                delta = ch0.get("delta") or {}
                if content := delta.get("content"):
                    yield content

    async def get_embeddings(self, texts: list[str]) -> list[list[float]]:
        client = await self._get_client()
        if self.backend == LLMBackend.DASHSCOPE:
            url = (
                "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
            )
            model = settings.DASHSCOPE_EMBEDDING_MODEL
            key_header = {
                "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                "Content-Type": "application/json",
            }
            resp = await client.post(
                url,
                headers=key_header,
                json={"model": model, "input": texts},
            )
        elif self.backend == LLMBackend.MINIMAX:
            base = settings.MINIMAX_BASE_URL.rstrip("/")
            last_err = None
            for ep in (
                f"{base}/v1/embeddings",
                "https://api.minimax.chat/v1/embeddings",
            ):
                try:
                    resp = await client.post(
                        ep,
                        headers=_minimax_headers(),
                        json={
                            "model": settings.MINIMAX_EMBEDDING_MODEL,
                            "input": texts,
                        },
                    )
                    if resp.status_code != 200:
                        resp = await client.post(
                            ep,
                            headers=_minimax_headers(),
                            json={
                                "model": settings.MINIMAX_EMBEDDING_MODEL,
                                "texts": texts,
                            },
                        )
                    resp.raise_for_status()
                    data = resp.json()
                    if "data" in data:
                        return [item["embedding"] for item in data["data"]]
                    vecs = data.get("vectors") or data.get("embeddings")
                    if vecs:
                        return vecs
                except Exception as e:
                    last_err = e
                    continue
            raise RuntimeError(
                f"MiniMax Embedding 失败: {last_err}。可设置 MINIMAX_GROUP_ID 或换后端。"
            )
        else:
            base = settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
            url = f"{base}/embeddings"
            model = settings.OPENAI_EMBEDDING_MODEL
            resp = await client.post(
                url,
                headers={
                    "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                    "Content-Type": "application/json",
                },
                json={"model": model, "input": texts},
            )
        resp.raise_for_status()
        data = resp.json()
        return [item["embedding"] for item in data["data"]]


llm_client = AgentLLMClient()
