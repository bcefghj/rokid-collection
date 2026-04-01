#!/bin/bash
# 双击此文件：通过 ADB 连接 Rokid AI 眼镜并显示眼镜屏幕
# 需要：1) 眼镜已用 USB 连接并开启 USB 调试  2) 已安装 scrcpy 和 adb

cd "$(dirname "$0")"

# 检查 adb
if ! command -v adb &> /dev/null; then
    echo "未找到 adb。请先安装 Android 平台工具："
    echo "  brew install --cask android-platform-tools"
    echo ""
    read -p "按回车键退出..."
    exit 1
fi

# 检查 scrcpy
if ! command -v scrcpy &> /dev/null; then
    echo "未找到 scrcpy。请先安装："
    echo "  brew install scrcpy"
    echo ""
    read -p "按回车键退出..."
    exit 1
fi

# 检查是否有设备连接
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "未检测到设备。请确认："
    echo "  1. Rokid 眼镜已用 USB 连接到电脑"
    echo "  2. 眼镜已开启「开发者选项」和「USB 调试」"
    echo ""
    adb devices
    echo ""
    read -p "按回车键退出..."
    exit 1
fi

echo "正在打开 Rokid 眼镜屏幕..."
echo "关闭本窗口即可结束投屏。"
echo ""

# 启动 scrcpy 显示眼镜画面（无音频、适合眼镜投屏）
scrcpy --no-audio

read -p "投屏已结束，按回车键关闭窗口..."
