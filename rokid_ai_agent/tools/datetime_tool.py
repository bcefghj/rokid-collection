"""日期时间工具"""

from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

WEEKDAYS_CN = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]


async def get_datetime(action: str = "now", offset_days: int = 0) -> str:
    """获取日期时间信息

    Args:
        action: 操作类型 - now(当前时间), date(当前日期), weekday(星期几), countdown(倒计时)
        offset_days: 天数偏移（正数为未来，负数为过去）
    """
    now = datetime.now()
    target = now + timedelta(days=offset_days)

    if action == "now":
        return (
            f"当前时间：{target.strftime('%Y年%m月%d日 %H:%M:%S')} "
            f"{WEEKDAYS_CN[target.weekday()]}"
        )
    if action == "date":
        return (
            f"{target.strftime('%Y年%m月%d日')} "
            f"{WEEKDAYS_CN[target.weekday()]}"
        )
    if action == "weekday":
        return f"{target.strftime('%Y年%m月%d日')}是{WEEKDAYS_CN[target.weekday()]}"
    if action == "countdown":
        if offset_days == 0:
            return "请指定天数偏移量来计算倒计时"
        direction = "后" if offset_days > 0 else "前"
        return (
            f"{abs(offset_days)}天{direction}是"
            f"{target.strftime('%Y年%m月%d日')} "
            f"{WEEKDAYS_CN[target.weekday()]}"
        )
    return f"未知操作: {action}"


TOOL_DEFINITION = {
    "name": "get_datetime",
    "description": "获取日期时间信息，包括当前时间、日期计算、星期查询等",
    "parameters": {
        "type": "object",
        "properties": {
            "action": {
                "type": "string",
                "description": "操作类型: now/date/weekday/countdown",
                "default": "now",
                "enum": ["now", "date", "weekday", "countdown"],
            },
            "offset_days": {
                "type": "integer",
                "description": "天数偏移（正数未来，负数过去）",
                "default": 0,
            },
        },
    },
    "handler": get_datetime,
}
