#!/bin/bash
# Rokid AI 眼镜视觉助手 - 眼镜端交互脚本
# 功能：通过 ADB 操作眼镜摄像头拍照，发送到后端分析，语音播报结果

set -e

SERVER_URL="${SERVER_URL:-http://localhost:8000}"
PHOTO_DIR="/tmp/rokid_vision"
GLASSES_DCIM="/sdcard/DCIM/Camera"

mkdir -p "$PHOTO_DIR"

adb_cmd() {
    if [ -n "$ADB_DEVICE" ]; then
        adb -s "$ADB_DEVICE" "$@"
    else
        adb "$@"
    fi
}

check_connection() {
    if ! adb_cmd get-state >/dev/null 2>&1; then
        echo "错误: 未检测到 Rokid 眼镜连接"
        echo "请确保眼镜通过 USB 连接并已开启 ADB 调试"
        exit 1
    fi
    local model
    model=$(adb_cmd shell getprop ro.product.model 2>/dev/null)
    echo "已连接设备: $model"
}

capture_photo() {
    local filename="rokid_capture_$(date +%Y%m%d_%H%M%S).jpg"
    local local_path="$PHOTO_DIR/$filename"

    echo "正在通过眼镜摄像头拍照..."

    # 方法1: 使用 screencap 截取当前屏幕（包含相机预览）
    adb_cmd shell screencap -p "/sdcard/$filename"
    adb_cmd pull "/sdcard/$filename" "$local_path" >/dev/null 2>&1
    adb_cmd shell rm "/sdcard/$filename"

    echo "$local_path"
}

capture_camera_photo() {
    local filename="rokid_cam_$(date +%Y%m%d_%H%M%S).jpg"
    local local_path="$PHOTO_DIR/$filename"

    echo "正在触发眼镜拍照..."

    # 通过 keyevent 触发拍照
    adb_cmd shell input keyevent 27  # KEYCODE_CAMERA

    sleep 2

    # 获取最新拍摄的照片
    local latest
    latest=$(adb_cmd shell "ls -t $GLASSES_DCIM/*.jpg 2>/dev/null | head -1" | tr -d '\r')

    if [ -n "$latest" ] && [ "$latest" != "" ]; then
        adb_cmd pull "$latest" "$local_path" >/dev/null 2>&1
        echo "$local_path"
    else
        echo "未找到照片，使用屏幕截图替代"
        capture_photo
    fi
}

analyze_image() {
    local image_path="$1"
    local question="${2:-请描述这张图片的内容}"
    local scene="${3:-general}"

    echo "正在分析图片..."
    echo "  问题: $question"
    echo "  场景: $scene"

    local response
    response=$(curl -s -X POST "$SERVER_URL/api/vision/analyze" \
        -F "image=@$image_path" \
        -F "question=$question" \
        -F "use_rag=true" \
        -F "scene_type=$scene")

    local answer
    answer=$(echo "$response" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('enhanced_answer', d.get('vision_description', '分析失败')))" 2>/dev/null || echo "服务连接失败")

    echo ""
    echo "====== 分析结果 ======"
    echo "$answer"
    echo "======================"

    # 通过 TTS 在眼镜上播报
    tts_speak "$answer"
}

tts_speak() {
    local text="$1"
    # 通过广播触发 Rokid 系统 TTS
    adb_cmd shell am broadcast \
        -a com.rokid.os.master.assist.server.cmd \
        --es cmd_type "tts_speak" \
        --es text "$text" \
        2>/dev/null || true

    echo "(已发送 TTS 播报请求)"
}

load_knowledge() {
    echo "正在加载示例知识库..."
    local resp
    resp=$(curl -s -X POST "$SERVER_URL/api/knowledge/load-sample")
    echo "$resp" | python3 -c "import sys, json; d=json.load(sys.stdin); print(f'已加载 {d.get(\"count\", 0)} 条知识')" 2>/dev/null
}

show_help() {
    cat <<EOF
Rokid AI 视觉知识助手 - 眼镜端交互工具

用法: $0 <命令> [参数]

命令:
  photo [问题] [场景]    拍照并分析（场景：general/travel/shopping/reading/translate）
  screen [问题] [场景]   截屏并分析
  analyze <图片路径> [问题] [场景]   分析指定图片
  load-kb               加载示例知识库
  check                 检查设备连接
  help                  显示帮助

示例:
  $0 photo "这是什么建筑？" travel
  $0 screen "翻译这段文字" translate
  $0 analyze /tmp/test.jpg "这是什么花？" general
  $0 load-kb

环境变量:
  SERVER_URL   后端服务地址 (默认: http://localhost:8000)
  ADB_DEVICE   ADB 设备序列号 (可选)
EOF
}

case "${1:-help}" in
    photo)
        check_connection
        image=$(capture_camera_photo)
        analyze_image "$image" "${2:-请描述这张图片的内容}" "${3:-general}"
        ;;
    screen)
        check_connection
        image=$(capture_photo)
        analyze_image "$image" "${2:-请描述这张图片的内容}" "${3:-general}"
        ;;
    analyze)
        if [ -z "$2" ]; then
            echo "请指定图片路径"
            exit 1
        fi
        analyze_image "$2" "${3:-请描述这张图片的内容}" "${4:-general}"
        ;;
    load-kb)
        load_knowledge
        ;;
    check)
        check_connection
        ;;
    help|*)
        show_help
        ;;
esac
