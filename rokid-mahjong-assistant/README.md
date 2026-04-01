# Rokid AI 麻将助手

> **⚠️ 参考声明 / Attribution Notice**
>
> 本项目基于 **[LYiHub/AR-Mahjong-Assistant-preview](https://github.com/LYiHub/AR-Mahjong-Assistant-preview)** 改编，在此郑重声明并致谢原作者 **LYiHub**。
>
> 原项目为面向 **RayNeo（雷鸟）X3 Pro AR 眼镜** 设计的麻将辅助系统，本项目在其基础上进行了适配，使其能运行在 **Rokid AI 眼镜（乐奇）** 上。
>
> 如原作者认为本项目存在侵权问题，请通过 GitHub Issues 联系，本人将立即删除。
>
> This project is derived from [LYiHub/AR-Mahjong-Assistant-preview](https://github.com/LYiHub/AR-Mahjong-Assistant-preview). Full credit goes to the original author **LYiHub**. If you find any copyright concerns, please open an issue and this repository will be removed immediately.

---

基于 Rokid AI 眼镜（乐奇）的 AR 麻将辅助应用。通过摄像头识别手牌，AI 分析最优出牌策略，在眼镜屏幕上实时显示建议。

## 功能特性

- **手牌识别**：通过眼镜摄像头拍摄手牌，YOLO 模型自动识别
- **牌效分析**：计算向听数、进张数，给出最优切牌建议
- **吃碰杠提示**：分析可能的吃、碰、杠机会及最优应对
- **语音场况**：通过语音输入记录其他玩家出牌，优化分析精度
- **AR 显示**：480x640 竖屏适配，黑底绿字 HUD 风格

## 系统要求

### 眼镜端
- Rokid AI 眼镜（乐奇 RG-glasses）
- Android 12 (API 32)
- 480x640 竖屏显示

### 服务端
- Python 3.9+
- LM Studio / Ollama（本地 LLM，用于语音意图解析）

## 项目结构

```
rokid-mahjong-assistant/
├── app/                    # Android 客户端
│   ├── src/main/
│   │   ├── java/com/rokid/mahjong/
│   │   │   ├── MainActivity.kt        # 主界面 + 相机 + 按键控制
│   │   │   ├── AppConfig.kt           # 配置（服务端地址等）
│   │   │   ├── RokidMahjongApp.kt     # Application
│   │   │   ├── model/                 # 数据模型
│   │   │   ├── repository/            # 数据仓库
│   │   │   ├── service/               # API 服务 + 设备管理
│   │   │   ├── utils/                 # 麻将映射 + 录音
│   │   │   └── viewmodel/             # ViewModel
│   │   └── res/                       # 布局、颜色、字体等
│   └── build.gradle.kts
├── server/                 # Python 服务端（FastAPI）
│   ├── main.py             # API 入口
│   ├── vision_service.py   # YOLO 视觉识别
│   ├── yolo_inference.py   # ONNX 推理引擎
│   ├── efficiency_engine.py # 牌效计算引擎
│   ├── mahjong_state_tracker.py # 对局状态追踪
│   ├── stt_service.py      # 语音转文字
│   ├── llm_service.py      # LLM 意图解析
│   ├── models/yolo/        # YOLO 模型权重
│   └── requirements.txt
├── docker-compose.yml
└── README.md
```

## 快速开始

### 1. 启动服务端

```bash
# 方式一：Docker
docker-compose up -d

# 方式二：本地运行
cd server
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env 配置 LLM 地址
python main.py
```

### 2. 配置客户端

编辑 `app/src/main/java/com/rokid/mahjong/AppConfig.kt`：
```kotlin
const val SERVER_BASE_URL = "http://你的电脑IP:8000/"
```

### 3. 编译安装

使用 Android Studio 打开项目，连接 Rokid 眼镜，编译运行。

或使用命令行：
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 操作方式

| 操作 | 功能 |
|------|------|
| **音量+** | 开始/结束对局 |
| **确认键（触控板点击）** | 拍照 / 确认发送 |
| **右键（DPAD_RIGHT）** | 开始/停止录音 / 取消操作 |
| **左键（DPAD_LEFT）** | 发送照片（照片预览时） |
| **上下滑动** | 滚动操作建议 |
| **音量-** | 返回 |

## 技术架构

```
Rokid 眼镜 (客户端)          服务端 (电脑)
┌───────────────┐          ┌──────────────────┐
│  Camera2 拍照  │───HTTP──→│  FastAPI          │
│  触控板操作    │          │  ├─ YOLO 识别      │
│  录音/语音    │───HTTP──→│  ├─ 牌效计算       │
│  AR 显示      │←──JSON───│  ├─ STT 语音识别   │
│  (480x640)    │          │  └─ LLM 意图分析   │
└───────────────┘          └──────────────────┘
```

## 适配说明

本项目基于 [AR-Mahjong-Assistant](https://github.com) 改编，针对 Rokid AI 眼镜做了以下适配：

| 原项目 (雷鸟 X3 Pro) | 本项目 (Rokid AI) |
|---|---|
| RayNeo Mercury SDK | 纯 Android API |
| 横屏双眼渲染 | 竖屏 480x640 单屏 |
| 镜腿触控手势 | 触控板 D-Pad 按键 |
| BaseMirrorActivity | AppCompatActivity |
| compileSdk 36 | compileSdk 34, targetSdk 32 |

## 致谢 / Credits

本项目基于以下开源项目改编：

- **[LYiHub/AR-Mahjong-Assistant-preview](https://github.com/LYiHub/AR-Mahjong-Assistant-preview)** — 原始 AR 麻将助手项目，面向 RayNeo 雷鸟 X3 Pro 眼镜设计，本项目核心架构、YOLO 模型、牌效引擎、服务端代码均源自此项目。原作者：LYiHub。
- **[SYSTRAN/faster-whisper](https://github.com/SYSTRAN/faster-whisper)** — 离线语音转文字
- **[FluffyStuff/riichi-mahjong-tiles](https://github.com/FluffyStuff/riichi-mahjong-tiles)** — 麻将图片素材
- Jon Chan 提供的 Mahjong Dataset（发布于 Roboflow Universe）

## License

MIT License

> 注意：本项目为学习和研究目的改编自开源项目。原项目 [LYiHub/AR-Mahjong-Assistant-preview](https://github.com/LYiHub/AR-Mahjong-Assistant-preview) 版权归原作者所有。
