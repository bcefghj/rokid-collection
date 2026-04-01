"""翻译工具：利用 LLM 进行多语言翻译，适配 AR 眼镜的实时翻译场景"""

from langchain_core.tools import tool
from langchain_openai import ChatOpenAI
from app.config import get_settings


@tool
def translate(text: str, target_lang: str = "英文") -> str:
    """将文本翻译成目标语言。

    Args:
        text: 需要翻译的文本
        target_lang: 目标语言，如"英文"、"日文"、"中文"等
    """
    settings = get_settings()
    try:
        llm = ChatOpenAI(
            model=settings.model_name,
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            temperature=0,
        )
        result = llm.invoke(
            f"请将以下文本翻译成{target_lang}，只输出翻译结果，不要解释：\n\n{text}"
        )
        return result.content
    except Exception as e:
        return f"翻译出错: {str(e)}"
