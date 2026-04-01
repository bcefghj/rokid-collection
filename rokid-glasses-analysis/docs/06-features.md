# 功能实现细节

> 每个核心功能的技术实现原理

---

## 拍照

### 触发方式

| 方式 | 触发信号 | 说明 |
|------|---------|------|
| 功能键短按 | `MSG_TAKE_PICTURE` | 默认配置 |
| 语音命令 | `OA_PaiZhao` | 说"拍照" |
| 手机远程 | `TakePhotoAction` | 手机端触发 |
| AI 辅助 | `TYPE_PICTURE_AI_ASSIST` | AI 自动拍照 |

### 完整流程

```
触发拍照
  ↓
前置检查:
  ├── 存储空间是否充足？
  ├── 电量是否 > 10%？
  └── 相机环境是否正常？（没有其他应用在用相机）
  ↓
openCamera() → 打开 Sony IMX681 摄像头
  ↓
doTakePicture() → 拍照
  ↓
虹软 ArcSoft SDK 后处理:
  ├── HDR 高动态范围处理
  ├── 低光场景增强
  ├── 运动检测与防抖
  ├── 光学畸变校正
  └── 遮挡检测 (阈值 < 0.1f, 手指挡住镜头就不拍)
  ↓
保存到 /sdcard/DCIM/Camera/
  ↓
生成缩略图
  ↓
通过蓝牙发送缩略图到手机 (Med.Med_NewThumbnail)
```

### 摄像头参数

| 项目 | 值 |
|------|-----|
| 传感器 | Sony IMX681 |
| HAL 版本 | v3.7 |
| 支持帧率 | 15/24/30/60 fps |
| 调校版本 | 4.1.4 |
| 闪光灯 | 无 |

---

## 录像

### 视频参数

| 项目 | 值 |
|------|-----|
| 分辨率 | 480×640 |
| 编码器 | H.264 |
| 码率 | 4 Mbps |
| 帧率 | 30 fps |
| I 帧间隔 | 10 秒 |
| 最长时间 | 600 秒 (10 分钟) |

### 触发方式

| 方式 | 说明 |
|------|------|
| 功能键长按 | 默认配置 |
| 语音 "录像" | 开始录像 |
| 语音 "结束录像" | 停止录像 |
| 手机远程 | 通过蓝牙命令 |

### 限制条件

- 最小存储空间：400 MB
- 最低电量：10%
- 超长按功能键可强制停止

### 存储路径

`/sdcard/Movies/Camera/`

---

## 录音

### 音频参数

| 项目 | 值 |
|------|-----|
| 编码器 | AAC |
| 码率 | 196 kbps |
| 采样率 | 16 kHz |
| 声道 | 单声道 |

### 存储路径

`/sdcard/Recordings/`

---

## 翻译

### 架构

翻译是**眼镜+手机协同**的典型功能：

```
┌──────────────┐                    ┌──────────────┐
│   Rokid 眼镜  │                    │   手机 App    │
│              │                    │              │
│ 8通道麦克风   │ ── 蓝牙传输音频 ──→│ 微软 Azure   │
│  收音        │                    │ Speech SDK   │
│              │                    │ 语音识别      │
│              │                    │     ↓        │
│              │                    │ 翻译引擎      │
│              │                    │     ↓        │
│ 屏幕显示     │ ←── 蓝牙返回结果 ──│ 翻译结果      │
│ 翻译结果     │                    │              │
└──────────────┘                    └──────────────┘
```

### 通信命令

| 命令 | 说明 |
|------|------|
| `Trans.Trans_ResponseChangeSceneId` | 翻译场景切换 |
| 眼镜端场景 Key | `translate` |

### 语音场景

| 常量 | 值 | 场景 |
|------|-----|------|
| `AGT_SPEECH_SCENE_TRANSLATION` | 1 | 翻译模式 |
| `AGT_SPEECH_SCENE_CALL` | 2 | 通话模式 |
| `AGT_SPEECH_SCENE_CONFERENCE` | 3 | 会议模式 |

### 手机端

| Activity | 功能 |
|----------|------|
| `RealTimeTranslationActivity` | 实时翻译 |
| `TranslationSettingsActivity` | 翻译设置 |

---

## 导航

### 技术方案

| 地区 | 方案 |
|------|------|
| 国内 | 高德地图 SDK |
| 海外 | 海外导航模块 (`NavigationOverseaPageActivity`) |

### 区分逻辑

```java
getNaviActivityClass() → appIsOverseas()
  ? NavigationOverseaPageActivity   // 海外
  : NavigationPageActivity          // 国内 (高德)
```

### 工作流程

```
手机计算路线 (高德地图 SDK)
  ↓
导航指令通过蓝牙 (Nav.* 命令) 发送到眼镜
  ↓
眼镜显示:
  ├── 转向箭头
  ├── 剩余距离
  └── 路线提示
```

### 启动参数

```java
NaviStart event → intent.putExtra("destination", destination)
                → intent.putExtra("naviType", type)
                → intent.putExtra("locPermissionTip", tip)
```

### 高德地图 API Key

`6f7762ff2284d88f5c32a8484bb4b710`

