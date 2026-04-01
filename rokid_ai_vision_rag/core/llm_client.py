"""LLM 客户端 - 统一封装 DashScope / OpenAI / MiniMax 调用"""

from __future__ import annotations

import base64
import json
import logging
from pathlib import Path
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


class LLMClient:
    """统一 LLM 客户端，支持 DashScope、OpenAI、MiniMax"""

    def __init__(self):
        self.backend = settings.LLM_BACKEND
        self._http_client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._http_client is None or self._http_client.is_closed:
            self._http_client = httpx.AsyncClient(timeout=60.0)
        return self._http_client

    async def close(self):
        if self._http_client and not self._http_client.is_closed:
            await self._http_client.aclose()

    def _encode_image(self, image_path: str) -> str:
        with open(image_path, "rb") as f:
            return base64.b64encode(f.read()).decode("utf-8")

    async def analyze_image(
        self,
        image_path: str,
        prompt: str,
        system_prompt: str = "",
    ) -> str:
        if self.backend == LLMBackend.DASHSCOPE:
            return await self._dashscope_vision(
                image_path, prompt, system_prompt
            )
        if self.backend == LLMBackend.MINIMAX:
            return await self._minimax_vision(
                image_path, prompt, system_prompt
            )
        return await self._openai_vision(image_path, prompt, system_prompt)

    async def chat(
        self,
        prompt: str,
        system_prompt: str = "",
    ) -> str:
        if self.backend == LLMBackend.DASHSCOPE:
            return await self._dashscope_chat(prompt, system_prompt)
        if self.backend == LLMBackend.MINIMAX:
            return await self._minimax_chat(prompt, system_prompt)
        return await self._openai_chat(prompt, system_prompt)

    async def chat_stream(
        self,
        prompt: str,
        system_prompt: str = "",
    ) -> AsyncGenerator[str, None]:
        if self.backend == LLMBackend.DASHSCOPE:
            async for chunk in self._dashscope_chat_stream(
                prompt, system_prompt
            ):
                yield chunk
        elif self.backend == LLMBackend.MINIMAX:
            async for chunk in self._minimax_chat_stream(
                prompt, system_prompt
            ):
                yield chunk
        else:
            async for chunk in self._openai_chat_stream(
                prompt, system_prompt
            ):
                yield chunk

    async def get_embeddings(self, texts: list[str]) -> list[list[float]]:
        if self.backend == LLMBackend.DASHSCOPE:
            return await self._dashscope_embeddings(texts)
        if self.backend == LLMBackend.MINIMAX:
            return await self._minimax_embeddings(texts)
        return await self._openai_embeddings(texts)

    # ---- DashScope 实现 ----

    async def _dashscope_vision(
        self, image_path: str, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        image_b64 = self._encode_image(image_path)

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({
            "role": "user",
            "content": [
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{image_b64}"
                    },
                },
                {"type": "text", "text": prompt},
            ],
        })

        resp = await client.post(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.DASHSCOPE_VL_MODEL,
                "messages": messages,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _dashscope_chat(
        self, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        resp = await client.post(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.DASHSCOPE_TEXT_MODEL,
                "messages": messages,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _dashscope_chat_stream(
        self, prompt: str, system_prompt: str
    ) -> AsyncGenerator[str, None]:
        client = await self._get_client()
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        async with client.stream(
            "POST",
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.DASHSCOPE_TEXT_MODEL,
                "messages": messages,
                "stream": True,
            },
        ) as resp:
            async for line in resp.aiter_lines():
                if not line.startswith("data: "):
                    continue
                payload = line[6:]
                if payload.strip() == "[DONE]":
                    break
                import json

                chunk = json.loads(payload)
                delta = chunk["choices"][0].get("delta", {})
                if content := delta.get("content"):
                    yield content

    async def _dashscope_embeddings(
        self, texts: list[str]
    ) -> list[list[float]]:
        client = await self._get_client()
        resp = await client.post(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings",
            headers={
                "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.DASHSCOPE_EMBEDDING_MODEL,
                "input": texts,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return [item["embedding"] for item in data["data"]]

    # ---- MiniMax 实现（chatcompletion_v2 + 可选 embo-01 向量）----

    def _minimax_chat_url(self) -> str:
        return f"{settings.MINIMAX_BASE_URL.rstrip('/')}/v1/text/chatcompletion_v2"

    async def _minimax_vision(
        self, image_path: str, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        image_b64 = self._encode_image(image_path)
        url = f"data:image/jpeg;base64,{image_b64}"
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({
            "role": "user",
            "content": [
                {"type": "text", "text": prompt},
                {"type": "image_url", "image_url": {"url": url}},
            ],
        })
        resp = await client.post(
            self._minimax_chat_url(),
            headers=_minimax_headers(),
            json={
                "model": settings.MINIMAX_VL_MODEL,
                "messages": messages,
                "max_completion_tokens": 1024,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("base_resp", {}).get("status_code", 0) not in (0, None):
            raise RuntimeError(
                data.get("base_resp", {}).get("status_msg", str(data))
            )
        return data["choices"][0]["message"]["content"]

    async def _minimax_chat(
        self, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        resp = await client.post(
            self._minimax_chat_url(),
            headers=_minimax_headers(),
            json={
                "model": settings.MINIMAX_MODEL,
                "messages": messages,
                "max_completion_tokens": 2048,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("base_resp", {}).get("status_code", 0) not in (0, None):
            raise RuntimeError(
                data.get("base_resp", {}).get("status_msg", str(data))
            )
        return data["choices"][0]["message"]["content"]

    async def _minimax_chat_stream(
        self, prompt: str, system_prompt: str
    ) -> AsyncGenerator[str, None]:
        client = await self._get_client()
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        async with client.stream(
            "POST",
            self._minimax_chat_url(),
            headers=_minimax_headers(),
            json={
                "model": settings.MINIMAX_MODEL,
                "messages": messages,
                "stream": True,
                "max_completion_tokens": 2048,
            },
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

    async def _minimax_embeddings(
        self, texts: list[str]
    ) -> list[list[float]]:
        """MiniMax 向量：优先 OpenAI 风格；失败则尝试 api.minimax.chat + Group-Id。"""
        client = await self._get_client()
        base = settings.MINIMAX_BASE_URL.rstrip("/")
        for ep in (
            f"{base}/v1/embeddings",
            "https://api.minimax.chat/v1/embeddings",
        ):
            try:
                headers = _minimax_headers()
                resp = await client.post(
                    ep,
                    headers=headers,
                    json={
                        "model": settings.MINIMAX_EMBEDDING_MODEL,
                        "input": texts,
                    },
                )
                if resp.status_code != 200:
                    resp = await client.post(
                        ep,
                        headers=headers,
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
                logger.debug("MiniMax embedding %s: %s", ep, e)
                continue
        raise RuntimeError(
            "MiniMax Embedding 调用失败。请配置 MINIMAX_GROUP_ID，"
            "或改用 LLM_BACKEND=dashscope/openai 做向量，"
            "详见 README。"
        )

    # ---- OpenAI 实现 ----

    async def _openai_vision(
        self, image_path: str, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        image_b64 = self._encode_image(image_path)
        base_url = (
            settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
        )

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({
            "role": "user",
            "content": [
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{image_b64}"
                    },
                },
                {"type": "text", "text": prompt},
            ],
        })

        resp = await client.post(
            f"{base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.OPENAI_VL_MODEL,
                "messages": messages,
                "max_tokens": 500,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _openai_chat(
        self, prompt: str, system_prompt: str
    ) -> str:
        client = await self._get_client()
        base_url = (
            settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
        )
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        resp = await client.post(
            f"{base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.OPENAI_TEXT_MODEL,
                "messages": messages,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    async def _openai_chat_stream(
        self, prompt: str, system_prompt: str
    ) -> AsyncGenerator[str, None]:
        client = await self._get_client()
        base_url = (
            settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
        )
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        async with client.stream(
            "POST",
            f"{base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.OPENAI_TEXT_MODEL,
                "messages": messages,
                "stream": True,
            },
        ) as resp:
            async for line in resp.aiter_lines():
                if not line.startswith("data: "):
                    continue
                payload = line[6:]
                if payload.strip() == "[DONE]":
                    break
                import json

                chunk = json.loads(payload)
                delta = chunk["choices"][0].get("delta", {})
                if content := delta.get("content"):
                    yield content

    async def _openai_embeddings(
        self, texts: list[str]
    ) -> list[list[float]]:
        client = await self._get_client()
        base_url = (
            settings.OPENAI_BASE_URL or "https://api.openai.com/v1"
        )
        resp = await client.post(
            f"{base_url}/embeddings",
            headers={
                "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": settings.OPENAI_EMBEDDING_MODEL,
                "input": texts,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        return [item["embedding"] for item in data["data"]]


llm_client = LLMClient()
