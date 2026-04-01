# 雷鸟 X3 Pro 应用深度解析

> 基于 ADB 实机数据分析，深入每一个应用的组件、权限、交互关系。

---

## 一、应用生态全景图

```
┌────────────────────────────────────────────────────────────────────┐
│                        用户直接使用的应用                           │
│                                                                    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
│  │ 相机  │ │ 导航  │ │ 翻译  │ │提词器 │ │ 笔记 │ │ 音乐  │ │ 图库  │ │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ │
│     │        │        │        │        │        │        │      │
│  ┌──┴────────┴────────┴────────┴────────┴────────┴────────┴───┐  │
│  │                  Mercury Launcher（主桌面）                  │  │
│  │   启动所有应用 / 管理锁屏 / 蓝牙手机连接 / 高德定位          │  │
│  └─────┬──────────────────┬──────────────────┬────────────────┘  │
│        │                  │                  │                    │
│  ┌─────▼─────┐     ┌─────▼─────┐     ┌──────▼──────┐            │
│  │XR Runtime │◄───►│AI Runtime │     │ AI Speech   │            │
│  │ 传感器/相机│     │ AI推理引擎 │     │ 语音助手     │            │
│  │ 3DoF追踪  │     │ 视觉/语音  │     │ 唤醒词监听   │            │
│  └─────┬─────┘     └─────┬─────┘     └──────┬──────┘            │
│        │                 │                   │                    │
│  ┌─────▼─────────────────▼───────────────────▼─────┐             │
│  │                Live AI（实时AI处理）              │             │
│  │           实时翻译 / 实时字幕 / AI视觉            │             │
│  └─────────────────────────────────────────────────┘             │
│                                                                    │
│  ┌────────────────────┐  ┌────────────────────────┐               │
│  │  App Container     │  │  第三方应用              │               │
│  │  运行手机Android应用│  │  支付宝/微信/B站/小红书  │               │
│  └────────────────────┘  └────────────────────────┘               │
└────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心应用逐个深入解析

---

### 2.1 Mercury Launcher（主桌面）

**一句话**：整个眼镜的"管家"，不只是一个桌面，它管着大部分系统级功能。

| 项目 | 值 |
|------|-----|
| 包名 | `com.ffalconxr.mercury.launcher` |
| 版本 | 2026.02.04.06 (code: 2026020406) |
| 安装位置 | `/system_ext/priv-app/RayNeoLauncher/` |
| 运行权限 | system (UID=1000)，最高系统权限 |
| 运行内存 | ~234MB |
| targetSdk | 31 (Android 12) |

#### 组件清单

**Activity（界面页面）：**

| Activity | 作用 |
|----------|------|
| `MainActivity` | 主界面——你戴上眼镜看到的第一个画面 |
| `ProtocolActivity` | 协议/深链接处理——从其他应用跳转进来 |
| `WizardUserAgreementActivity` | 首次使用的用户协议向导 |
| `WizardPrivacyActivity` | 隐私政策向导 |
| `NetConfigActivity` | WiFi 配网向导 |
| `EnterActivity` | 开机引导入口 |

**Service（后台服务）：**

| Service | 作用 |
|---------|------|
| `XRDeviceManagerService` | **设备管理核心**——管理眼镜的硬件状态、外设连接 |
| `InputControlService` | **触控板输入控制**——拦截和分发触控板手势 |
| `QuickShotService` | **快拍服务**——快速拍照功能 |
| `GnirehtetService` | **VPN 反向网络共享**——通过 USB 从电脑共享网络给眼镜 |
| `LogService` | 日志收集服务——收集系统运行日志用于诊断 |
| `AuthenticationService` | 雷鸟账号认证服务 |
| `APSService`（高德SDK） | **高德定位服务**——为导航和位置功能提供 GPS 数据 |
| `MessengerUtils$ServerService` | 进程间通信服务——Launcher 与其他应用之间的消息通道 |

**BroadcastReceiver（广播接收器）：**

| Receiver | 监听什么 |
|----------|---------|
| `SystemUIReceiver` | **系统UI事件**——电量变化、USB连接、时区更改等 |
| `InstallResultReceiver` | 应用安装/卸载结果 |
| `IosBroadCastReceiver` | iOS WiFi 直连广播（用于手机互联） |
| 多个 WorkManager Proxy | 后台任务调度——定时任务、充电检测、网络状态等 |

**ContentProvider（数据共享）：**

| Provider | 提供什么数据 |
|----------|------------|
| `MercuryInteractProvider` | **核心数据交互接口**——其他应用通过它和 Launcher 通信 |
| `RecordNotifyProvider` | 录制通知数据 |
| `TodoNotifyProvider` | 待办事项通知数据 |
| `ConnectContentProvider` | 手机连接传输模式数据 |
| `MlKitInitProvider` | Google ML Kit 初始化（用于机器学习功能） |

#### 权限解读（60+ 个权限！）

Launcher 拥有极其广泛的系统权限，说明它是整个系统的控制中心：

```
📱 硬件控制
├── CAMERA（相机）
├── RECORD_AUDIO（录音）
├── CAPTURE_AUDIO_OUTPUT（捕获音频输出）
├── BLUETOOTH / BLUETOOTH_ADMIN / BLUETOOTH_CONNECT / BLUETOOTH_SCAN（蓝牙全权限）
├── MONITOR_INPUT（监控输入事件）
└── INJECT_EVENTS（注入输入事件——可以模拟触控操作）

