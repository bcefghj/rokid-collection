"""工具单元测试"""

import pytest
import asyncio


@pytest.fixture
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.mark.asyncio
async def test_calculator():
    from tools.calculator_tool import calculate

    result = await calculate("2 + 3 * 4")
    assert "14" in result

    result = await calculate("sqrt(144)")
    assert "12" in result

    result = await calculate("1 / 0")
    assert "零" in result or "除" in result


@pytest.mark.asyncio
async def test_datetime():
    from tools.datetime_tool import get_datetime

    result = await get_datetime("now")
    assert "当前时间" in result

    result = await get_datetime("weekday")
    assert "星期" in result

    result = await get_datetime("countdown", offset_days=7)
    assert "7天后" in result


@pytest.mark.asyncio
async def test_knowledge():
    from tools.knowledge_tool import query_knowledge

    result = await query_knowledge("Rokid Glasses")
    assert "Rokid" in result

    result = await query_knowledge("CXR")
    assert "CXR" in result or "通信" in result

    result = await query_knowledge("不存在的主题xyz")
    assert "未找到" in result


def test_tool_registry():
    from core.tool_registry import ToolRegistry

    reg = ToolRegistry()

    async def dummy_handler(x: str = "") -> str:
        return f"result: {x}"

    reg.register(
        "test_tool",
        "测试工具",
        {"type": "object", "properties": {"x": {"type": "string"}}},
        dummy_handler,
    )

    assert "test_tool" in reg.list_tools()
    assert len(reg.get_openai_tools()) == 1

    tool = reg.get_tool("test_tool")
    assert tool is not None
    assert tool.name == "test_tool"
