"""知识查询工具 - 查询内置知识库"""

import logging

logger = logging.getLogger(__name__)

KNOWLEDGE_BASE = {
    "rokid_glasses": {
        "title": "Rokid Glasses 产品信息",
        "content": (
            "Rokid Glasses 是一款 AI+AR 消费级智能眼镜，售价 2499 元。"
            "搭载高通 AR1 芯片，1200 万像素摄像头，支持 4K 录制。"
            "集成通义千问、DeepSeek 等大模型，具备语音唤醒、拍照识物、实时翻译、AR 导航。"
            "重量 38.5 克，续航 12 小时。支持自定义智能体和私有化模型部署。"
        ),
    },
    "rokid_style": {
        "title": "Rokid Style 产品信息",
        "content": (
            "Rokid Style 是 CES 2026 发布的无屏 AI 眼镜。"
            "双芯片设计（NXP RT600 + 高通 AR1），1200 万像素摄像头，4K 录制。"
            "重量 38.5 克，续航 12 小时。专为全天候使用设计。"
        ),
    },
    "ai_glasses_market": {
        "title": "AI 眼镜市场概况",
        "content": (
            "2025 年全球 AI 眼镜销量约 600 万部，2026 年预计 2000 万部。"
            "Meta Ray-Ban 全球占 84%，中国厂商 Rokid、华为、小米、雷鸟占前五中四席。"
            "核心功能：实时翻译、拍照识物、AI 问答、AR 导航、语音助手。"
            "中国市场 2025 年经历'百镜大战'，竞争激烈。"
        ),
    },
    "cxr_protocol": {
        "title": "CXR 通信协议",
        "content": (
            "CXR 是 Rokid 眼镜与手机间的通信协议，支持 BLE GATT 和 RFCOMM。"
            "命令类型包括：Ai（AI助手）、Jsai（JS引擎）、Trans（翻译）、Sys（系统）等 21 类。"
            "通过 CXRServiceManager 管理消息发送与接收，支持 Caps 序列化。"
        ),
    },
}


async def query_knowledge(topic: str) -> str:
    """查询内置知识库获取 Rokid 相关信息

    Args:
        topic: 查询主题关键词
    """
    topic_lower = topic.lower()

    matches = []
    for key, info in KNOWLEDGE_BASE.items():
        if any(
            kw in topic_lower
            for kw in [key, info["title"].lower()]
        ) or any(
            kw in info["content"].lower()
            for kw in topic_lower.split()
        ):
            matches.append(info)

    if not matches:
        all_topics = [v["title"] for v in KNOWLEDGE_BASE.values()]
        return f"未找到与 '{topic}' 相关的知识。可用主题：{', '.join(all_topics)}"

    parts = []
    for m in matches[:3]:
        parts.append(f"【{m['title']}】\n{m['content']}")
    return "\n\n".join(parts)


TOOL_DEFINITION = {
    "name": "query_knowledge",
    "description": "查询 Rokid 产品知识库，获取智能眼镜相关技术、产品、市场信息",
    "parameters": {
        "type": "object",
        "properties": {
            "topic": {
                "type": "string",
                "description": "查询主题，如 'Rokid Glasses', 'CXR协议', 'AI眼镜市场'",
            },
        },
        "required": ["topic"],
    },
    "handler": query_knowledge,
}