⚙️ 系统管理
├── REBOOT / SHUTDOWN / RECOVERY（重启/关机/恢复模式）
├── DEVICE_POWER（电源控制）
├── SET_TIME / SET_TIME_ZONE（设置时间/时区）
├── WRITE_SETTINGS / WRITE_SECURE_SETTINGS（修改所有系统设置）
├── INSTALL_PACKAGES / DELETE_PACKAGES（安装/卸载应用）
├── CLEAR_APP_USER_DATA（清除应用数据）
├── MANAGE_DEBUGGING（管理ADB调试）
└── CHANGE_CONFIGURATION（修改系统配置）

🌐 网络
├── INTERNET / ACCESS_NETWORK_STATE / CHANGE_NETWORK_STATE
├── ACCESS_WIFI_STATE / CHANGE_WIFI_STATE / MANAGE_WIFI_COUNTRY_CODE
└── CHANGE_WIFI_MULTICAST_STATE

📍 位置
├── ACCESS_FINE_LOCATION（精确定位）
├── ACCESS_COARSE_LOCATION（粗略定位）
└── ACCESS_LOCATION_EXTRA_COMMANDS

📁 存储
├── READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE
└── MOUNT_UNMOUNT_FILESYSTEMS（挂载文件系统）

📞 通信
├── READ_PHONE_STATE / READ_PRIVILEGED_PHONE_STATE
├── ANSWER_PHONE_CALLS / MANAGE_OWN_CALLS
├── READ_CALL_LOG / READ_CONTACTS
└── GET_ACCOUNTS / ACCOUNT_MANAGER
```

#### 关键运行时行为

从运行日志看到 Launcher 实际在做这些事情：

```
1. 启动后立即绑定 XR Runtime 的 MonitorService
   → 获取传感器数据、相机状态
   
2. 启动后绑定 Media 的 MediaCoreService
   → 控制音乐播放、音量

3. 启动高德定位 APSService
   → 持续获取位置信息

4. HeadTrigger 组件持续监听
   → 使用重力传感器 + 抬手检测传感器
   → 戴上眼镜 → 亮屏，摘下 → 息屏

5. SystemUIReceiver 持续监听系统事件
   → 电池变化、蓝牙连接、USB状态等
```

---

### 2.2 XR Runtime（AR 运行时核心）

**一句话**：所有 AR 功能的基石，没有它就没有 AR。

| 项目 | 值 |
|------|-----|
| 包名 | `com.rayneo.xr.runtime` |
| 版本 | 1.1.7.9 (code: 1010709) |
| 安装位置 | `/system_ext/priv-app/RayNeoRuntime/` |
| 运行内存 | ~60MB |
| targetSdk | 32 |
| minSdk | 29 (Android 10) |

#### 核心组件

**MonitorService** —— 这是 XR Runtime 最重要的服务：

```
运行状态：前台服务（isForeground=true）
通知频道：FFalconXRNotifications
通知类型：category=service（静默服务通知）

