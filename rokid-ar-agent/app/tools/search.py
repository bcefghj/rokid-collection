"""联网搜索工具：通过 DuckDuckGo 搜索实时信息"""

import httpx
from langchain_core.tools import tool


@tool
def web_search(query: str) -> str:
    """搜索互联网获取实时信息。当用户询问新闻、天气、实时数据等需要联网查询的问题时使用。

    Args:
        query: 搜索关键词
    """
    try:
        resp = httpx.get(
            "https://api.duckduckgo.com/",
            params={"q": query, "format": "json", "no_html": 1, "skip_disambig": 1},
            timeout=10,
        )
        data = resp.json()

        results = []
        if data.get("Abstract"):
            results.append(data["Abstract"])
        for topic in data.get("RelatedTopics", [])[:5]:
            if isinstance(topic, dict) and topic.get("Text"):
                results.append(topic["Text"])

        if not results:
            return f"未找到关于 '{query}' 的搜索结果，请尝试换个关键词。"
        return "\n".join(results)
    except Exception as e:
        return f"搜索出错: {str(e)}"
