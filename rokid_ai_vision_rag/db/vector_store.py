"""向量数据库模块 - 基于 ChromaDB 的知识存储与检索"""

from __future__ import annotations

import logging
import uuid
from typing import Optional

import chromadb
from chromadb.config import Settings as ChromaSettings

from config import settings
from core.llm_client import llm_client

logger = logging.getLogger(__name__)


class VectorStore:
    """ChromaDB 向量存储，支持 LLM Embedding API"""

    def __init__(self):
        self._client: Optional[chromadb.ClientAPI] = None
        self._collection = None

    def _get_client(self) -> chromadb.ClientAPI:
        if self._client is None:
            self._client = chromadb.PersistentClient(
                path=settings.CHROMA_PERSIST_DIR,
                settings=ChromaSettings(anonymized_telemetry=False),
            )
        return self._client

    def _get_collection(self):
        if self._collection is None:
            client = self._get_client()
            self._collection = client.get_or_create_collection(
                name=settings.CHROMA_COLLECTION,
                metadata={"hnsw:space": "cosine"},
            )
        return self._collection

    async def add_document(
        self,
        content: str,
        metadata: Optional[dict] = None,
        doc_id: Optional[str] = None,
    ) -> str:
        collection = self._get_collection()
        doc_id = doc_id or str(uuid.uuid4())

        embeddings = await llm_client.get_embeddings([content])

        collection.add(
            ids=[doc_id],
            embeddings=embeddings,
            documents=[content],
            metadatas=[metadata or {}],
        )
        logger.info("添加文档: %s (长度: %d)", doc_id, len(content))
        return doc_id

    async def add_documents(
        self,
        contents: list[str],
        metadatas: Optional[list[dict]] = None,
    ) -> list[str]:
        if not contents:
            return []

        collection = self._get_collection()
        ids = [str(uuid.uuid4()) for _ in contents]

        batch_size = 20
        all_embeddings = []
        for i in range(0, len(contents), batch_size):
            batch = contents[i : i + batch_size]
            embs = await llm_client.get_embeddings(batch)
            all_embeddings.extend(embs)

        collection.add(
            ids=ids,
            embeddings=all_embeddings,
            documents=contents,
            metadatas=metadatas or [{} for _ in contents],
        )
        logger.info("批量添加 %d 条文档", len(contents))
        return ids

    async def search(
        self,
        query: str,
        top_k: int = 5,
        where: Optional[dict] = None,
    ) -> list[dict]:
        collection = self._get_collection()

        if collection.count() == 0:
            return []

        query_embedding = await llm_client.get_embeddings([query])

        params = {
            "query_embeddings": query_embedding,
            "n_results": min(top_k, collection.count()),
        }
        if where:
            params["where"] = where

        results = collection.query(**params)

        docs = []
        for i in range(len(results["ids"][0])):
            distance = results["distances"][0][i] if results["distances"] else 1.0
            score = 1 - distance
            docs.append({
                "id": results["ids"][0][i],
                "content": results["documents"][0][i],
                "metadata": results["metadatas"][0][i] if results["metadatas"] else {},
                "score": round(score, 4),
            })

        docs.sort(key=lambda x: x["score"], reverse=True)
        return docs

    def count(self) -> int:
        return self._get_collection().count()

    def delete(self, doc_id: str):
        self._get_collection().delete(ids=[doc_id])

    def clear(self):
        client = self._get_client()
        try:
            client.delete_collection(settings.CHROMA_COLLECTION)
        except Exception:
            pass
        self._collection = None


vector_store = VectorStore()
