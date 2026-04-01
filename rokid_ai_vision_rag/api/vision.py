"""视觉分析 API"""

import tempfile
import logging

from fastapi import APIRouter, UploadFile, File, Form, HTTPException

from core.vision_analyzer import vision_analyzer
from core.rag_pipeline import rag_pipeline
from models.schemas import VisionAnalysisResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/vision", tags=["视觉分析"])


@router.post("/analyze", response_model=VisionAnalysisResponse)
async def analyze_image(
    image: UploadFile = File(..., description="图片文件"),
    question: str = Form(default="请描述这张图片的内容"),
    use_rag: bool = Form(default=True),
    scene_type: str = Form(default="general"),
):
    """分析眼镜摄像头拍摄的图片，结合 RAG 知识增强返回结果"""
    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(400, "请上传图片文件")

    suffix = "." + (image.filename or "img.jpg").rsplit(".", 1)[-1]
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
        content = await image.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        vision_desc = await vision_analyzer.analyze(
            image_path=tmp_path,
            question=question,
            scene_type=scene_type,
        )

        if use_rag:
            rag_result = await rag_pipeline.enhance_with_rag(
                vision_description=vision_desc,
                user_question=question,
            )
            return VisionAnalysisResponse(
                vision_description=vision_desc,
                rag_context=rag_result["rag_context"],
                enhanced_answer=rag_result["enhanced_answer"],
                confidence=rag_result["confidence"],
                sources=rag_result["sources"],
            )

        return VisionAnalysisResponse(
            vision_description=vision_desc,
            enhanced_answer=vision_desc,
            confidence=1.0,
        )
    except Exception as e:
        logger.exception("图片分析失败")
        raise HTTPException(500, f"分析失败: {e}")
    finally:
        import os
        os.unlink(tmp_path)


@router.post("/analyze-local", response_model=VisionAnalysisResponse)
async def analyze_local_image(
    image_path: str = Form(..., description="本地图片路径"),
    question: str = Form(default="请描述这张图片的内容"),
    use_rag: bool = Form(default=True),
    scene_type: str = Form(default="general"),
):
    """分析本地图片（用于 ADB 抓取的眼镜照片）"""
    import os
    if not os.path.exists(image_path):
        raise HTTPException(404, f"文件不存在: {image_path}")

    vision_desc = await vision_analyzer.analyze(
        image_path=image_path,
        question=question,
        scene_type=scene_type,
    )

    if use_rag:
        rag_result = await rag_pipeline.enhance_with_rag(
            vision_description=vision_desc,
            user_question=question,
        )
        return VisionAnalysisResponse(
            vision_description=vision_desc,
            rag_context=rag_result["rag_context"],
            enhanced_answer=rag_result["enhanced_answer"],
            confidence=rag_result["confidence"],
            sources=rag_result["sources"],
        )

    return VisionAnalysisResponse(
        vision_description=vision_desc,
        enhanced_answer=vision_desc,
        confidence=1.0,
    )
