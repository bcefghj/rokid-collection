"""对话记忆模块 - 基于 FAISS 的向量化对话记忆"""

from __future__ import annotations

import json
import logging
import os
from datetime import datetime
from typing import Optional
from collections import defaultdict

import numpy as np

from config import settings
from core.llm_client import llm_client

logger = logging.getLogger(__name__)


class ConversationMemory:
    """基于 FAISS 的对话记忆管理器"""

    def __init__(self):
        self._sessions: dict[str, list[dict]] = defaultdict(list)
        self._faiss_index = None
        self._memory_texts: list[str] = []
        self._memory_meta: list[dict] = []
        self._initialized = False

    def _ensure_faiss(self):
        if self._faiss_index is not None:
            return
        try:
            import faiss
            self._faiss_index = faiss.IndexFlatIP(settings.EMBEDDING_DIM)
            logger.info("FAISS 索引初始化完成 (dim=%d)", settings.EMBEDDING_DIM)
        except ImportError:
            logger.warning("FAISS 未安装，记忆检索功能降级为文本匹配")
            self._faiss_index = None

    def add_message(
        self, session_id: str, role: str, content: str
    ):
        msg = {
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat(),
        }
        self._sessions[session_id].append(msg)

        max_items = settings.MAX_MEMORY_ITEMS
        if len(self._sessions[session_id]) > max_items:
            self._sessions[session_id] = self._sessions[session_id][-max_items:]

    def get_history(
        self, session_id: str, last_n: int = 10
    ) -> list[dict]:
        messages = self._sessions.get(session_id, [])
        return messages[-last_n:]

    def get_history_as_messages(
        self, session_id: str, last_n: int = 10
    ) -> list[dict]:
        """返回适合 LLM API 的 messages 格式"""
        history = self.get_history(session_id, last_n)
        return [{"role": m["role"], "content": m["content"]} for m in history]

    async def search_memory(
        self, query: str, top_k: int = 3
    ) -> list[dict]:
        self._ensure_faiss()
        if self._faiss_index is None or len(self._memory_texts) == 0:
            return []

        query_emb = await llm_client.get_embeddings([query])
        query_vec = np.array(query_emb, dtype=np.float32)

        k = min(top_k, len(self._memory_texts))
        scores, indices = self._faiss_index.search(query_vec, k)

        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx < 0:
                continue
            results.append({
                "text": self._memory_texts[idx],
                "metadata": self._memory_meta[idx],
                "score": float(score),
            })
        return results

    async def store_to_long_term(
        self, text: str, metadata: Optional[dict] = None
    ):
        self._ensure_faiss()
        if self._faiss_index is None:
            return

        emb = await llm_client.get_embeddings([text])
        vec = np.array(emb, dtype=np.float32)

        self._faiss_index.add(vec)
        self._memory_texts.append(text)
        self._memory_meta.append(metadata or {})

    def get_session_info(self, session_id: str) -> dict:
        msgs = self._sessions.get(session_id, [])
        if not msgs:
            return {
                "session_id": session_id,
                "message_count": 0,
                "created_at": "",
                "last_active": "",
            }
        return {
            "session_id": session_id,
            "message_count": len(msgs),
            "created_at": msgs[0]["timestamp"],
            "last_active": msgs[-1]["timestamp"],
        }

    def list_sessions(self) -> list[str]:
        return list(self._sessions.keys())

    def clear_session(self, session_id: str):
        self._sessions.pop(session_id, None)


memory = ConversationMemory()
