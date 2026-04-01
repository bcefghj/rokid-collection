# 语音系统解析

> 从唤醒词"若琪"到执行命令的完整流程

---

## 概述

Rokid AI 眼镜的语音系统分为三个部分：

1. **离线语音识别** — 在眼镜本地运行，不需要网络
2. **本地 TTS (文字转语音)** — 眼镜自己"说话"
3. **在线语音识别** — 通过手机调用云端服务

---

## 离线语音识别

### 引擎信息

| 项目 | 详情 |
|------|------|
| **引擎** | Rokid 自研 native 库 |
| **库文件** | `librt_instruct.so` |
| **JNI 接口** | `RtInstructSdk` |
| **输入** | 8 通道麦克风阵列 |
| **采样率** | 16kHz |
| **网络需求** | 完全离线，不需要联网 |
| **语言** | 中文 (`zh`) / 英文 (`en`) |

### 完整的 19 个离线语音命令

#### 唤醒词（3 个）

| 命令 ID | 你说的话 | 说明 |
|---------|---------|------|
| `OA_Ruoqi` | **"若琪"** | 主唤醒词 |
| `OA_Leqi` | **"乐琪"** | 别名唤醒词 |
| `OA_HiBaoLong` | **"嗨暴龙"** | 别名唤醒词 |

#### 电话控制（1 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_JieDianHua` | **"接电话"** | 接听来电 |

#### 音乐控制（4 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_XiaYiShou` | **"下一首"** | 切到下一首歌 |
| `OA_ShangYiShou` | **"上一首"** | 切到上一首歌 |
| `OA_BoFangYinYue` | **"播放音乐"** | 开始播放音乐 |
| `OA_ZanTingBoFang` | **"暂停播放"** | 暂停当前播放 |

#### 音量控制（2 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_ShengYinDaYiDian` | **"声音大一点"** | 增大音量 |
| `OA_ShengYinXiaoYiDian` | **"声音小一点"** | 减小音量 |

#### 亮度控制（2 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_LiangYiDian` | **"亮一点"** | 增大屏幕亮度 |
| `OA_AnYiDian` | **"暗一点"** | 减小屏幕亮度 |

#### 拍摄控制（5 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_PaiZhao` | **"拍照"** | 拍一张照片 |
| `OA_LuYin` | **"录音"** | 开始录音 |
| `OA_JieShuLuYin` | **"结束录音"** | 停止录音 |
| `OA_LuXiang` | **"录像"** | 开始录像 |
| `OA_JieShuLuXiang` | **"结束录像"** | 停止录像 |

#### 系统功能（2 个）

| 命令 ID | 你说的话 | 效果 |
|---------|---------|------|
| `OA_DangQianDianLiang` | **"当前电量"** | 语音播报电量百分比 |
| `OA_TuiChu` | **"退出"** | 退出当前场景 |

### 语音识别完整流程

```
第1步：声音采集
  8通道麦克风阵列同时收音
  ↓ 16kHz 采样率，波束成形，远场拾音

第2步：本地引擎分析
  librt_instruct.so (Native 引擎，通过 JNI 调用)
  ↓ 实时匹配预设的 19 个关键词

第3步：命令回调
  匹配成功 → onInstructCall(instructId)
  ↓

第4步：命令查表
  OffLineManager.handleInstruct()
  ↓ 在 actionMap 中查找对应的 Action

第5步：执行操作
  actionMap.get(cmdId).handleAction()
  ↓ 比如拍照、播放音乐、调音量...
```

### 语音控制开关

- **开启：** `settings_voice_control = "open"`
- **关闭：** `settings_voice_control = "close"`

### 语言切换

- 系统语言为中文 (`zh_cn`) → 语音引擎使用 `"zh"` 模式
- 其他语言 → 使用 `"en"` 模式

---

## 本地 TTS (文字转语音)

### 引擎信息

| 项目 | 详情 |
|------|------|
| **引擎** | Rokid 自研 native 库 |
| **库文件** | `librfm-tts-jni.so` |
| **版本** | 3.0.1.2 |
| **运行进程** | `:tts` (独立进程) |
| **AIDL 接口** | `ITtsServer` |
| **音频格式** | PCM 流式输出 |

### 工作流程

```
第1步：初始化
  复制 assets/resource/ → appFiles/resource/
  ↓
  TtsNative.init(configDir, serialNo, "Glasses", params)

第2步：文字输入
  调用 TtsNative.predict(text, timestamp)
  ↓
  返回 PCM 音频数据

第3步：音频播放
  通过 StreamAudioPlayer 流式播放
```

### 为什么用独立进程？

TTS 引擎运行在独立的 `:tts` 进程中，这样做的好处是：
- 即使主应用崩溃，TTS 也不受影响
- TTS 处理不会阻塞主进程的其他功能
- 内存独立管理，不影响主进程

### 性能指标

TTS 系统会监控以下性能数据：
- `firstPacketRt` — 首包响应时间（用户等多久才开始听到声音）
- `rtf` — 实时因子（合成速度与播放速度的比值）
- `acousticRt` — 声学模型处理时间
- `vocoderRt` — 声码器时间

### 双通道 TTS

| 通道 | 方式 | 用途 |
|------|------|------|
| **本地 TTS** | 眼镜端 `librfm-tts-jni.so` 直接合成播放 | 眼镜自己说话 |
| **远程 TTS** | 通过蓝牙 GATT 发送到手机端播放 | 让手机说话 |

远程 TTS 使用命令：`Sys.Tts_SendPlayTts`

---

## 在线语音识别

### 翻译场景

翻译功能需要更高精度的语音识别，使用**微软 Azure 语音服务**：

```
眼镜麦克风收音 → 蓝牙传到手机
  ↓
手机调用 MicrosoftCognitiveServicesSpeech SDK
  ↓
识别结果返回
```

### AI 助手语音识别

AI 助手支持两种模式：
- **本地模式** — 使用眼镜端 AI
- **手机模式** — 通过蓝牙调用手机端语音助手

### 语音场景常量

| 常量 | 值 | 场景 |
|------|-----|------|
| `AGT_SPEECH_SCENE_TRANSLATION` | 1 | 翻译模式 |
| `AGT_SPEECH_SCENE_CALL` | 2 | 通话模式 |
| `AGT_SPEECH_SCENE_CONFERENCE` | 3 | 会议模式 |
