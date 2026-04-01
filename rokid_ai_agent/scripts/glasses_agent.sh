#!/bin/bash
# Rokid AI Agent - 眼镜端交互脚本
# 通过命令行与 Agent 服务交互，操作 Rokid 眼镜

set -e

SERVER_URL="${SERVER_URL:-http://localhost:8001}"
SESSION_ID="${SESSION_ID:-glasses_$(date +%Y%m%d)}"

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
        exit 1
    fi
    local model
    model=$(adb_cmd shell getprop ro.product.model 2>/dev/null)
    echo "已连接: $model"
}

check_server() {
    if ! curl -s "$SERVER_URL/health" >/dev/null 2>&1; then
        echo "错误: Agent 服务未启动 ($SERVER_URL)"
        echo "请先运行: cd rokid_ai_agent && python main.py"
        exit 1
    fi
    echo "Agent 服务已连接"
}

agent_chat() {
    local message="$1"
    local context="${2:-}"

    echo ""
    echo "你: $message"
    echo "---"

    local body
    body=$(python3 -c "
import json
d = {
    'message': '''$message''',
    'session_id': '$SESSION_ID',
    'scene_context': '''$context'''
}
print(json.dumps(d, ensure_ascii=False))
")

    local response
    response=$(curl -s -X POST "$SERVER_URL/api/agent/chat" \
        -H "Content-Type: application/json" \
        -d "$body")

    local answer
    answer=$(echo "$response" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('answer', '处理失败'))
# 显示工具调用
for tc in d.get('tool_calls', []):
    print(f'  [工具] {tc[\"tool_name\"]}: {tc[\"tool_output\"][:100]}')
" 2>/dev/null || echo "服务连接失败")

    echo "Agent: $answer"

    # TTS 播报
    local short_answer
    short_answer=$(echo "$answer" | head -3 | cut -c1-200)
    adb_cmd shell am broadcast \
        -a com.rokid.os.master.assist.server.cmd \
        --es cmd_type "tts_speak" \
        --es text "$short_answer" \
        2>/dev/null || true
}

interactive_mode() {
    echo "========================================="
    echo "  Rokid AI Agent - 交互模式"
    echo "  会话ID: $SESSION_ID"
    echo "  输入 'quit' 退出, 'photo' 拍照分析"
    echo "========================================="
    echo ""

    while true; do
        read -p "你> " input
        case "$input" in
            quit|exit|q)
                echo "再见！"
                break
                ;;
            photo)
                agent_chat "请帮我拍照并描述看到的内容"
                ;;
            battery|电量)
                agent_chat "查看眼镜电量"
                ;;
            info|设备)
                agent_chat "查看眼镜设备信息"
                ;;
            "")
                continue
                ;;
            *)
                agent_chat "$input"
                ;;
        esac
    done
}

demo_mode() {
    echo "====== Rokid AI Agent 演示 ======"
    echo ""

    echo "--- 演示1: 查询设备信息 ---"
    agent_chat "帮我看看眼镜的设备信息和电量"
    sleep 2

    echo ""
    echo "--- 演示2: 知识查询 ---"
    agent_chat "Rokid Glasses 有哪些 AI 功能？"
    sleep 2

    echo ""
    echo "--- 演示3: 翻译功能 ---"
    agent_chat "帮我把'你好，世界'翻译成英文、日文和法文"
    sleep 2

    echo ""
    echo "--- 演示4: 数学计算 ---"
    agent_chat "帮我算一下 sqrt(144) + sin(pi/2) * 100"
    sleep 2

    echo ""
    echo "--- 演示5: 联网搜索 ---"
    agent_chat "2026年最新的AI眼镜市场情况怎么样？"
    sleep 2

    echo ""
    echo "--- 演示6: 日期查询 ---"
    agent_chat "今天是几号？30天后是什么日子？"
    sleep 2

    echo ""
    echo "====== 演示完成 ======"
}

show_help() {
    cat <<EOF
Rokid AI Agent - 眼镜端交互工具

用法: $0 <命令> [参数]

命令:
  chat <消息>     发送单条消息给 Agent
  interactive     交互式对话模式
  demo            运行演示流程
  tools           列出可用工具
  sessions        查看会话列表
  check           检查连接状态
  help            显示帮助

示例:
  $0 chat "今天天气怎么样？"
  $0 chat "帮我把Hello翻译成中文"
  $0 interactive
  $0 demo

环境变量:
  SERVER_URL   Agent 服务地址 (默认: http://localhost:8001)
  SESSION_ID   会话 ID (默认: glasses_日期)
  ADB_DEVICE   ADB 设备序列号 (可选)
EOF
}

case "${1:-help}" in
    chat)
        shift
        check_server
        agent_chat "$*"
        ;;
    interactive|i)
        check_connection
        check_server
        interactive_mode
        ;;
    demo)
        check_connection
        check_server
        demo_mode
        ;;
    tools)
        curl -s "$SERVER_URL/api/tools/list" | python3 -m json.tool
        ;;
    sessions)
        curl -s "$SERVER_URL/api/conversation/sessions" | python3 -m json.tool
        ;;
    check)
        check_connection
        check_server
        echo "一切就绪！"
        ;;
    help|*)
        show_help
        ;;
esac
