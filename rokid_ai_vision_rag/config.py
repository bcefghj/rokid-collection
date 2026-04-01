"""应用配置模块 - 支持 DashScope / OpenAI 双后端"""

import os
from enum import Enum


class LLMBackend(str, Enum):
    DASHSCOPE = "dashscope"
    OPENAI = "openai"
    MINIMAX = "minimax"


class Settings:
    LLM_BACKEND: LLMBackend = LLMBackend(
        os.getenv("LLM_BACKEND", "dashscope")
    )

    # MiniMax: https://platform.minimax.io （OpenAI 风格对话见 chatcompletion_v2）
    MINIMAX_API_KEY: str = os.getenv("MINIMAX_API_KEY", "")
    MINIMAX_BASE_URL: str = os.getenv(
        "MINIMAX_BASE_URL", "https://api.minimax.io"
    )
    MINIMAX_MODEL: str = os.getenv("MINIMAX_MODEL", "MiniMax-M2.5")
    MINIMAX_VL_MODEL: str = os.getenv(
        "MINIMAX_VL_MODEL", "MiniMax-Text-01"
    )
    MINIMAX_EMBEDDING_MODEL: str = os.getenv(
        "MINIMAX_EMBEDDING_MODEL", "embo-01"
    )
    MINIMAX_GROUP_ID: str = os.getenv("MINIMAX_GROUP_ID", "")

    DASHSCOPE_API_KEY: str = os.getenv("DASHSCOPE_API_KEY", "")
    DASHSCOPE_VL_MODEL: str = os.getenv(
        "DASHSCOPE_VL_MODEL", "qwen-vl-plus"
    )
    DASHSCOPE_TEXT_MODEL: str = os.getenv(
        "DASHSCOPE_TEXT_MODEL", "qwen-plus"
    )
    DASHSCOPE_EMBEDDING_MODEL: str = os.getenv(
        "DASHSCOPE_EMBEDDING_MODEL", "text-embedding-v3"
    )

    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_BASE_URL: str = os.getenv("OPENAI_BASE_URL", "")
    OPENAI_VL_MODEL: str = os.getenv("OPENAI_VL_MODEL", "gpt-4o")
    OPENAI_TEXT_MODEL: str = os.getenv("OPENAI_TEXT_MODEL", "gpt-4o-mini")
    OPENAI_EMBEDDING_MODEL: str = os.getenv(
        "OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"
    )

    CHROMA_PERSIST_DIR: str = os.getenv("CHROMA_PERSIST_DIR", "./chroma_db")
    CHROMA_COLLECTION: str = os.getenv(
        "CHROMA_COLLECTION", "rokid_knowledge"
    )

    RAG_TOP_K: int = int(os.getenv("RAG_TOP_K", "5"))
    RAG_SCORE_THRESHOLD: float = float(
        os.getenv("RAG_SCORE_THRESHOLD", "0.3")
    )

    SERVER_HOST: str = os.getenv("SERVER_HOST", "0.0.0.0")
    SERVER_PORT: int = int(os.getenv("SERVER_PORT", "8000"))

    ADB_DEVICE: str = os.getenv("ADB_DEVICE", "")
    GLASSES_PHOTO_DIR: str = os.getenv(
        "GLASSES_PHOTO_DIR", "/sdcard/DCIM/Camera"
    )


settings = Settings()
