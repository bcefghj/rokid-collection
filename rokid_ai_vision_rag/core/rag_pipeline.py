"""RAG 管道 - 检索增强生成，结合视觉分析与知识库"""

from __future__ import annotations

import logging

from core.llm_client import llm_client
from core.prompt_templates import RAG_ENHANCEMENT_PROMPT, VISION_SYSTEM_PROMPT
from db.vector_store import vector_store
from config import settings

logger = logging.getLogger(__name__)


class RAGPipeline:
    """RAG 检索增强生成管道"""

    async def retrieve(
        self,
        query: str,
        top_k: int = 0,
        category: str = "",
    ) -> list[dict]:
        k = top_k or settings.RAG_TOP_K
        where = {"category": category} if category else None
        results = await vector_store.search(query, top_k=k, where=where)

        filtered = [
            r for r in results if r["score"] >= settings.RAG_SCORE_THRESHOLD
        ]
        logger.info(
            "检索 '%s': %d 条结果 (过滤后 %d 条)",
            query[:50],
            len(results),
            len(filtered),
        )
        return filtered

    def _format_context(self, results: list[dict]) -> str:
        if not results:
            return "（未检索到相关知识）"

        parts = []
        for i, r in enumerate(results, 1):
            title = r["metadata"].get("title", "未命名")
            source = r["metadata"].get("source", "unknown")
            parts.append(
                f"[{i}] {title} (来源: {source}, 相关度: {r['score']:.2f})\n"
                f"{r['content']}"
            )
        return "\n\n".join(parts)

    async def enhance_with_rag(
        self,
        vision_description: str,
        user_question: str,
        category: str = "",
    ) -> dict:
        search_query = f"{user_question} {vision_description[:200]}"
        results = await self.retrieve(search_query, category=category)

        rag_context = self._format_context(results)

        prompt = RAG_ENHANCEMENT_PROMPT.format(
            vision_description=vision_description,
            rag_context=rag_context,
            user_question=user_question,
        )

        enhanced = await llm_client.chat(
            prompt=prompt, system_prompt=VISION_SYSTEM_PROMPT
        )

        sources = [
            r["metadata"].get("title", r["id"]) for r in results
        ]

        avg_score = (
            sum(r["score"] for r in results) / len(results)
            if results
            else 0.0
        )

        return {
            "enhanced_answer": enhanced,
            "rag_context": rag_context if results else None,
            "sources": sources,
            "confidence": round(avg_score, 4),
        }


rag_pipeline = RAGPipeline()
