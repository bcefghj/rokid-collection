"""知识库管理 API"""

import json
import logging
from pathlib import Path

from fastapi import APIRouter, HTTPException

from db.vector_store import vector_store
from models.schemas import (
    KnowledgeAddRequest,
    KnowledgeAddResponse,
    KnowledgeBatchAddRequest,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/knowledge", tags=["知识库管理"])


@router.post("/add", response_model=KnowledgeAddResponse)
async def add_knowledge(req: KnowledgeAddRequest):
    """添加单条知识"""
    content = f"{req.title}\n{req.content}"
    metadata = {
        "title": req.title,
        "category": req.category,
        "tags": ",".join(req.tags),
        "source": req.source,
    }
    doc_id = await vector_store.add_document(content, metadata)
    return KnowledgeAddResponse(id=doc_id, message="知识添加成功")


@router.post("/batch-add")
async def batch_add_knowledge(req: KnowledgeBatchAddRequest):
    """批量添加知识"""
    contents = [f"{item.title}\n{item.content}" for item in req.items]
    metadatas = [
        {
            "title": item.title,
            "category": item.category,
            "tags": ",".join(item.tags),
            "source": item.source,
        }
        for item in req.items
    ]
    ids = await vector_store.add_documents(contents, metadatas)
    return {"ids": ids, "count": len(ids), "message": "批量添加成功"}


@router.post("/load-sample")
async def load_sample_knowledge():
    """加载示例知识库"""
    sample_path = Path(__file__).parent.parent / "knowledge_base" / "sample_data.json"
    if not sample_path.exists():
        raise HTTPException(404, "示例数据文件不存在")

    with open(sample_path, "r", encoding="utf-8") as f:
        items = json.load(f)

    contents = [f"{item['title']}\n{item['content']}" for item in items]
    metadatas = [
        {
            "title": item["title"],
            "category": item.get("category", "general"),
            "tags": ",".join(item.get("tags", [])),
            "source": item.get("source", "sample"),
        }
        for item in items
    ]

    ids = await vector_store.add_documents(contents, metadatas)
    return {
        "ids": ids,
        "count": len(ids),
        "message": f"成功加载 {len(ids)} 条示例知识",
    }


@router.get("/count")
async def get_count():
    """获取知识库条目数"""
    return {"count": vector_store.count()}


@router.delete("/{doc_id}")
async def delete_knowledge(doc_id: str):
    """删除知识条目"""
    vector_store.delete(doc_id)
    return {"message": f"已删除: {doc_id}"}


@router.delete("/")
async def clear_knowledge():
    """清空知识库"""
    vector_store.clear()
    return {"message": "知识库已清空"}
