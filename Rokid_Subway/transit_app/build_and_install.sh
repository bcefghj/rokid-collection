#!/bin/bash
# Rokid 地铁导航 - 构建并安装到眼镜
# 用法: ./build_and_install.sh

set -e

echo "=============================="
echo "  Rokid 地铁导航 构建安装脚本"
echo "=============================="

cd "$(dirname "$0")"

# 检查 ADB 连接
echo ""
echo "[1/4] 检查 ADB 设备连接..."
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "❌ 未检测到 ADB 设备，请确保 Rokid 眼镜已通过 USB 连接"
    exit 1
fi
echo "✅ 已连接设备: $DEVICE"

# 构建 APK
echo ""
echo "[2/4] 构建 APK..."
if [ -f "gradlew" ]; then
    chmod +x gradlew
    ./gradlew assembleDebug
else
    echo "⚠️  未找到 gradlew，请先在 Android Studio 中构建项目"
    echo "    或运行: gradle wrapper 生成 gradlew"
    echo ""
    echo "也可以直接用 Android Studio 打开此项目构建"
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 构建失败"
    exit 1
fi
echo "✅ APK 构建成功: $APK_PATH"

# 安装到设备
echo ""
echo "[3/4] 安装到 Rokid 眼镜..."
adb install -r "$APK_PATH"
echo "✅ 安装成功"

# 启动应用
echo ""
echo "[4/4] 启动地铁导航..."
adb shell am start -n com.rokid.transit/.ui.TransitMainActivity
echo "✅ 已启动"

echo ""
echo "=============================="
echo "  安装完成！"
echo "  在眼镜的应用列表中可以找到「地铁导航」"
echo "=============================="
