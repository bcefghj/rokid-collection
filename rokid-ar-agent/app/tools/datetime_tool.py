"""日期时间工具"""

from datetime import datetime
from langchain_core.tools import tool


@tool
def get_current_datetime() -> str:
    """获取当前日期和时间。当用户询问现在几点、今天星期几等时间相关问题时使用。"""
    now = datetime.now()
    weekdays = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    weekday = weekdays[now.weekday()]
    return f"当前时间：{now.strftime('%Y年%m月%d日 %H:%M:%S')} {weekday}"


@tool
def calculate(expression: str) -> str:
    """计算数学表达式。

    Args:
        expression: 数学表达式，如 "2+3*4"、"sqrt(16)"、"100/7"
    """
    import math

    safe_dict = {
        "abs": abs, "round": round, "min": min, "max": max,
        "sqrt": math.sqrt, "pow": pow, "pi": math.pi, "e": math.e,
        "sin": math.sin, "cos": math.cos, "tan": math.tan,
        "log": math.log, "log10": math.log10,
    }
    try:
        result = eval(expression, {"__builtins__": {}}, safe_dict)
        return f"{expression} = {result}"
    except Exception as e:
        return f"计算出错: {str(e)}"