绑定关系（谁在用它）：
├── com.rayneo.airuntime     ← AI 引擎
├── com.ffalconxr.mercury.launcher  ← 主桌面
├── com.rayneo.live.ai       ← Live AI
└── com.rayneo.record        ← 录音/录屏
```

这说明 **MonitorService 是整个 AR 系统的中枢**——4 个核心应用都绑定了它。它提供：
- 传感器数据聚合（加速度计 + 陀螺仪 + 磁力计）
- 3DoF 头部追踪计算
- 相机预览流分发
- AR 渲染管线管理

**XRProvider** —— ContentProvider，对外暴露 XR 数据：
- Authority: `com.rayneo.xr.provider`
- 其他应用通过它查询 XR 状态

**权限**（精简但关键）：

```
FOREGROUND_SERVICE        → 常驻前台运行
CAMERA                    → 控制摄像头
HIGH_SAMPLING_RATE_SENSORS → 高速率传感器采样（415Hz）
MANAGE_EXTERNAL_STORAGE   → 管理存储
READ/WRITE_EXTERNAL_STORAGE
```

**Intent Filter 中有 USB 设备事件**：
```
USB_DEVICE_ATTACHED / USB_DEVICE_DETACHED
```
说明 XR Runtime 还监听 USB 外设的插拔（可能用于连接外部追踪设备或控制器）。

---

### 2.3 AI Runtime（AI 推理引擎）

**一句话**：眼镜上所有 AI 能力的大脑。

| 项目 | 值 |
|------|-----|
| 包名 | `com.rayneo.airuntime` |
| 版本 | 0.2.32 (code: 232) |
| 安装位置 | `/system_ext/priv-app/RayNeoAIRuntime/` |
| 运行权限 | system (UID=1000) |
| 运行内存 | ~110MB |
| targetSdk | 32 |

#### 它能做什么

从权限列表可以推断它的能力范围：

```
┌─────────────────────────────────────────┐
│          AI Runtime 能力矩阵            │
├─────────────────────────────────────────┤
│                                         │
│  👁️ 视觉 AI                             │
│  ├── CAMERA → 实时获取相机画面          │
│  └── 图像识别、OCR、人脸检测            │
│                                         │
│  🎤 语音 AI                             │
│  ├── RECORD_AUDIO → 录音               │
│  ├── CAPTURE_AUDIO_OUTPUT → 捕获音频    │
│  ├── MODIFY_AUDIO_SETTINGS → 控制音频   │
│  └── 语音识别、TTS 语音合成             │
│                                         │
│  📍 位置感知                            │
│  ├── ACCESS_FINE_LOCATION              │
│  └── 场景识别（室内/户外）              │
│                                         │
│  🌐 网络 AI                             │
│  ├── INTERNET → 云端 AI 调用            │
│  ├── ACCESS_NETWORK_STATE              │
│  └── 本地不够时上云端推理               │
│                                         │
│  📱 系统集成                            │
│  ├── BLUETOOTH_CONNECT → 蓝牙设备交互   │
│  ├── RECEIVE_BOOT_COMPLETED → 开机启动  │
│  └── READ_PRIVILEGED_PHONE_STATE       │
└─────────────────────────────────────────┘
```

#### 运行行为

- **开机自启**：通过 `BootBroadcastReceiver` 监听 `BOOT_COMPLETED` 和 `LOCKED_BOOT_COMPLETED`
- **绑定 XR Runtime**：启动后立即连接 MonitorService，获取相机和传感器数据
- **提供文件共享**：`AiRuntimeFileProvider` 允许其他应用读取 AI 处理结果

---

### 2.4 AI Speech（语音助手系统）

**一句话**：始终在后台监听唤醒词，是最复杂的应用之一。

| 项目 | 值 |
|------|-----|
| 包名 | `com.rayneo.aispeech` |
| 版本 | 2026.01.29.10 |
| 安装位置 | `/system_ext/priv-app/RayNeoAISpeech/` |
| 运行权限 | system (UID=1000) |
| targetSdk | **34** (Android 14！最高的) |

#### 三进程架构

AI Speech 是**唯一使用三个独立进程**的应用：

```
进程1: com.rayneo.aispeech（主服务）
  ├── SpeechService    → 核心语音处理
  ├── SpeechProvider   → 语音数据接口
  └── 内存 ~98MB

