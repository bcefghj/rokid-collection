# 完整应用清单

> 系统里到底装了哪些 App？

---

## Rokid 核心应用 (7 个)

### RokidSpriteAssistServer — AI 核心大脑

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.os.sprite.assistserver` |
| **大小** | 224 MB (最大的应用) |
| **位置** | `/product/app/RokidSpriteAssistServer/` |
| **UID** | 1000 (system) |
| **进程** | 主进程 + `:tts` 子进程 |

**包含的服务：**

| 服务 | 职责 |
|------|------|
| `MasterAssistService` | 总调度中心，协调所有功能 |
| `InstructService` | 离线语音命令识别引擎 |
| `SpriteMediaService` | 拍照、录像、录音管理 |
| `PaymentService` | 统一调度 5 种支付渠道 |
| `TtsService` | 文字转语音（独立 :tts 进程） |
| `RokidBluetoothService` | 蓝牙管理（内含 15 个子 Manager） |
| `WebServerService` | 内置 Web 服务器（端口 8848） |
| `SystemFuncService` | 系统级功能服务 |
| `SpriteWifiService` | WiFi 管理 |
| `JsAiService` | JavaScript AI 引擎 |

### RokidSpriteLauncher — 主界面

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.os.sprite.launcher` |
| **大小** | 28 MB |
| **位置** | `/product/app/RokidSpriteLauncher/` |
| **UID** | 1000 (system) |

**主要页面：**

| Fragment/Activity | 功能 |
|-------------------|------|
| `CenterFragment` | 中心主页（时间、日期、计划卡片） |
| `MsgListFragment` | 消息列表 |
| `AppListFragment` | 应用列表 |
| `TranslatePageActivity` | 翻译页 |
| `NavigationPageActivity` | 导航页 |
| `ChatPageActivity` | AI 聊天 |
| `AudioPageActivity` | 录音页 |
| `LivePageActivity` | 直播页 |
| `MusicPageActivity` | 音乐页 |
| `WordTipsPageActivity` | 提词器 |

### RokidSpriteLive — 直播推流

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.os.sprite.live` |
| **大小** | 12 MB |
| **位置** | `/product/app/RokidSpriteLive/` |
| **核心服务** | `LiveService` |
| **功能** | RTMP 实时直播推流 |

### RokidSysConfig — 系统配置

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.sysconfig` |
| **大小** | < 1 MB |
| **位置** | `/system/priv-app/RokidSysConfig/` (特权应用) |
| **核心服务** | `ConfigService` |

**核心功能：**
- PsensorObserver：佩戴检测（通过 UEvent 监控 I2C extcon 设备）
- LED 灯光控制（通过 `LightsCtrl` HAL）
- 折叠关机策略
- 传感器配置

### CXRService — 蓝牙通信桥梁

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.cxrservice` |
| **大小** | < 1 MB |
| **位置** | `/system/app/CXRService/` |
| **版本** | 1.109 |
| **功能** | 管理所有蓝牙/MFi 通信 |

### RokidOtaUpgrade — OTA 升级

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.glass.ota` |
| **大小** | 3.4 MB |
| **位置** | `/product/app/RokidOtaUpgrade/` |

**服务：**
- `CheckService` — 检查更新
- `OtaService` — 执行更新
- `DownloadService` — 下载固件

### RokidScreenRecord — 屏幕录制

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.os.master.screenstream` |
| **大小** | 11 MB |
| **位置** | `/product/app/RokidScreenRecord/` |
| **功能** | 录屏 / 投屏 / 直播推流 |

---

## 支付应用 (4 个)

| 应用 | 包名 | 大小 | 核心服务 |
|------|------|------|---------|
| **支付宝 (眼镜版)** | `com.eg.android.AlipayGGlasses` | 96 MB | `Glass2PayService` (刷脸/扫码) |
| **蚂蚁集团** | `com.antgroup.glasses` | 48 MB | `GPassService` (通行证) |
| **蚂蚁支付** | `com.iap.mobile.ar_pay` | 19 MB | AR 支付 |
| **京东支付** | `com.jd.jr.joyaibuy` | 28 MB | `SDKService` (JoyGo) |

---

## 手机端 App

### Android 版

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.sprite.aiapp` |
| **版本** | 1.4.0.0312 |
| **大小** | 391 MB |
| **保护** | 网易易盾 (NetEase NIS) 加壳 |

**集成的第三方服务：**

| 服务 | 用途 |
|------|------|
| 高德地图 | 导航 |
| 微软 Azure Speech | 语音识别/翻译 |
| 腾讯 TPNS | 推送 |
| 华为 HMS / Vivo / 小米 / OPPO Push | 各厂商推送 |
| 字节跳动 Zeus | 抖音直播 |
| 京东 JoyGo | 京东支付 |
| Google ML Kit | 语言识别 |
| 微信 SDK | 登录/分享 |
| 阿里 RTC | 实时通信 |
| 支付宝扫码 SDK | 扫码支付 (AppKey: `AR_EYE_ANDROID`) |

**主要功能模块：**

| 功能 | Activity/Service |
|------|-----------------|
| 蓝牙连接 | `ConnectCompanionDeviceService` |
| AI 服务 | `AiService` (前台服务) |
| 翻译 | `RealTimeTranslationActivity` |
| 导航 | `MapActivity`, `NavigationActivity` |
| 提词器 | `PrompterMainActivity` |
| 笔记 | `NotesMainActivity` |
| 音乐 | `QQMusicCallbackActivity` |
| 支付 | `RokidGlassActivity`, `Glass2PayService` |
| 直播 | `CXRLinkService` |
| 社区 | `PublishPostActivity`, `AgentStoreActivity` |
| OTA | `SettingsOtaActivity` |
| 通知转发 | `MessageNotificationListenerService` |

### iOS 版

| 项目 | 详情 |
|------|------|
| **包名** | `com.rokid.rokidglasses` |
| **版本** | 1.3.1 (Build 202603061524) |
| **大小** | 624 MB |
| **最低 iOS** | 18.0 |

**通信方式：**
- Apple MFI External Accessory (`com.rokid.aiglasses`, `com.rokid.bolonglasses`)
- BLE Central 模式
- 后台支持：bluetooth-central, external-accessory, audio, location

**URL Scheme：**

| Scheme | 用途 |
|--------|------|
| `rokidai://` | 深链接 |
| `wx**************` | 微信 |
| `joygorokid` | 京东 |
| `awbkus8yvpwfz8tx` | 抖音 |

**集成的 Framework：**

| Framework | 用途 |
|-----------|------|
| `MicrosoftCognitiveServicesSpeech` | 微软 Azure 语音 |
| `alivcffmpeg` + `AliVCSDK_ARTC` | 阿里云 RTC |
| `onnxruntime` | 本地 AI 推理 |
| `RKEncrypt` | Rokid 加密 |
| `nuisdk` | 阿里 NUI 语音 |
| `AMapNavi.bundle` | 高德导航 |
| `swift-transformers` | NLP Tokenizer |
| `RGSDK.bundle` | Rokid Glass SDK |

---

## 其他系统标准应用 (83 个)

包含 Android 12 标准系统应用：
- Settings (设置)
- Bluetooth (蓝牙)
- Camera2 (相机)
- 各种 ContentProvider
- SystemUI
- 等等...
