# Rokid AR 智能助手 Agent

基于 ReAct 范式的 AI Agent，为 Rokid AR 眼镜提供多模态智能交互。

## 功能特性

- **ReAct 推理**：自动推理 → 选择工具 → 执行 → 返回，支持多步复杂任务
- **RAG 知识检索**：ChromaDB 向量数据库，支持文档导入与语义检索
- **多工具调用**：联网搜索、附近 POI、路线导航、翻译、计算、时间查询
- **流式输出**：SSE 流式响应，适配 AR 眼镜实时显示
- **多会话管理**：支持多用户独立会话与历史记录
- **Docker 部署**：一键启动，开箱即用

## 技术栈

| 组件 | 技术 |
|------|------|
| Agent 框架 | LangChain + LangGraph (ReAct) |
| LLM | OpenAI API / 通义千问（可切换） |
| 向量数据库 | ChromaDB |
| 后端框架 | FastAPI + Uvicorn |
| 流式传输 | SSE (Server-Sent Events) |
| 地图服务 | 高德地图 Web API |
| 部署 | Docker / Docker Compose |

## 架构

```
用户语音/文字 → FastAPI API → ReAct Agent → 工具调用 → LLM 推理 → 响应
                                  ↓
                          ChromaDB (RAG)
                          高德地图 (POI/导航)
                          DuckDuckGo (搜索)
                          翻译 / 计算 / 时间
```

## 快速开始

### 1. 环境配置

```bash
cp .env.example .env
# 编辑 .env 填入 API Key
```

### 2. 本地运行

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### 3. Docker 运行

```bash
docker compose up -d
```

### 4. 访问 API

- API 文档：http://localhost:8000/docs
- 健康检查：http://localhost:8000/health

## API 接口

### 对话

```bash
# 同步对话
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "附近有什么好吃的餐馆？"}'

# 流式对话 (SSE)
curl -X POST http://localhost:8000/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我规划从光谷到武汉站的步行路线"}'
```

### 知识库

```bash
# 添加知识
curl -X POST http://localhost:8000/knowledge/add \
  -H "Content-Type: application/json" \
  -d '{"texts": ["Rokid Glasses 支持 Android 12", "眼镜分辨率为 480x640"]}'

# 检索知识
curl -X POST http://localhost:8000/knowledge/query \
  -H "Content-Type: application/json" \
  -d '{"query": "眼镜分辨率是多少"}'
```

## 项目结构

```
rokid-ar-agent/
├── app/
│   ├── main.py              # FastAPI 入口 & API 路由
│   ├── agent.py             # ReAct Agent 核心逻辑
│   ├── config.py            # 配置管理
│   ├── tools/               # 工具集
│   │   ├── search.py        # 联网搜索
│   │   ├── amap.py          # 高德地图 POI & 导航
│   │   ├── translate.py     # 多语言翻译
│   │   └── datetime_tool.py # 时间 & 计算
│   └── rag/
│       └── knowledge_base.py # ChromaDB 知识库
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
└── .env.example
```

## License

MIT
