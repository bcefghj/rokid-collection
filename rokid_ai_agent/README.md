# Rokid AI 智能场景 Agent

基于 ReAct 模式的 AI Agent 系统，集成多种工具（搜索、翻译、计算、知识库、眼镜控制），为 Rokid AI 眼镜提供智能场景交互能力。

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | FastAPI + Uvicorn |
| Agent 架构 | ReAct (Reasoning + Acting) |
| LLM | 通义千问 / OpenAI (Function Calling) |
| 对话记忆 | FAISS 向量索引 |
| 流式响应 | SSE (Server-Sent Events) |
| 眼镜控制 | ADB 命令 |

## 核心能力

- **ReAct Agent**：自动推理 → 选择工具 → 执行 → 返回结果
- **6 大工具**：联网搜索、多语言翻译、数学计算、知识查询、日期时间、眼镜控制
- **对话记忆**：基于 FAISS 的向量化长期记忆
- **Function Calling**：原生 OpenAI/DashScope 工具调用协议
- **多会话**：支持并行多个对话会话
- **流式输出**：SSE 流式响应支持

## 工具列表

| 工具 | 功能 | 示例 |
|------|------|------|
| `web_search` | 联网搜索 | "搜索最新AI眼镜新闻" |
| `translate` | 多语言翻译 | "翻译Hello为中文" |
| `calculator` | 数学计算 | "计算 sqrt(144)+pi" |
| `query_knowledge` | Rokid知识库 | "查询Rokid Glasses信息" |
| `get_datetime` | 日期时间 | "今天几号？30天后呢？" |
| `glasses_control` | 眼镜控制 | "拍照/查电量/设备信息" |

## API 端点

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/agent/chat` | Agent 对话（自动工具调用） |
| POST | `/api/agent/chat/stream` | 流式对话 |
| GET | `/api/conversation/sessions` | 会话列表 |
| GET | `/api/conversation/sessions/{id}` | 会话详情 |
| DELETE | `/api/conversation/sessions/{id}` | 清除会话 |
| GET | `/api/tools/list` | 工具列表 |
| POST | `/api/tools/execute/{name}` | 直接执行工具 |
| GET | `/health` | 健康检查 |

## 快速开始

```bash
# 1. 安装依赖
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 2. 配置 API Key（任选其一）
export DASHSCOPE_API_KEY="your-key"
# 或 OpenAI
export LLM_BACKEND=openai && export OPENAI_API_KEY="your-key"
# 或 MiniMax（支持 Agent 工具调用，与官方 chatcompletion_v2 一致）
export LLM_BACKEND=minimax && export MINIMAX_API_KEY="your-key"
# export MINIMAX_GROUP_ID="..."   # Embedding/部分接口需要时再加

# 3. 启动服务
python main.py
# 服务启动在 http://localhost:8001
# API 文档: http://localhost:8001/docs

# 4. 与 Agent 对话
curl -X POST http://localhost:8001/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我查看眼镜的电量", "session_id": "test"}'
```

## 眼镜端交互

```bash
# 交互式对话模式
bash scripts/glasses_agent.sh interactive

# 单条消息
bash scripts/glasses_agent.sh chat "Rokid Glasses有哪些AI功能？"

# 运行演示流程
bash scripts/glasses_agent.sh demo

# 查看可用工具
bash scripts/glasses_agent.sh tools
```

## 项目结构

```
rokid_ai_agent/
├── main.py                     # FastAPI 服务入口 + 工具注册
├── config.py                   # 配置管理
├── requirements.txt
├── api/
│   ├── agent.py                # Agent 对话 API
│   ├── conversation.py         # 会话管理 API
│   └── tools.py                # 工具管理 API
├── core/
│   ├── llm_client.py           # LLM 客户端（Function Calling）
│   ├── agent_executor.py       # ReAct Agent 执行引擎
│   ├── memory.py               # FAISS 对话记忆
│   └── tool_registry.py        # 工具注册中心
├── tools/
│   ├── search_tool.py          # 联网搜索
│   ├── translate_tool.py       # 多语言翻译
│   ├── calculator_tool.py      # 数学计算
│   ├── knowledge_tool.py       # 知识查询
│   ├── datetime_tool.py        # 日期时间
│   └── glasses_tool.py         # 眼镜 ADB 控制
├── models/
│   └── schemas.py              # Pydantic 数据模型
├── scripts/
│   └── glasses_agent.sh        # 眼镜端交互脚本
└── tests/
    ├── test_tools.py           # 工具测试
    └── test_api.py             # API 测试
```

## Agent 工作流程

```
用户输入
  ↓
Agent 接收消息 + 加载对话历史
  ↓
LLM 推理（带工具定义）
  ↓
┌─ finish_reason == "tool_calls" ─→ 执行工具 → 将结果送回 LLM → 循环
└─ finish_reason == "stop" ────────→ 返回最终回答
  ↓
存入对话记忆 + 长期向量记忆
  ↓
返回结果（含工具调用记录 + 推理过程）
```
