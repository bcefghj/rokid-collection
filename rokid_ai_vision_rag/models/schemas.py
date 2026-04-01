"""Pydantic 数据模型"""

from __future__ import annotations

from typing import Optional
from pydantic import BaseModel, Field


class VisionAnalysisRequest(BaseModel):
    question: str = Field(default="请描述这张图片的内容", description="分析问题")
    use_rag: bool = Field(default=True, description="是否启用RAG知识增强")
    scene_type: str = Field(
        default="general",
        description="场景类型: general/travel/shopping/reading/translate",
    )


class VisionAnalysisResponse(BaseModel):
    vision_description: str = Field(description="视觉分析结果")
    rag_context: Optional[str] = Field(
        default=None, description="RAG检索到的知识上下文"
    )
    enhanced_answer: str = Field(description="知识增强后的最终回答")
    confidence: float = Field(default=0.0, description="置信度")
    sources: list[str] = Field(default_factory=list, description="知识来源")


class RAGQueryRequest(BaseModel):
    query: str = Field(description="查询文本")
    top_k: int = Field(default=5, description="返回结果数量")


class RAGQueryResponse(BaseModel):
    query: str
    results: list[dict] = Field(default_factory=list)
    total: int = 0


class KnowledgeAddRequest(BaseModel):
    title: str = Field(description="知识标题")
    content: str = Field(description="知识内容")
    category: str = Field(default="general", description="分类")
    tags: list[str] = Field(default_factory=list, description="标签")
    source: str = Field(default="manual", description="来源")


class KnowledgeAddResponse(BaseModel):
    id: str
    message: str


class KnowledgeBatchAddRequest(BaseModel):
    items: list[KnowledgeAddRequest]


class HealthResponse(BaseModel):
    status: str
    version: str
    llm_backend: str
    knowledge_count: int
