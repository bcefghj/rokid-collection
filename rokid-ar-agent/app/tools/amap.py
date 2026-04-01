"""高德地图工具：附近 POI 搜索与路线规划，适配 Rokid AR 眼镜的 LBS 场景"""

import httpx
from langchain_core.tools import tool
from app.config import get_settings


@tool
def search_nearby(keyword: str, location: str = "", city: str = "武汉") -> str:
    """搜索附近的地点（餐馆、商场、景点等）。

    Args:
        keyword: 搜索关键词，如"餐馆"、"咖啡"、"便利店"
        location: 经纬度，格式为"经度,纬度"，不传则按城市搜索
        city: 城市名，默认武汉
    """
    settings = get_settings()
    if not settings.amap_api_key:
        return "未配置高德 API Key，请在 .env 中设置 AMAP_API_KEY"

    params = {
        "key": settings.amap_api_key,
        "keywords": keyword,
        "city": city,
        "offset": 10,
        "extensions": "all",
    }
    if location:
        params["location"] = location
        params["sortrule"] = "distance"

    try:
        resp = httpx.get(
            "https://restapi.amap.com/v3/place/text",
            params=params,
            timeout=10,
        )
        data = resp.json()
        if data.get("status") != "1" or not data.get("pois"):
            return f"未找到附近的{keyword}"

        lines = []
        for i, poi in enumerate(data["pois"][:8], 1):
            name = poi.get("name", "")
            address = poi.get("address", "")
            tel = poi.get("tel", "")
            distance = poi.get("distance", "")
            dist_str = f"  距离{distance}m" if distance else ""
            lines.append(f"{i}. {name}{dist_str}\n   地址: {address}  电话: {tel}")

        return "\n".join(lines)
    except Exception as e:
        return f"搜索出错: {str(e)}"


@tool
def route_plan(origin: str, destination: str, mode: str = "walking") -> str:
    """路线规划（步行或驾车）。

    Args:
        origin: 起点，格式为"经度,纬度"或地名
        destination: 终点，格式为"经度,纬度"或地名
        mode: 出行方式，walking(步行) 或 driving(驾车)
    """
    settings = get_settings()
    if not settings.amap_api_key:
        return "未配置高德 API Key"

    def geocode(address: str) -> str:
        if "," in address and address.replace(",", "").replace(".", "").isdigit():
            return address
        resp = httpx.get(
            "https://restapi.amap.com/v3/geocode/geo",
            params={"key": settings.amap_api_key, "address": address},
            timeout=10,
        )
        data = resp.json()
        if data.get("geocodes"):
            return data["geocodes"][0].get("location", "")
        return ""

    try:
        origin_loc = geocode(origin)
        dest_loc = geocode(destination)
        if not origin_loc or not dest_loc:
            return "无法解析起点或终点地址，请提供更具体的地名"

        endpoint = "walking" if mode == "walking" else "driving"
        resp = httpx.get(
            f"https://restapi.amap.com/v3/direction/{endpoint}",
            params={
                "key": settings.amap_api_key,
                "origin": origin_loc,
                "destination": dest_loc,
            },
            timeout=10,
        )
        data = resp.json()

        if data.get("status") != "1":
            return "路线规划失败"

        route = data.get("route", {})
        paths = route.get("paths", [])
        if not paths:
            return "未找到可用路线"

        path = paths[0]
        distance = int(path.get("distance", 0))
        duration = int(path.get("duration", 0))

        steps = []
        for s in path.get("steps", [])[:10]:
            steps.append(f"  → {s.get('instruction', '')}")

        mode_str = "步行" if mode == "walking" else "驾车"
        result = f"{mode_str}路线：总距离 {distance}m，预计 {duration // 60} 分钟\n"
        result += "\n".join(steps)
        return result
    except Exception as e:
        return f"路线规划出错: {str(e)}"
