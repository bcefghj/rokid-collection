"""应用配置"""

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
    DASHSCOPE_MODEL: str = os.getenv("DASHSCOPE_MODEL", "qwen-plus")
    DASHSCOPE_VL_MODEL: str = os.getenv("DASHSCOPE_VL_MODEL", "qwen-vl-plus")
    DASHSCOPE_EMBEDDING_MODEL: str = os.getenv(
        "DASHSCOPE_EMBEDDING_MODEL", "text-embedding-v3"
    )

    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_BASE_URL: str = os.getenv("OPENAI_BASE_URL", "")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    OPENAI_EMBEDDING_MODEL: str = os.getenv(
        "OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"
    )

    MEMORY_PERSIST_DIR: str = os.getenv("MEMORY_PERSIST_DIR", "./agent_memory")
    EMBEDDING_DIM: int = int(os.getenv("EMBEDDING_DIM", "1024"))

    SERVER_HOST: str = os.getenv("SERVER_HOST", "0.0.0.0")
    SERVER_PORT: int = int(os.getenv("SERVER_PORT", "8001"))

    MAX_AGENT_ITERATIONS: int = int(os.getenv("MAX_AGENT_ITERATIONS", "8"))
    MAX_MEMORY_ITEMS: int = int(os.getenv("MAX_MEMORY_ITEMS", "50"))


settings = Settings()