---

## AI 视觉助手 (慧眼)

### 工作流程

```
眼镜摄像头
  ↓ 拍照 或 视频帧
通过蓝牙传到手机
  ↓
手机端 AI 推理:
  ├── iOS: onnxruntime (本地推理) + swift-transformers (NLP)
  └── Android: AI 功能被加壳保护
  ↓
识别结果返回眼镜显示
```

### 相关命令

| 命令 | 说明 |
|------|------|
| `Ai.KeyDown` | AI 按键触发 |
| `Ai.Ai_TakePhoto` | AI 拍照 |
| `Ai.Ai_TakePhoto_Over` | AI 拍照完成 |
| `ARTC.OnPushVideoFrame` | 推送视频帧到手机 |
| `ARTC.NotifyUserSpeaking` | 用户正在说话 |

### 场景 Key

| Key | 功能 |
|-----|------|
| `ai_chat` | AI 聊天对话 |
| `ai_assist` | AI 助手快捷操作 |

---

## 支付系统

### 5 种支付渠道

| 渠道 | 场景Key | 眼镜端APK | 核心服务 | 实现方式 |
|------|---------|----------|---------|---------|
| 支付宝(传统) | `payment` | 96MB | `Glass2PayService` | 刷脸/扫码支付 |
| 蚂蚁支付(新) | `ant_pay` | 48+19MB | `GPassService` | 新版支付 |
| 微信支付 | `weixin_pay` | — | `PhoneWxPayService` | 手机端代理 |
| 京东支付 | `jd_pay` | 28MB | `SDKService` | JoyGo SDK |
| 城市导览 | `city_guide` | — | `AliCityGuideActivity` | 手机端 |

### 注意事项

- 支付宝和蚂蚁支付**互斥**（初始化一个会销毁另一个）
- 所有支付服务由 `PaymentService`（AssistServer 的子服务）统一调度

### 代理通信机制

```
手机 App ←→ 蓝牙 GATT (Pay.* 命令)
  ↓
PaymentProxyManager
  ↓
各个 PayCenter
  ├── AlipayPayCenter (支付宝)
  ├── AntPayCenter (蚂蚁)
  ├── WxPayCenter (微信)
  └── JdPayCenter (京东)
```

---

## 屏幕录制/投屏

### 三种模式

| 模式 | 值 | 说明 | 参数 |
|------|-----|------|------|
| RECORD | 1 | 本地录屏为 MP4 | 480×640, H.264, 4Mbps |
| STREAM | 2 | 仅编码不存储 | 用于投屏 |
| LIVE_STREAM | 3 | RTMP 实时直播 | 推送到流媒体服务器 |

### 捕获方式

使用 `SurfaceControl.createDisplay()` 创建虚拟显示，投射主屏画面。

---

## 直播推流

### RokidSpriteLive 架构

```
摄像头 → CameraRecorder → VideoEncoder (H.264)
                                ↓
                         RtmpSenderImpl → FLV 封装 → RTMP 服务器
                                ↑
麦克风 → AudioRecorder → AudioEncoder (AAC)
```

### 第三方直播集成

- 抖音直播：通过字节跳动 Zeus 插件系统集成
- 手机端 `CXRLinkService` 提供直播连接服务

---

## 提词器

### 工作方式

```
手机端: PrompterMainActivity → PrompterEditActivity
  ↓ 编辑提词内容
蓝牙 GATT 发送到眼镜
  ↓
眼镜端: WordTipsPageActivity
  ↓ 480×640 屏幕显示文字内容
```

场景 Key：`word_tips`

---

## 音乐控制

### 协议

| 协议 | 用途 |
|------|------|
| A2DP | 蓝牙音频传输（手机音乐→眼镜扬声器） |
| HFP | 通话音频 |
| AVRCP | 媒体控制（播放/暂停/切歌） |

### 控制方式

| 方式 | 说明 |
|------|------|
| 语音命令 | "播放音乐"/"暂停播放"/"下一首"/"上一首" |
| 蓝牙命令 | `Music.*` 系列 |
| 手机端 App | `QQMusicCallbackActivity` |
| 眼镜端显示 | `MusicPageActivity` + `MusicStatusFragment` (歌词) |

### 支持的蓝牙音频编码

SBC, AAC, aptX, aptX HD, LDAC, CELT, aptX Adaptive, aptX TWS+, LC3 (BT LE Audio)

---

## OTA 升级

### 机制

Android A/B 分区更新引擎 (`UpdateEngine`)

### 流程

```
1. 检查更新: ota.rokid.com/v1/extended/ota/check
  ↓ (MD5 签名认证)
2. 下载固件: 断点续传, 重试 3 次, MD5 校验
  ↓
3. 解压: payload.bin from update.zip
  ↓
4. 安装: UpdateEngine.applyPayload() → A/B 分区更新
  ↓
5. 重启
```

### 电量要求

| 阶段 | 最低电量 |
|------|---------|
| 下载 | 30% |
| 安装 | 70% (未充电时) |

### 远程 OTA

支持通过手机蓝牙触发眼镜 OTA：`sprite_cmd_start_ota`
