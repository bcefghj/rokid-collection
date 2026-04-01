"""ReAct Agent 核心：基于 LangChain 的推理-行动循环"""

from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langgraph.prebuilt import create_react_agent

from app.config import get_settings
from app.tools.search import web_search
from app.tools.amap import search_nearby, route_plan
from app.tools.translate import translate
from app.tools.datetime_tool import get_current_datetime, calculate
from app.rag.knowledge_base import search_knowledge_base

SYSTEM_PROMPT = """你是 Rokid AR 眼镜的智能助手，运行在用户的 AR 眼镜上。

你的能力：
1. 联网搜索：查找实时新闻、百科知识等
2. 附近搜索：搜索附近的餐馆、商场、景点等 POI
3. 路线规划：步行或驾车导航
4. 翻译：多语言翻译
5. 时间查询：当前日期和时间
6. 计算：数学计算
7. 知识库检索：查询已存储的领域知识

回答原则：
- 简洁明了，适合 AR 眼镜显示和 TTS 语音播报
- 单次回复控制在 100 字以内（适配语音播报）
- 优先使用工具获取准确信息，不要编造
- 中文回答"""

ALL_TOOLS = [
    web_search,
    search_nearby,
    route_plan,
    translate,
    get_current_datetime,
    calculate,
    search_knowledge_base,
]

_sessions: dict[str, list] = {}


def get_agent():
    """创建 ReAct Agent"""
    settings = get_settings()
    llm = ChatOpenAI(
        model=settings.model_name,
        api_key=settings.openai_api_key,
        base_url=settings.openai_base_url,
        temperature=0,
        streaming=True,
    )
    agent = create_react_agent(llm, ALL_TOOLS, prompt=SYSTEM_PROMPT)
    return agent


def chat(session_id: str, user_input: str) -> str:
    """同步对话"""
    agent = get_agent()

    if session_id not in _sessions:
        _sessions[session_id] = []

    _sessions[session_id].append(HumanMessage(content=user_input))

    result = agent.invoke({"messages": _sessions[session_id]})

    ai_messages = [m for m in result["messages"] if isinstance(m, AIMessage) and m.content]
    if ai_messages:
        reply = ai_messages[-1].content
        _sessions[session_id].append(AIMessage(content=reply))
        return reply

    return "抱歉，我暂时无法回答这个问题。"


async def chat_stream(session_id: str, user_input: str):
    """流式对话（SSE）"""
    agent = get_agent()

    if session_id not in _sessions:
        _sessions[session_id] = []

    _sessions[session_id].append(HumanMessage(content=user_input))

    full_reply = ""
    async for event in agent.astream_events(
        {"messages": _sessions[session_id]},
        version="v2",
    ):
        kind = event["event"]
        if kind == "on_chat_model_stream":
            chunk = event["data"]["chunk"]
            if chunk.content:
                full_reply += chunk.content
                yield chunk.content

    if full_reply:
        _sessions[session_id].append(AIMessage(content=full_reply))


def clear_session(session_id: str):
    """清除会话历史"""
    _sessions.pop(session_id, None)
