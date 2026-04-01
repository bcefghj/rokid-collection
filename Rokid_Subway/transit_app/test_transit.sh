#!/bin/bash
# Rokid 地铁导航 - 快速测试脚本
# 用法: ./test_transit.sh [城市]
# 
# 注意: 眼镜需要连接 WiFi 才能查询路线
# 如果眼镜未联网，可以用手机开热点给眼镜连

CITY="${1:-北京市}"

echo "=============================="
echo "  Rokid 地铁导航 测试"
echo "=============================="

# 检查 WiFi
echo ""
echo "[检查] 眼镜网络状态..."
WIFI_CONNECTED=$(adb shell dumpsys wifi | grep "current SSID" | head -1)
echo "  WiFi: $WIFI_CONNECTED"

PING_RESULT=$(adb shell ping -c 1 -W 2 restapi.amap.com 2>&1)
if echo "$PING_RESULT" | grep -q "1 received"; then
    echo "  网络: ✅ 可访问高德 API"
else
    echo "  网络: ❌ 无法访问高德 API"
    echo ""
    echo "  请先让眼镜连接 WiFi:"
    echo "    方式1: 在眼镜设置中连接 WiFi"
    echo "    方式2: adb shell cmd wifi connect-network <SSID> wpa2 <密码>"
    echo ""
    echo "  或者用手机开热点让眼镜连接"
    exit 1
fi

# 测试场景
echo ""
echo "[测试] 发送查询: 天安门广场 → 北京西站 (${CITY})"
adb shell am force-stop com.rokid.transit 2>/dev/null
sleep 1
adb shell am start -n com.rokid.transit/.ui.TransitMainActivity \
    --es origin "116.397128,39.916527" \
    --es city "$CITY" \
    --es origin_name "天安门广场" \
    --es destination "北京西站"

echo ""
echo "✅ 已发送查询，请查看眼镜屏幕"
echo ""
echo "=============================="
echo "  其他测试命令："
echo ""
echo "  # 查上海的地铁"
echo "  adb shell am start -n com.rokid.transit/.ui.TransitMainActivity \\"
echo "    --es origin '121.473701,31.230416' \\"
echo "    --es city '上海市' \\"
echo "    --es origin_name '人民广场' \\"
echo "    --es destination '浦东机场'"
echo ""
echo "  # 只打开app手动输入"
echo "  adb shell am start -n com.rokid.transit/.ui.TransitMainActivity"
echo ""
echo "  # 查看日志"
echo "  adb logcat | grep -i transit"
echo "=============================="