进程2: com.rayneo.aispeech:interactor（交互进程）
  ├── 处理语音命令的执行
  ├── 与其他应用交互
  └── 内存 ~67MB

进程3: com.rayneo.aispeech:rhotword（唤醒词检测）
  ├── RayNeoHotwordDetectionService
  ├── 始终在后台运行
  ├── 监听麦克风等待唤醒词
  └── 独立进程=即使主服务崩溃也不影响唤醒
```

#### 组件详解

**Activity（界面）：**

| Activity | 作用 |
|----------|------|
| `MainActivity` | 语音助手主界面 |
| `CommandTestActivity` | 语音命令测试（开发调试用） |
| `AIEngineTestActivity` | AI 引擎测试（开发调试用） |
| `SettingsActivity` | 语音助手设置页 |

**Service（后台服务）：**

| Service | 作用 |
|---------|------|
| `SpeechService` | **语音处理核心**——被 Launcher 调用，处理语音识别和命令执行 |
| `RayNeoRecognitionService` | **标准语音识别服务**——实现 Android 标准 `RecognitionService` 接口 |
| `RayNeoVoiceInteractionService` | **语音交互服务**——实现 Android 标准 `VoiceInteractionService`，就像手机上的 Google Assistant |

**特殊权限**（语音助手独有的）：

```
RECORD_AUDIO              → 录音
RECORD_BACKGROUND_AUDIO   → 后台录音（关键！屏幕关了也能录）
CAPTURE_AUDIO_HOTWORD     → 捕获唤醒词音频
MANAGE_HOTWORD_DETECTION  → 管理唤醒词检测
MANAGE_VOICE_KEYPHRASES   → 管理语音关键短语
INTERACT_ACROSS_USERS     → 跨用户交互
SYSTEM_ALERT_WINDOW       → 悬浮窗（语音助手界面浮在其他应用上方）
```

#### 语音交互流程

```
用户说 "你好雷鸟"
    ↓
rhotword 进程（唤醒词检测）
├── 始终在后台监听麦克风
├── 使用低功耗模式（可能借助 ADSP）
├── 检测到唤醒词后...
    ↓
SpeechService（主服务）被唤醒
├── 开始正式录音和语音识别
├── 将语音转为文字
    ↓
interactor 进程
├── 解析用户意图
├── 执行对应操作
│   ├── "打开相机" → 启动 com.leiniao.camera
│   ├── "拍照" → 发送指令给相机
│   ├── "播放音乐" → 控制 com.rayneo.media
│   ├── "导航到xxx" → 启动 com.ffalcon.navigation
│   └── 其他 AI 问答 → 通过 AI Runtime 处理
    ↓
结果通过语音/文字反馈给用户
```

---

### 2.5 Live AI（实时 AI 处理）

| 项目 | 值 |
|------|-----|
| 包名 | `com.rayneo.live.ai` |
| 安装位置 | `/system_ext/priv-app/RayNeoLiveAI/` |
| 运行权限 | system (UID=1000) |
| 运行内存 | ~60MB |

**核心功能推断**：

从运行时数据看到它绑定了 XR Runtime 的 MonitorService，说明它在实时获取相机和传感器数据。结合名字 "Live AI"，它的功能很可能是：

```
相机实时画面
    ↓
Live AI 接收画面流
    ├── 实时翻译（看到外文自动翻译）
    ├── 实时字幕（对话实时字幕）
    ├── 实时标注（识别物体并标注）
    └── AI 识图（拍照识物）
    ↓
结果叠加到 AR 显示上
```

---

### 2.6 App Container（应用容器/虚拟机）

| 项目 | 值 |
|------|-----|
| 包名 | `com.ffalcon.appcontainer` |
| 安装位置 | `/system_ext/priv-app/AppContainer/` |

**作用**：让普通 Android 手机 App 在 AR 眼镜上运行。

**原理**：
```
普通手机 App（设计给触摸屏的）
    ↓
