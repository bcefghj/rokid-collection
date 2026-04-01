#!/bin/bash
# 双击此文件：在电脑上查看 Rokid 眼镜画面，并用电脑鼠标/键盘直接操作眼镜
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

echo "正在打开眼镜画面（可用电脑操作）..."
echo ""
echo "  ► 在弹窗里：鼠标点击 = 眼镜触屏点击，拖动 = 滑动，键盘可直接输入"
echo "  ► 关闭投屏窗口或在本窗口按 Ctrl+C 即可结束"
echo ""

# 启动 scrcpy：画面 + 键鼠操作都会同步到眼镜
scrcpy --no-audio

read -p "已断开，按回车键关闭窗口..."
