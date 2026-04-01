"""视觉分析模块 - 调用多模态 LLM 分析眼镜摄像头图像"""

import logging
from pathlib import Path

from core.llm_client import llm_client
from core.prompt_templates import VISION_SYSTEM_PROMPT, SCENE_PROMPTS

logger = logging.getLogger(__name__)


class VisionAnalyzer:
    """多模态视觉分析器"""

    async def analyze(
        self,
        image_path: str,
        question: str = "请描述这张图片的内容",
        scene_type: str = "general",
    ) -> str:
        path = Path(image_path)
        if not path.exists():
            raise FileNotFoundError(f"图片不存在: {image_path}")

        scene_prompt = SCENE_PROMPTS.get(scene_type, SCENE_PROMPTS["general"])
        full_prompt = f"{scene_prompt}\n\n用户提问: {question}"

        logger.info(
            "分析图片: %s, 场景: %s", path.name, scene_type
        )
        result = await llm_client.analyze_image(
            image_path=str(path),
            prompt=full_prompt,
            system_prompt=VISION_SYSTEM_PROMPT,
        )
        logger.info("视觉分析完成, 结果长度: %d", len(result))
        return result


vision_analyzer = VisionAnalyzer()