App Container 拦截
    ├── 拦截触摸事件 → 转换为触控板事件
    ├── 拦截屏幕渲染 → 加入合目处理
    ├── 拦截窗口大小 → 适配 640×480 分辨率
    └── 提供悬浮球入口（:floatball 子进程）
    ↓
手机 App 在 AR 眼镜上正常显示
```

---

### 2.7 导航（Navigation）

| 项目 | 值 |
|------|-----|
| 包名 | `com.ffalcon.navigation` |
| 安装位置 | `/system_ext/priv-app/RayNeoNavigation/` |

从传感器日志看到导航应用的实际行为：

```
导航运行时调用的传感器：
├── 加速度计 (samplingPeriod=20000us = 50Hz) → 步行检测
├── 陀螺仪未校准 (samplingPeriod=20000us) → 航向角计算
├── 磁力计 (samplingPeriod=20000us) → 方向判断
└── 使用高德SDK: com.amap.api.col.3nl.* → 高德地图数据

导航是 XR Runtime MonitorService 的调用者之一
└── recentCallingPackage=com.ffalcon.navigation
```

---

### 2.8 相机（Camera）

| 项目 | 值 |
|------|-----|
| 包名 | `com.leiniao.camera` |
| 安装位置 | `/system_ext/priv-app/MercuryCamera/` |
| 运行内存 | ~132MB |

从传感器日志：
```
相机使用的传感器：
├── GSensorUtil (加速度计, samplingPeriod=5000us=200Hz)
│   → 照片方向校正
├── OrientationEventListener (加速度计, samplingPeriod=200000us)
│   → 横竖屏检测
└── 相机进程以 system 权限运行
```

---

## 三、第三方应用深度解析

### 3.1 支付宝眼镜版（AlipayGGlasses）

| 项目 | 值 |
|------|-----|
| 包名 | `com.eg.android.AlipayGGlasses` |
| 安装位置 | `/product/priv-app/AlipayGGlasses/`（预装特权应用）|
| 运行内存 | ~137MB（最大的第三方应用！）|

从传感器日志发现一个有趣的细节：
```
com.alipay.verifyiotidentity.helper.ImuUtils
├── 使用加速度计 (samplingPeriod=20000us)
├── 使用陀螺仪 (samplingPeriod=20000us)
└── 作用：IoT 身份验证 → 通过运动传感器验证你是真人而非机器
    （这是支付宝的活体检测/设备认证机制）
