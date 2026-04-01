#!/bin/bash
# 地铁导航快捷启动脚本
# 用法: ./subway.sh 目的地 [出发地经纬度] [城市]
# 例子:
#   ./subway.sh 北京西站
#   ./subway.sh 天安门 116.397428,39.90923 北京市
#   ./subway.sh 上海虹桥站

DEST="$1"
ORIGIN="${2:-}"
CITY="${3:-}"

if [ -z "$DEST" ]; then
    echo "用法: ./subway.sh <目的地> [出发地经纬度] [城市]"
    echo ""
    echo "例子:"
    echo "  ./subway.sh 北京西站"
    echo "  ./subway.sh 天安门 116.397428,39.90923 北京市"
    echo "  ./subway.sh 上海虹桥站"
    exit 1
fi

echo "🚇 地铁导航: $DEST"

# 确保WiFi连接
echo "检查网络..."
WIFI_STATUS=$(adb shell dumpsys connectivity 2>/dev/null | head -5 | grep "Active default network")
if echo "$WIFI_STATUS" | grep -q "none"; then
    echo "尝试连接WiFi..."
    adb shell svc wifi enable 2>/dev/null
    sleep 1
    adb shell 'cmd wifi connect-network "iPhone" wpa2 "13296886603"' 2>/dev/null
    sleep 3
fi

# 构建intent
INTENT_ARGS="--es destination \"$DEST\""
if [ -n "$ORIGIN" ]; then
    INTENT_ARGS="$INTENT_ARGS --es origin \"$ORIGIN\""
fi
if [ -n "$CITY" ]; then
    INTENT_ARGS="$INTENT_ARGS --es city \"$CITY\""
fi

adb shell am force-stop com.rokid.transit 2>/dev/null
adb shell "am start -n com.rokid.transit/.ui.TransitMainActivity $INTENT_ARGS" 2>/dev/null

echo "已启动地铁导航 → $DEST"
