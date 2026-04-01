# Rokid AI 视觉知识助手

基于多模态 LLM 和 RAG 的智能视觉问答系统，为 Rokid AI 眼镜提供拍照识物、知识增强问答等能力。

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | FastAPI + Uvicorn |
| 多模态 LLM | 通义千问-VL / OpenAI GPT-4V |
| 文本 LLM | 通义千问 / OpenAI |
| 向量数据库 | ChromaDB |
| Embedding | DashScope / OpenAI Embedding API |
| 图像处理 | Pillow |
| 流式响应 | SSE (Server-Sent Events) |

## 核心能力

- **多模态视觉分析**：通过眼镜摄像头拍照 → 多模态 LLM 分析 → 物体识别、场景理解
- **RAG 检索增强**：向量化知识库 + 语义检索 → 知识增强回答
- **Prompt Engineering**：5 种场景化提示模板（通用、旅行、购物、阅读、翻译）
- **知识库管理**：添加/批量导入/检索/删除知识条目
- **眼镜集成**：ADB 拍照 → 分析 → TTS 语音播报

## API 端点

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/vision/analyze` | 上传图片分析（RAG增强） |
| POST | `/api/vision/analyze-local` | 分析本地图片 |
| POST | `/api/rag/query` | 语义检索知识库 |
| POST | `/api/knowledge/add` | 添加知识条目 |
| POST | `/api/knowledge/batch-add` | 批量添加 |
| POST | `/api/knowledge/load-sample` | 加载示例数据 |
| GET | `/api/knowledge/count` | 知识库计数 |
| GET | `/health` | 健康检查 |

## 快速开始

```bash
# 1. 安装依赖
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 2. 配置 API Key（任选其一）

# 方式 A：通义千问
export DASHSCOPE_API_KEY="your-key"

# 方式 B：OpenAI
export LLM_BACKEND=openai
export OPENAI_API_KEY="your-key"

# 方式 C：MiniMax（官方文档 https://platform.minimax.io ）
export LLM_BACKEND=minimax
export MINIMAX_API_KEY="your-key"
# 可选：部分 Embedding 接口需要 Group-Id（控制台「接口密钥」旁可见）
# export MINIMAX_GROUP_ID="your-group-id"
# 可选：模型名（默认 MiniMax-M2.5；看图用 MiniMax-Text-01）
# export MINIMAX_MODEL="MiniMax-M2.5"
# export MINIMAX_VL_MODEL="MiniMax-Text-01"

# 3. 启动服务
python main.py
# 服务启动在 http://localhost:8000
# API 文档: http://localhost:8000/docs

# 4. 加载示例知识库
curl -X POST http://localhost:8000/api/knowledge/load-sample

# 5. 眼镜拍照分析
bash scripts/glasses_vision.sh photo "这是什么？" general
```

## 眼镜端交互

```bash
# 检查眼镜连接
bash scripts/glasses_vision.sh check

# 拍照并分析
bash scripts/glasses_vision.sh photo "这是什么建筑？" travel

# 截屏分析
bash scripts/glasses_vision.sh screen "翻译这段文字" translate

# 分析指定图片
bash scripts/glasses_vision.sh analyze /tmp/test.jpg "这是什么花？"
```

## 项目结构

```
rokid_ai_vision_rag/
├── main.py                     # FastAPI 服务入口
├── config.py                   # 配置管理
├── requirements.txt
├── api/
│   ├── vision.py               # 视觉分析 API
│   ├── rag.py                  # RAG 检索 API
│   └── knowledge.py            # 知识库管理 API
├── core/
│   ├── llm_client.py           # LLM 统一客户端
│   ├── vision_analyzer.py      # 多模态视觉分析器
│   ├── rag_pipeline.py         # RAG 管道
│   └── prompt_templates.py     # Prompt 模板
├── db/
│   └── vector_store.py         # ChromaDB 向量存储
├── models/
│   └── schemas.py              # Pydantic 数据模型
├── scripts/
│   └── glasses_vision.sh       # 眼镜端交互脚本
├── knowledge_base/
│   └── sample_data.json        # 示例知识数据
└── tests/
    └── test_api.py             # API 测试
```