```

**为什么内存占用这么大？**
支付宝是一个超级 App，即使是眼镜版也包含了小程序框架、支付引擎、安全模块等大量组件。

---

### 3.2 搜狗输入法 IoT 版

| 项目 | 值 |
|------|-----|
| 包名 | `com.sogou.inputmethod.iot` |
| 安装位置 | `/product/priv-app/RayNeoInputMethod/` |
| 运行内存 | ~82MB |

**为什么 AR 眼镜需要输入法？**
- 搜索功能需要文字输入
- WiFi 密码输入
- 语音转文字后的编辑修改
- 这是 IoT 专版，针对触控板操作做了适配

---

### 3.3 用户安装的应用（/data/app/）

| 应用 | 包名 | 功能定位 |
|------|------|---------|
| **微信** | `com.tencent.mm` | 通过 App Container 运行，接收消息通知 |
| **腾讯会议** | `com.tencent.wemeet.app` | AR 远程会议 |
| **B站** | `tv.danmaku.bili` | 视频观看（眼前大屏体验） |
| **小红书** | `com.xingin.xhs` | 生活方式内容浏览 |
| **微博** | `com.sina.weibo` | 社交媒体 |
| **QQ音乐** | `com.tencent.qqmusicpad` | 音乐播放 |
| **凤凰阅读** | `com.phoenix.read` | 电子书阅读 |
| **LifeAI** | `com.rayneo.lifeai` | 雷鸟出品的 AI 生活助手 |
| **AirScreen** | `com.rayneo.airscreen` | 投屏/镜像显示 |
| **NoteShow** | `com.chatppt.noteshow.release` | 演讲提词器/PPT 控制 |
| **Nova** | `com.larus.nova` | 第三方浏览器 |
| **滑雪游戏** | `com.rayneogame.skiing` | AR 游戏 |
| **AR 宠物** | `com.RayneoGame.ARPet` | AR 虚拟宠物 |
| **弹球游戏** | `com.rayneogame.pinball` | AR 游戏 |
| **BeatSpace** | `com.Ffalcon.BEATSPACE` | 音乐节奏游戏 |
| **茅台扫描器** | `com.ffalcon.vision.scanner.maotai` | AR 识别茅台酒真伪 |

---

## 四、应用间通信关系图（实测数据）

基于运行时 Service 绑定关系绘制的**实际数据流图**：

```
┌─────────────────────────────────────────────────────────────────┐
│                     应用间实时通信关系                           │
│                                                                 │
│                    ┌──────────────────┐                          │
│         ┌─────────│ Mercury Launcher │──────────┐               │
│         │  调用    │   (管家/中枢)     │  调用     │               │
│         ↓         └────────┬─────────┘          ↓               │
│  ┌──────────────┐          │            ┌──────────────┐        │
│  │ AI Speech    │          │            │ Media Player │        │
│  │ SpeechService│          │ 绑定       │MediaCoreService│       │
│  │ 由Launcher   │          │            │ 由Launcher    │        │
│  │ 最近调用     │          │            │ 绑定控制      │        │
│  └──────────────┘          ↓            └──────────────┘        │
│                    ┌──────────────────┐                          │
│         ┌─────────│  XR Runtime      │──────────┐               │
│         │  绑定    │ MonitorService   │  绑定     │               │
│         │         │  (AR核心枢纽)     │          │               │
│         ↓         └──┬──────────┬────┘          ↓               │
│  ┌──────────────┐    │          │       ┌──────────────┐        │
│  │ AI Runtime   │    │          │       │   Record     │        │
│  │ (AI推理引擎) │    │          │       │  (录音录屏)   │        │
│  └──────────────┘    │          │       └──────────────┘        │
│                      ↓          │                                │
│              ┌──────────────┐   │                                │
│              │   Live AI    │   │                                │
│              │ (实时AI处理)  │   │                                │
│              └──────────────┘   │                                │
│                                 ↓                                │
│                         ┌──────────────┐                         │
│                         │  Navigation  │                         │
│                         │  (导航)      │                         │
│                         │ 调用MonitorSvc│                        │
│                         └──────────────┘                         │
│                                                                  │
│  关键发现：                                                       │
│  1. XR Runtime 的 MonitorService 是 AR 系统的核心枢纽             │
│     4个应用同时绑定它                                              │
│  2. Launcher 是 system 权限的控制中心                              │
│     它调用 Speech/Media/Location 等所有服务                        │
│  3. 高德定位运行在 Launcher 进程内                                 │
│     为导航和所有位置功能提供数据                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 五、应用权限对比表

| 权限 | Launcher | XR Runtime | AI Runtime | AI Speech | 含义 |
|------|:--------:|:----------:|:----------:|:---------:|------|
| CAMERA | ✅ | ✅ | ✅ | ❌ | 相机 |
| RECORD_AUDIO | ✅ | ❌ | ✅ | ✅ | 录音 |
| BLUETOOTH* | ✅ | ❌ | ✅ | ❌ | 蓝牙 |
| LOCATION | ✅ | ❌ | ✅ | ❌ | 定位 |
| INTERNET | ✅ | ❌ | ✅ | ✅ | 网络 |
| FOREGROUND_SERVICE | ✅ | ✅ | ✅ | ❌ | 常驻前台 |
| SYSTEM_ALERT_WINDOW | ✅ | ❌ | ❌ | ✅ | 悬浮窗 |
| REBOOT/SHUTDOWN | ✅ | ❌ | ❌ | ❌ | 重启关机 |
| INSTALL_PACKAGES | ✅ | ❌ | ❌ | ❌ | 安装应用 |
| INJECT_EVENTS | ✅ | ❌ | ❌ | ❌ | 注入输入 |
| HIGH_SAMPLING_RATE_SENSORS | ❌ | ✅ | ❌ | ❌ | 高速传感器 |
| CAPTURE_AUDIO_HOTWORD | ❌ | ❌ | ❌ | ✅ | 唤醒词 |
| MANAGE_HOTWORD_DETECTION | ❌ | ❌ | ❌ | ✅ | 唤醒词管理 |

