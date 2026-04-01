"""RAG 检索 API"""

from fastapi import APIRouter, HTTPException

from core.rag_pipeline import rag_pipeline
from models.schemas import RAGQueryRequest, RAGQueryResponse

router = APIRouter(prefix="/api/rag", tags=["RAG检索"])


@router.post("/query", response_model=RAGQueryResponse)
async def query_knowledge(req: RAGQueryRequest):
    """语义检索知识库"""
    results = await rag_pipeline.retrieve(query=req.query, top_k=req.top_k)
    return RAGQueryResponse(
        query=req.query,
        results=results,
        total=len(results),
    )
