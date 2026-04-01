#!/bin/bash
# 双击：打开眼镜画面，并用键盘操作
# 左方向键=向后滑动，右方向键=向前滑动，空格=单击，连续两次空格=双击

cd "$(dirname "$0")"
SCRIPT_DIR="$(pwd)"

# 检查 adb
if ! command -v adb &> /dev/null; then
    echo "未找到 adb。请先安装：brew install --cask android-platform-tools"
    read -p "按回车键退出..."
    exit 1
fi

# 检查 scrcpy
if ! command -v scrcpy &> /dev/null; then
    echo "未找到 scrcpy。请先安装：brew install scrcpy"
    read -p "按回车键退出..."
    exit 1
fi

# 检查设备
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "未检测到设备，请连接眼镜并开启 USB 调试。"
    adb devices
    read -p "按回车键退出..."
    exit 1
fi

# 检查 Python3
if ! command -v python3 &> /dev/null; then
    echo "未找到 python3，请先安装 Python 3。"
    read -p "按回车键退出..."
    exit 1
fi

# 检查 pynput
if ! python3 -c "import pynput" 2>/dev/null; then
    echo "未找到 pynput。请先安装：pip3 install pynput"
    read -p "按回车键退出..."
    exit 1
fi

# 后台启动 scrcpy
scrcpy --no-audio &
SCRCPY_PID=$!

# 退出时结束 scrcpy
cleanup() {
    kill $SCRCPY_PID 2>/dev/null
    exit 0
}
trap cleanup INT TERM

echo "已打开眼镜画面。"
echo "  ← 左方向键 = 向后滑动    → 右方向键 = 向前滑动"
echo "  空格 = 单击（点屏幕中心）   连续按两次空格 = 双击"
echo "  按 Ctrl+C 结束"
echo ""

python3 "$SCRIPT_DIR/key_control_glasses.py"

cleanup