**规律**：
- **Launcher** = 什么权限都有的"上帝模式"
- **XR Runtime** = 只要传感器和相机，极简但高效
- **AI Runtime** = 全面感知（视觉+语音+位置+网络）
- **AI Speech** = 专注于音频和语音交互

---

## 六、应用技术栈分析

从包名和库引用可以看出各应用使用的技术栈：

| 应用 | 语言 | 框架/SDK | 第三方库 |
|------|------|---------|---------|
| Launcher | Kotlin | Android SDK, WorkManager | 高德 SDK, Google ML Kit, OkDownload, BlankJ Utils |
| XR Runtime | Kotlin/C++ | Android NDK, OpenXR | - |
| AI Runtime | Kotlin | Android SDK | - |
| AI Speech | Kotlin | Android VoiceInteraction API | BlankJ Utils, ProfileInstaller |
| 相机 | Java/Kotlin | Camera2 API | - |
| 导航 | Kotlin | Android SDK | 高德地图 SDK (com.amap.api) |
| 支付宝 | Java | Alipay SDK | IMU 传感器活体检测 |
| 搜狗输入法 | Java/Kotlin | IME Framework | - |

---

## 七、运行时资源消耗总览

### 内存占用排行（所有雷鸟应用）

```
应用                              内存      占比(3.7GB)  状态
────────────────────────────────────────────────────────────
Mercury Launcher                  234MB     6.3%        常驻
相机 (com.leiniao.camera)         132MB     3.6%        常驻
图库 (com.rayneo.gallery)         133MB     3.6%        常驻
AI Runtime                        110MB     3.0%        常驻
AI Speech (主进程)                  98MB     2.6%        常驻
录音 (com.rayneo.record)           92MB     2.5%        常驻
电话 (com.rayneo.phone)            88MB     2.4%        常驻
媒体 (com.rayneo.media)           106MB     2.9%        常驻
AI Speech (交互进程)               67MB     1.8%        常驻
笔记 (com.rayneo.notes)            61MB     1.6%        常驻
Live AI                            60MB     1.6%        常驻
XR Runtime                         60MB     1.6%        常驻
App Container (悬浮球)              61MB     1.6%        常驻
────────────────────────────────────────────────────────────
雷鸟应用总计                     ~1.3GB    35.1%

第三方应用（支付宝等）            ~340MB     9.2%
SystemUI + Settings 等            ~200MB     5.4%
system_server                      304MB     8.2%
────────────────────────────────────────────────────────────
总计约                            ~2.1GB    56.8%   (剩余约1.6GB)
```

---

## 八、重要发现与结论

### 发现1：Launcher 是真正的"操作系统"

Mercury Launcher 远不是一个简单的桌面。它拥有：
- 60+ 个系统权限（包括重启、安装应用、注入输入等）
- 内置高德定位服务
- 内置 VPN 服务（Gnirehtet 反向网络共享）
- 内置账号认证系统
- 内置设备管理器
- 它实际上是整个 RayNeo OS 的控制中心

### 发现2：XR Runtime MonitorService 是 AR 枢纽

4 个核心应用同时绑定它，说明所有 AR 数据流都经过这个服务。如果它挂了，整个 AR 系统都会瘫痪。

### 发现3：AI Speech 是最复杂的单一应用

三进程架构（主服务 + 交互 + 唤醒词）在 Android 应用中非常罕见。targetSdk=34 说明它使用了 Android 14 的最新 API。

### 发现4：支付宝在用传感器做身份验证

`ImuUtils` 通过加速度计和陀螺仪做活体检测，确保操作者是真人而非程序。

### 发现5：系统内存压力很大

3.7GB 内存中，雷鸟应用 + 系统服务已经占了约 2.1GB，只剩约 1.6GB 给用户。这解释了为什么系统标记 `ro.config.low_ram=true`。

---

> 📖 数据来源：2026年3月19日通过 ADB 从设备 BC94249AF514442 采集的实时 dumpsys 数据。
> 所有服务绑定关系、权限列表、进程内存数据均为运行时实测。
