"""RAG 知识库：基于 ChromaDB 的向量检索增强，为 Agent 提供领域知识"""

import os
import chromadb
from langchain_core.tools import tool
from app.config import get_settings

_client = None
_collection = None


def _get_collection():
    global _client, _collection
    if _collection is None:
        settings = get_settings()
        persist_dir = settings.chroma_persist_dir
        os.makedirs(persist_dir, exist_ok=True)
        _client = chromadb.PersistentClient(path=persist_dir)
        _collection = _client.get_or_create_collection(
            name="knowledge_base",
            metadata={"hnsw:space": "cosine"},
        )
    return _collection


def add_documents(texts: list[str], metadatas: list[dict] | None = None) -> int:
    """向知识库批量添加文档"""
    collection = _get_collection()
    ids = [f"doc_{collection.count() + i}" for i in range(len(texts))]
    collection.add(documents=texts, ids=ids, metadatas=metadatas)
    return len(ids)


def query_knowledge(query: str, n_results: int = 3) -> list[str]:
    """检索知识库"""
    collection = _get_collection()
    if collection.count() == 0:
        return []
    results = collection.query(query_texts=[query], n_results=min(n_results, collection.count()))
    return results.get("documents", [[]])[0]


@tool
def search_knowledge_base(query: str) -> str:
    """从本地知识库中检索相关信息。当用户询问的问题可能与已存储的文档知识相关时使用。

    Args:
        query: 检索关键词或问题
    """
    docs = query_knowledge(query, n_results=3)
    if not docs:
        return "知识库为空，暂无相关信息。"
    result = "从知识库中检索到以下相关内容：\n\n"
    for i, doc in enumerate(docs, 1):
        result += f"[{i}] {doc}\n\n"
    return result
