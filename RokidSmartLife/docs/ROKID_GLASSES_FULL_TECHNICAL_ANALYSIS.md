# Rokid AI 眼镜 完整技术深度分析 (开发者指南)

> 分析日期：2026-03-13 | 固件版本：1.15.004 | Android 12 (API 32)
> 设备型号：RG-glasses | 硬件版本：DVT3 | 芯片：Qualcomm Neo (QCS6490)

---

# 第一部分：硬件与系统架构

## 1.1 硬件规格

| 项目 | 详情 |
|------|------|
| **SoC** | Qualcomm Neo (QCS6490), 1+3+4 三簇 CPU |
| **CPU** | 1× Prime (最高 2.4GHz) + 3× Big (最高 2.208GHz) + 4× Little (A55, 最高 1.094~1.651GHz) |
| **GPU** | Adreno (285~540MHz), OpenGL ES 3.2, Vulkan |
| **内存** | 1.78 GB |
| **存储** | ~20GB (系统 ~2.3GB + 数据分区 ~19GB) |
| **显示屏** | 480×640 像素竖屏 (内置 INTERNAL) |
| **摄像头** | 1× Sony IMX681, 后置, 支持 15/24/30/60fps, 无闪光灯, Camera HAL v3.7 |
| **电池** | 锂离子, 4.501V |
| **麦克风** | **8 通道麦克风阵列** (16kHz 采样率, 波束成形/远场拾音) |
| **扬声器** | Qualcomm WSA 智能功放 |
| **IMU** | TDK-Invensense ICM-4x6xx 六轴 (I3C 12.5MHz, 1~500Hz) |
| **接近传感器** | Sensortek UCS146E0 (佩戴检测) |
| **蓝牙** | BT 5.2+, BLE, A2DP, HFP, PBAP, LE Audio (LC3) |
| **WiFi** | QCA WLAN, WiFi Direct (P2P) |
| **USB** | USB-C, 支持 ADB/MTP/UVC(摄像头)/UAC2(音频) |
| **MFi** | Apple Made for iPhone 认证 |
| **内核** | Linux 5.10.209-perf |

## 1.2 系统分区架构

```
/system  (795MB) ── Android 核心 + Rokid 系统服务
/product (639MB) ── Rokid 定制应用 (AI助手/支付/启动器)
/vendor  (571MB) ── 高通 HAL/驱动/固件
/system_ext (291MB) ── Settings 等系统扩展
/vendor_dlkm (61MB) ── 161 个可加载内核模块
/odm (1MB)  ── ODM 定制 (最小化)
/data (19GB) ── 用户数据 (需root访问)
```

## 1.3 系统启动流程

```
Linux Kernel 5.10
  ↓
init (Android 12)
  ↓ vendor init .rc (66个HAL服务)
  ├── 高通 HAL 服务 (音频/显示/传感器/蓝牙/相机/WiFi...)
  ├── CXRService.apk (persistent, system uid) ─── 蓝牙通信桥梁
  ├── RokidSysConfig.apk (persistent, directBoot) ─── LED/传感器/关机策略
  └── RokidSpriteAssistServer.apk (persistent) ─── AI核心大脑
       ├── MasterAssistService ── 主协调中心
       ├── InstructService ── 语音指令引擎
       ├── SpriteMediaService ── 拍照/录像/录音
       ├── PaymentService ── 支付集成
       ├── TtsService (:tts进程) ── 语音合成
       ├── RokidBluetoothService ── 蓝牙管理(15个Manager)
       ├── SystemFuncService ── 系统功能
       ├── WebServerService ── 内置Web服务器(端口8848)
       ├── SpriteWifiService ── WiFi管理
       └── JsAiService ── JS AI引擎
  
  ↓ BOOT_COMPLETED
  ├── RokidSpriteLauncher (主界面)
  ├── RokidOtaUpgrade (OTA服务)
  └── 支付应用 (Alipay/AntGroup/JdPay)
```

Rokid 的定制服务**不是独立的 init 进程**，而是以系统 APK (`persistent=true`) 形式运行。

---

# 第二部分：交互系统 (完整细节)

## 2.1 物理输入设备

| 设备 | 芯片 | 总线 | 功能 |
|------|------|------|------|
| `ROKID,PSOC-TP-R` | Cypress PSOC | I2C | 触控板 (镜腿) |
| `qpnp_pon` | Qualcomm PMIC | 内部 | 电源/功能键 |
| `gpio-keys` | GPIO | 内部 | 音量+/-、HOME、CAMERA、FOCUS |

## 2.2 触控板操作 → 系统事件映射

触控板的手势被 PSOC 固件转换为 Android KeyEvent：

| 触控板操作 | Android KeyCode | 系统行为 |
|-----------|-----------------|---------|
| **单击** | `KEYCODE_ENTER (66)` | 确认/选择 |
| **双击** | `key 202 → SPRITE_DOUBLE_TAP` | 自定义双击事件 |
| **向前滑动** | `key 183 → SPRITE_SWIPE_FORWARD` | 自定义前滑事件 |
| **向后滑动** | `key 184 → SPRITE_SWIPE_BACK` | 自定义后滑事件 |
| **向上滑动** | `KEYCODE_DPAD_UP (19)` | 焦点上移 / 切换到消息页 |
| **向下滑动** | `KEYCODE_DPAD_DOWN (20)` | 焦点下移 / 切换到应用列表页 |
| **向左滑动** | `KEYCODE_DPAD_LEFT (21)` | 焦点左移 / 返回操作 |
| **向右滑动** | `KEYCODE_DPAD_RIGHT (22)` | 焦点右移 / 前进操作 |
| **长按** | (长按 ENTER 变体) | 上下文菜单/长按操作 |

## 2.3 功能键操作

| 操作 | 系统广播 Action | 默认行为 |
|------|----------------|---------|
| **短按** | `ACTION_SPRITE_BUTTON_UP` | **拍照** (可配置为录像/录音) |
| **长按** | `ACTION_SPRITE_BUTTON_LONG_PRESS` | **录像** (可配置为拍照/录音) |
| **超长按** | `ACTION_SPRITE_BUTTON_VERY_VERY_LONG_PRESS` | 强制停止录像 |
| **按下** | `ACTION_SPRITE_BUTTON_DOWN` | 按下事件 |

**自定义配置：**
- `settings_interaction_shortPressFun` → 默认 `"picture"`, 可选 `"video"` / `"audio"`
- `settings_interaction_longPressFun` → 默认 `"video"`, 可选 `"picture"` / `"audio"`

**AI 键：**
- `ACTION_AI_START` → 启动 AI 助手 (根据模式选择本地AI或手机端助手)
- `ACTION_SETTINGS_KEY` → 快捷指令 (蓝牙已连接时)

## 2.4 功能键处理流程 (完整)

```
物理按键事件
  ↓
FunctionKeyReceiver 接收广播
  ↓
1. 佩戴延迟检查 → 刚戴上眼镜 3 秒内忽略按键
2. 防抖检查 → 1 秒内重复按键忽略
3. 设备状态检查 → 镜腿折叠/未佩戴则忽略
4. OTA 检查 → 更新中播放"正在更新"TTS并忽略
  ↓
按键分发:
├── 短按 → 发送 pressKey="1" 到手机 → 执行 shortPressFun 配置的功能
├── 长按 → 发送 longPressKey="1" 到手机 → 执行 longPressFun 配置的功能
├── 超长按 → 强制停止录像
├── AI键 → 模式判断:
│   ├── 模式 ≠ "2" → 本地AI助手 (openSceneWithIgnoreTips("ai_assist"))
│   └── 模式 = "2" → 手机端语音助手 (startVoiceRecognition)
└── 设置键 → 蓝牙已连接 → 发送 openAIFunction(2,2) → AI助手
```

## 2.5 镜腿/佩戴检测

**实现方式：** RokidSysConfig 中的 `PsensorObserver` 通过 Linux `UEventObserver` 监控 I2C extcon 设备节点。

| 检测项 | UEvent 字段 | 系统属性 | 行为 |
|--------|------------|---------|------|
| **镜腿展开** | `DOCK=1` | `vendor.rkd.glasses.is_spread=1` | 允许操作 |
| **镜腿折叠** | `DOCK=0` | `vendor.rkd.glasses.is_spread=0` | 忽略按键; 启动关机倒计时(默认20分钟) |
| **佩戴上** | `JIG=1` | `vendor.rkd.glasses.is_take_on=1` | 唤醒屏幕、播放音效、关闭充电灯 |
| **摘下** | `JIG=0` | `vendor.rkd.glasses.is_take_on=0` | 忽略按键; 3秒延迟防抖 |

**折叠自动关机：** 镜腿折叠且未充电 → 倒计时 `rkd_shutdown_timeout`(默认1200000ms=20分钟) → 写入 I2C `auto_startup=1` → `PowerManager.shutdown("leg_fold_timeout")`

## 2.6 Launcher 主界面交互

**三页结构 (ViewPager2)：**

```
← 上滑(DPAD_UP) →  消息列表页(MsgListFragment)
                     ↕ 上下滑动切换
                   中心主页(CenterFragment) ←← 默认页
                     ↕ 上下滑动切换
← 下滑(DPAD_DOWN) → 应用列表页(AppListFragment)
```

**CenterFragment 按键：**
- `BACK` → 息屏 (`PowerManagerUtils.goToSleep()`)
- `DPAD_UP` → 切换到消息列表
- `DPAD_DOWN` → 切换到应用列表
- `ENTER` → 展开/折叠计划卡片

**AppListFragment 焦点导航：**
- 水平 GridLayout 网格，焦点驱动导航
- `DPAD_LEFT/RIGHT` → 焦点左右移动
- 到达左边界第一次 → 显示"返回"提示 + 音效
- 到达左边界第二次 → 返回中心页
- 到达右边界 → 播放边界音效

**音频反馈：**
- 焦点切换 → `playFocusChangeSoundEffect()`
- 边界触达 → `playFocusOutSoundEffect()`

## 2.7 头部手势检测

| 库 | 功能 |
|----|------|
| `librfm3dof.so` | 3DOF 头部姿态追踪 |
| `nodshake` | 点头(确认)/摇头(取消)手势检测 |

## 2.8 底部状态栏

每个页面都有固定状态栏，包含：

```
[时间] [WiFi信号] [蓝牙图标] [电量图标+%] [天气] [外设] [录音指示]
```

WiFi 信号分 5 级：≥-55dBm(满) / ≥-66dBm(3格) / ≥-88dBm(2格) / ≥-100dBm(1格) / <-100dBm(无)

状态栏底部可嵌入 Fragment 显示活跃场景：音乐/通话/录音/录像/AI助手/点餐。

---

# 第三部分：语音系统 (完整细节)

## 3.1 离线语音识别

**引擎：** 自研 native 库 `librt_instruct.so`，通过 `RtInstructSdk` JNI 调用。

**工作原理：** 实时分析 8 通道麦克风阵列的音频流，在设备端本地匹配指令词。不需要网络。

**19 个离线语音命令：**

| # | 命令 | 中文语音 | 功能 |
|---|------|---------|------|
| 1 | `OA_Ruoqi` | **"若琪"** | 唤醒词 |
| 2 | `OA_Leqi` | **"乐琪"** | 唤醒词(别名) |
| 3 | `OA_HiBaoLong` | **"嗨暴龙"** | 唤醒词(别名) |
| 4 | `OA_JieDianHua` | **"接电话"** | 接听来电 |
| 5 | `OA_XiaYiShou` | **"下一首"** | 下一曲 |
| 6 | `OA_ShangYiShou` | **"上一首"** | 上一曲 |
| 7 | `OA_BoFangYinYue` | **"播放音乐"** | 播放音乐 |
| 8 | `OA_ZanTingBoFang` | **"暂停播放"** | 暂停播放 |
| 9 | `OA_ShengYinDaYiDian` | **"声音大一点"** | 增大音量 |
| 10 | `OA_ShengYinXiaoYiDian` | **"声音小一点"** | 减小音量 |
| 11 | `OA_LiangYiDian` | **"亮一点"** | 增大亮度 |
| 12 | `OA_AnYiDian` | **"暗一点"** | 减小亮度 |
| 13 | `OA_PaiZhao` | **"拍照"** | 拍照 |
| 14 | `OA_LuYin` | **"录音"** | 开始录音 |
| 15 | `OA_JieShuLuYin` | **"结束录音"** | 停止录音 |
| 16 | `OA_LuXiang` | **"录像"** | 开始录像 |
| 17 | `OA_JieShuLuXiang` | **"结束录像"** | 停止录像 |
| 18 | `OA_DangQianDianLiang` | **"当前电量"** | 查询电量 |
| 19 | `OA_TuiChu` | **"退出"** | 退出当前场景 |

**语音识别流程：**
```
8通道麦克风阵列 → librt_instruct.so (Native引擎) → 匹配指令词
  → onInstructCall(instructId) → OffLineManager.handleInstruct()
  → actionMap.get(cmdId).handleAction() → 执行具体操作
```

**语音控制开关：** `settings_voice_control` = `"open"` / `"close"`

**支持的语言切换：** 中文 (`zh_cn` → `"zh"`) / 英文 (其他 → `"en"`)

## 3.2 本地 TTS (文字转语音)

**引擎：** 自研 native 库 `librfm-tts-jni.so`，版本 3.0.1.2

**独立进程：** `:tts` 进程，通过 AIDL 接口 `ITtsServer` 提供服务

**工作方式：**
1. 初始化时复制 `assets/resource/` 到 `appFiles/resource/`
2. `TtsNative.init(configDir, serialNo, "Glasses", params)` 初始化引擎
3. `TtsNative.predict(text, timestamp)` → 返回 PCM 音频数据
4. 通过 `StreamAudioPlayer` 流式播放

**性能指标：** 首包响应时间(`firstPacketRt`), 实时因子(`rtf`), 声学模型时间(`acousticRt`), 语音合成器时间(`vocoderRt`)

**双通道 TTS：**
- **本地 TTS** → 眼镜端 `librfm-tts-jni.so` 直接合成播放
- **远程 TTS** → 通过蓝牙 GATT 发送到手机端播放 (`Sys.Tts_SendPlayTts`)

## 3.3 在线语音识别 (手机端)

**翻译功能的语音识别：** 使用 **微软 Azure 语音服务** (`MicrosoftCognitiveServicesSpeech` SDK)，在手机端运行。

**AI 助手的语音识别：** 可选本地AI或手机端助手模式。

---

# 第四部分：功能实现细节

## 4.1 翻译功能

**实现架构：**
```
用户说话 → 眼镜麦克风 → 蓝牙传输音频 → 手机App
  → 微软 Azure Speech SDK 语音识别
  → 翻译引擎处理
  → 结果通过蓝牙 GATT 返回 → 眼镜显示翻译结果
```

**通信命令：**
- `Trans.Trans_ResponseChangeSceneId` → 翻译场景切换
- 眼镜端场景 Key: `translate`
- 手机端: `RealTimeTranslationActivity`, `TranslationSettingsActivity`

**语音场景常量：**
- `AGT_SPEECH_SCENE_TRANSLATION = 1` → 翻译模式
- `AGT_SPEECH_SCENE_CALL = 2` → 通话模式
- `AGT_SPEECH_SCENE_CONFERENCE = 3` → 会议模式

**算力分配：** 语音识别和翻译计算在**手机端**执行，眼镜负责音频采集和结果显示。

## 4.2 提词器

**眼镜端：** `WordTipsPageActivity` (Launcher 中的 Activity)，场景 Key: `word_tips`

**手机端：** `PrompterMainActivity` → `PrompterEditActivity` → 设置内容 → 蓝牙推送到眼镜

**工作方式：** 手机编辑提词内容 → 通过蓝牙 GATT 发送到眼镜 → 眼镜端 480×640 屏幕显示文字内容

## 4.3 音乐控制

**实现方式：** 通过蓝牙 A2DP (音频) + HFP (通话) 协议

**架构：**
```
手机端音乐App (QQ音乐等)
  → A2DP 音频流 → 眼镜扬声器播放
  ← 控制命令 (上/下一首, 播放/暂停)
```

**控制通道：**
- 语音命令: "播放音乐"/"暂停播放"/"下一首"/"上一首"
- 蓝牙命令: `Music.*` 系列
- 手机端: `QQMusicCallbackActivity`
- 眼镜端: `MusicPageActivity` + `MusicStatusFragment` (状态栏歌词显示)

**蓝牙音频编码：** SBC, AAC, aptX, aptX HD, LDAC, LC3 (LE Audio)

## 4.4 导航

**实现：** 高德地图 SDK，在手机端运行导航计算

**国内/海外区分：**
```java
getNaviActivityClass() → appIsOverseas()
  ? NavigationOverseaPageActivity   // 海外导航
  : NavigationPageActivity          // 国内导航 (高德)
```

**通信命令：** `Nav.*` 系列通过蓝牙 GATT

**启动方式：**
```java
NaviStart event → intent.putExtra("destination", destination)
                → intent.putExtra("naviType", type)
                → intent.putExtra("locPermissionTip", tip)
```

**手机端：** `MapActivity`, `NavigationActivity`, `LocationService`
- 高德地图 API Key: `6f7762ff2284d88f5c32a8484bb4b710`
- 手机计算路线 → 导航指令通过蓝牙发送到眼镜 → 眼镜显示转向箭头/距离

## 4.5 慧眼 (AI 视觉助手)

**实现方式：**
```
眼镜摄像头 → 拍照/视频帧 → 蓝牙传输到手机
  → 手机端 AI 推理 (onnxruntime / 云端API)
  → 识别结果返回眼镜显示
```

**相关命令：**
- `Ai.KeyDown` → AI 按键触发
- `Ai.Ai_TakePhoto` → AI 拍照
- `Ai.Ai_TakePhoto_Over` → AI 拍照完成
- `ARTC.OnPushVideoFrame` → 推送视频帧到手机
- `ARTC.NotifyUserSpeaking` → 用户正在说话

**手机端框架：**
- iOS: `onnxruntime` (本地推理) + `swift-transformers` (NLP)
- Android: AI 功能可能加壳保护，具体实现被加密

**场景 Key:** `ai_chat` (AI 聊天), `ai_assist` (AI 助手 Cut)

## 4.6 支付系统

**5 种支付渠道：**

| 渠道 | 场景Key | APK大小 | 核心服务 |
|------|---------|---------|---------|
| **支付宝** (传统) | `payment` | 96MB | `Glass2PayService` 刷脸/扫码支付 |
| **蚂蚁支付** (新版) | `ant_pay` | 48+19MB | `GPassService` 新版支付 |
| **微信支付** | `weixin_pay` | - | `PhoneWxPayService` (手机端) |
| **京东支付** | `jd_pay` | 28MB | `SDKService` (JoyGo) |
| **城市导览** | `city_guide` | - | `AliCityGuideActivity` (手机端) |

**注意：** 支付宝和蚂蚁支付**互斥**，初始化一个会销毁另一个。

**代理通信机制：**
```
手机App ←→ 蓝牙GATT (Pay.* 命令) ←→ PaymentProxyManager ←→ 各PayCenter
```

**消息格式：**
```json
{"type":"PROXY_TYPE_PAY_DEVICE","requestId":"xxx","reqMessage":{"param0":"...","param1":"..."}}
```

## 4.7 拍照

**CameraX / Camera2 API，** 通过 `CameraFuncManager` 管理。

**触发方式：**
1. 功能键短按 (默认) → `MSG_TAKE_PICTURE`
2. 语音指令 "拍照" → `OA_PaiZhao`
3. 手机端远程拍照 → `TakePhotoAction`
4. AI 辅助拍照 → `TYPE_PICTURE_AI_ASSIST`

**拍照流程：**
```
触发 → 检查存储空间 → 检查电量(>10%) → 检查相机环境
  → openCamera() → doTakePicture() → 保存文件
  → 生成缩略图 → 通过蓝牙发送到手机 (Med.Med_NewThumbnail)
```

**图像处理（虹软 ArcSoft SDK）：**
- HDR 高动态范围
- 低光拍照增强
- 运动检测
- 畸变校正
- 遮挡检测 (阈值 < 0.1f)

**存储路径：** 照片和视频存储在 `/sdcard/DCIM/Camera/` 和 `/sdcard/Movies/Camera/`

## 4.8 录像

**视频参数：** 480×640, H.264, 4Mbps, 30fps, I帧间隔 10秒

**触发方式：**
1. 功能键长按 (默认) → `MSG_START_VIDEO_RECORD`
2. 语音 "录像" / "结束录像"
3. 手机端远程控制

**限制：** 最大 600 秒 (10分钟), 最小存储空间 400MB, 电量 >10%

## 4.9 录音

**参数：** AAC, 196kbps, 16kHz 单声道
**存储路径：** `/sdcard/Recordings/`

**触发方式：** 语音 "录音"/"结束录音"、功能键 (配置为 audio 时)、手机远程

## 4.10 屏幕录制/投屏

**三种模式：**
| 模式 | 说明 |
|------|------|
| RECORD (1) | 本地录屏为 MP4 (480×640 H.264 4Mbps) |
| STREAM (2) | 仅编码不存储 (投屏) |
| LIVE_STREAM (3) | RTMP 实时直播推流 |

**捕获方式：** 使用 `SurfaceControl.createDisplay()` 创建虚拟显示并投射主屏画面。

## 4.11 直播推流

**RokidSpriteLive：** RTMP 直播推流服务

**架构：**
```
摄像头 → CameraRecorder → VideoEncoder (H.264)
                               ↓
                        RtmpSenderImpl → FLV 封装 → RTMP 服务器
                               ↑
麦克风 → AudioRecorder → AudioEncoder (AAC)
```

**手机端集成：** 抖音直播 (字节跳动 Zeus 插件系统)

---

# 第五部分：通信协议 (完整细节)

## 5.1 CXRService — 通信桥梁

CXRService 是眼镜与手机之间所有通信的核心桥梁，支持三种连接方式：

| 连接方式 | clientType | 适用 | 传输方式 |
|----------|-----------|------|---------|
| **BT RFCOMM Socket** | 1 | Android 手机 | 经典蓝牙 SPP |
| **BLE GATT** | 2 | iOS 手机 (无MFi) | 低功耗蓝牙 |
| **MFI iAP2 Socket** | 3 | iOS 手机 (有MFi) | Apple 认证通道 |

## 5.2 BLE 广播与连接

**眼镜 BLE 广播内容：**
- Compound Service UUID
- 设备序列号 (`ro.serialno`)

**GATT 服务特征：**

| 特征 UUID | 属性 | 用途 |
|-----------|------|------|
| `IOS_CONNECTION_CHAR` | Read | iOS 读取连接信息 |
| `RFCOMM_CONNECTION_CHAR` | Read | Android 读取 RFCOMM UUID 和 MAC |
| `IOS_WRITE_CHAR` | Write | iOS 写入数据 |
| `IOS_NOTIFY_CHAR` | Notify | 向 iOS 推送数据 |
| `IOS_MFI_PAIRING_CHAR` | Write | MFI 配对请求 |
| `IOS_MFI_NOTIFY_CHAR` | Notify | MFI 通知 |

**连接流程（Android）：**
```
1. 手机 BLE 扫描 → 发现眼镜广播
2. 连接 GATT → 读取 RFCOMM_CONNECTION_CHAR → 获取 serviceRecord UUID + MAC
3. 建立 RFCOMM Socket 连接
4. 认证握手 (Caps 协议，版本 1.4)
5. 通信就绪
```

**连接流程（iOS MFI）：**
```
1. BLE 扫描 → 发现眼镜
2. 写入 MFI_PAIRING_CHAR → 触发经典蓝牙配对
3. 建立 External Accessory (iAP2) Socket
4. 使用协议: com.rokid.aiglasses / com.rokid.bolonglasses
5. 通信就绪
```

## 5.3 Caps 序列化协议

自定义二进制序列化格式，通过 JNI native 实现：
- 支持类型：Int32, UInt32, Int64, UInt64, Float, Double, String, Binary, Object (嵌套)
- 方法：`serialize()` / `parse()` / `write()` / `read()`
- 用于所有蓝牙 GATT 数据传输

## 5.4 CXR 命令体系 (21 个命令分类)

| cmd前缀 | 说明 | 主要Key |
|---------|------|---------|
| **`Sys`** | 系统 | 音频场景切换, TTS播放 |
| **`Trans`** | AI翻译 | 翻译场景切换, 翻译数据 |
| **`Tra`** | 翻译(旧) | 旧版翻译 |
| **`Med`** | 媒体 | 打开/关闭相机, 缩略图同步, 未同步计数 |
| **`Dev`** | 设备 | 设备信息, 电量, 亮度, 音量, 屏幕状态 |
| **`Ai`** | AI | AI按键, 拍照, 视频帧推送 |
| **`Nav`** | 导航 | 导航指令, 路线数据 |
| **`Music`** | 音乐 | 播放控制, 歌词 |
| **`Pay`** | 支付 | 支付代理消息 |
| **`Ntf`** | 通知 | 手机通知转发 |
| **`Wifi`** | WiFi | WiFi 状态 |
| **`Broadcast`** | 直播 | 直播推流控制 |
| **`ARTC`** | 实时通信 | 音视频帧, 聊天 |
| **`Jsai`** | JS AI | JavaScript AI 引擎 |
| **`Proxy`** | 代理 | 通用代理通信 |
| **`Schedule`** | 日程 | 日程同步/添加/删除 |
| **`Memo`** | 备忘 | 备忘同步 |
| **`Journey`** | 行程 | 行程管理(火车票/航班) |
| **`Ota`** | OTA | 固件更新通知 |
| **`Order`** | 点餐 | 餐饮点单 |
| **`Settings`** | 设置 | 设置同步 |
| **`Custom_View`** | 自定义视图 | 第三方自定义界面 |

**GATT 消息格式：**
```json
{
  "cmd": "Sys",
  "key": "Tts_SendPlayTts",
  "param": "{\"content\":\"你好\"}"
}
```

## 5.5 WiFi P2P 高速传输

`WifiP2pServerManager` 用于大文件传输（照片/视频同步），通过 WiFi Direct 建立高速连接。

iOS App 权限说明：*"发现和连接眼镜热点来同步媒体文件"*

## 5.6 内置 Web 服务器

**AndServer 框架，端口 8848。**

用于局域网文件传输：
- 传输目录：`/sdcard/Download/`
- 空间照片：`/sdcard/SpatialPhotos/`
- NSD (mDNS) 局域网发现
- 支持格式：image, video, 3d_video, audio, document, apk, subtitle, playlist

## 5.7 云端 API

| 环境 | 域名 |
|------|------|
| **生产** | `https://rokid-content-pro.rokid.com` |
| **测试** | `https://rokid-content-test.rokid.com` |
| **开发** | `https://rokid-content-dev.rokid.com` |
| **OTA** | `https://ota.rokid.com/v1/extended/ota/check` |

---

# 第六部分：手机 App 分析

## 6.1 Android App (com.rokid.sprite.aiapp)

**版本：** 1.4.0.0312 (391MB)
**保护：** 网易易盾 (NetEase NIS) 加壳，业务逻辑被加密

**从 Manifest 分析的功能模块：**

| 功能 | Activity/Service | 说明 |
|------|-----------------|------|
| **蓝牙连接** | `ConnectCompanionDeviceService` | CompanionDeviceManager API |
| **AI 服务** | `AiService` (前台服务) | AI 核心 |
| **翻译** | `RealTimeTranslationActivity`, `PhoneTranslationActivity` | 实时/离线翻译 |
| **导航** | `MapActivity`, `NavigationActivity`, `LocationService` | 高德地图 |
| **提词器** | `PrompterMainActivity`, `PrompterEditActivity` | 创建/编辑/搜索 |
| **笔记** | `NotesMainActivity`, `BindTripActivity` | 笔记+旅程 |
| **音乐** | `QQMusicCallbackActivity` | QQ 音乐 |
| **支付** | `RokidGlassActivity`, `Glass2PayService`, `PhoneWxPayService` | 支付宝/微信 |
| **直播** | `CXRLinkService` | 抖音直播 (字节跳动 Zeus) |
| **社区** | `PublishPostActivity`, `AgentStoreActivity` | UGC/Agent商店 |
| **OTA** | `SettingsOtaActivity` | 固件升级 |
| **通知** | `MessageNotificationListenerService` | 通知转发 |

**集成的第三方服务：**
- 高德地图 (API Key: `6f7762ff2284d88f5c32a8484bb4b710`)
- 腾讯 TPNS 推送
- 华为 HMS Push
- Vivo/小米/OPPO Push
- 字节跳动 (抖音直播/短视频)
- 京东 JoyGo
- Google ML Kit (语言识别)
- 微信登录/分享
- 阿里 RTC
- 微软语音 SDK
- 支付宝扫码 SDK (AppKey: `AR_EYE_ANDROID`)

## 6.2 iOS App (com.rokid.rokidglasses)

**版本：** 1.3.1 (Build 202603061524)
**最低 iOS：** 18.0
**大小：** 624MB

**通信方式：**
- Apple MFI External Accessory (`com.rokid.aiglasses`, `com.rokid.bolonglasses`)
- BLE Central 模式
- 后台支持：bluetooth-central, external-accessory, audio, location

**URL Scheme：**
- `rokidai://` — 深链接
- `wx656b5a082d3b902a` — 微信
- `joygorokid` — 京东
- `awbkus8yvpwfz8tx` — 抖音

**集成 Frameworks：**

| Framework | 用途 |
|-----------|------|
| `MicrosoftCognitiveServicesSpeech` | 微软 Azure 语音 (翻译) |
| `alivcffmpeg` + `AliVCSDK_ARTC` | 阿里云 RTC |
| `onnxruntime` | 本地 AI 推理 |
| `RKEncrypt` | Rokid 加密 |
| `nuisdk` | 阿里 NUI 语音 |
| `AMapNavi.bundle` | 高德导航 |
| `swift-transformers` | NLP Tokenizer |
| `RGSDK.bundle` | Rokid Glass SDK |

---

# 第七部分：场景管理系统

## 7.1 场景类型

AssistServer 通过 `SceneCoreManager` 统一管理三级场景：

**18 个场景 (Scene)：**

| 场景Key | 功能 | 眼镜端Activity |
|---------|------|----------------|
| `translate` | 翻译 | `TranslatePageActivity` |
| `navigation` | 导航 | `NavigationPageActivity` |
| `ai_chat` | AI 聊天 | `ChatPageActivity` |
| `video_record` | 录像 | (无独立Activity) |
| `audio_record` | 录音 | `AudioPageActivity` |
| `live_broadcast` | 直播 | `LivePageActivity` |
| `ar_picture` | AR拍照 | (相机内) |
| `mix_record` | 混合录制 | (录屏) |
| `phone_call` | 电话 | (悬浮窗) |
| `mobile_music` | 手机音乐 | (状态栏) |
| `music_word` | 歌词 | `MusicPageActivity` |
| `word_tips` | 提词器 | `WordTipsPageActivity` |
| `jsai` | JS AI | (后台) |
| `payment` | 支付 | (支付宝) |
| `ota` | OTA | (后台) |
| `custom_view` | 自定义视图 | (悬浮窗) |
| `food_order` | 点餐 | (状态栏) |
| `city_guide` | 城市导览 | (导览) |

**5 个页面 (Page)：** `launcher_main`, `settings`, `brightness`, `volume`, `caexpo`

**2 个 Cut (快捷操作)：** `take_picture` (拍照), `ai_assist` (AI助手)

## 7.2 场景控制 API

```java
// 打开场景（清除其他场景）
ControlSceneAppData.openParamSceneClearAllFirst(sceneKey, params, fromAssist);

// 关闭场景
ControlSceneAppData.closeScene(sceneKey, fromAssist);

// Launcher 接收到 cmd_control_scene_or_app 后执行场景切换
```

---

# 第八部分：LED 灯光系统

通过自定义 HAL `LightsCtrl` (`com.rokid.light.ILightsCtrl`) 控制。

| 事件 ID | 触发条件 |
|---------|---------|
| 4010 | 蓝牙配对中 (闪烁) |
| 4011 | 蓝牙已连接 |
| 1014 | 充电中 |
| 1013 | 充满电 |
| `EVENT_SPECIAL_ID_PHONE_RING` | 来电铃响 |
| `EVENT_SPECIAL_ID_PHONE_CALLING` | 通话中 |
| `EVENT_SPECIAL_ID_PHONE_MIC_MUTE` | 麦克风静音 |

佩戴时自动关闭充电 LED。

---

# 第九部分：OTA 升级系统

**机制：** Android A/B 更新引擎 (`UpdateEngine`)

**服务器：** `https://ota.rokid.com/v1/extended/ota/check`

**认证：** MD5 签名 (`key + device_type_id + device_id + secret + time`)

**下载：** 断点续传 (HTTP Range), 重试 3 次, MD5 校验

**安装流程：**
1. 解压 `payload.bin` 从 `update.zip`
2. `UpdateEngine.applyPayload()` 执行 A/B 分区更新
3. 安装完成后重启

**电量要求：** 下载 ≥ 30%, 升级 ≥ 70% (未充电时)

**远程 OTA：** 支持通过蓝牙从手机端触发 OTA (`sprite_cmd_start_ota`)

---

# 第十部分：音频系统

**8 通道麦克风阵列：** 16kHz 采样率，支持波束成形和远场拾音

**输出通道：**
- Primary output: PCM 16bit, 48kHz (FAST + PRIMARY)
- Deep buffer: PCM 16bit, 48kHz
- Compressed offload: MP3/FLAC/AAC/DTS/WMA/Vorbis
- VoIP: PCM 16bit, 8~48kHz
- Haptics: PCM 16bit, 48kHz (触觉反馈)

**蓝牙音频编码：** SBC, AAC, aptX, aptX HD, LDAC, CELT, aptX Adaptive, aptX TWS+, **LC3** (BT LE Audio)

**HFP 音频通道：** 写入 `/sys/bus/spi/devices/spi0.0/hfp_on` = `"1"` / `"0"` 切换

---

# 第十一部分：传感器系统

**ICM-4x6xx IMU 配置：**
- 总线：I3C 12.5MHz 高速
- I2C 地址：0x68
- 坐标轴映射：X→+Y, Y→-X, Z→+Z (安装方向校正)
- 运动检测阈值：0.6132g

**完整传感器列表 (16 个硬件传感器)：**
- 加速度计 (wakeup/non-wakeup)
- 陀螺仪 (wakeup/non-wakeup)
- 接近传感器 (wakeup/non-wakeup)
- 重力 (融合)
- 线性加速度 (融合)
- 游戏旋转向量 (融合)
- 未校准陀螺仪
- 未校准加速度计

**额外传感器配置 (neo_default_sensors.json)：**
- 磁力计, 运动检测, 传感器温度, 环境光, SAR
- 倾斜检测, 陀螺仪旋转矩阵, 地磁旋转向量
- 快速运动向量, 旋转向量

---

# 第十二部分：开发者指南 (为开发 APK 做准备)

## 12.1 开发环境要求

- **目标 SDK:** Android 12 (API 32), 建议 compileSdk 34
- **最低 SDK:** Android 10 (API 29)
- **屏幕:** 480×640 竖屏，纯焦点导航 (无触摸屏)
- **输入方式:** DPAD KeyEvent (触控板) + 自定义 KeyCode (功能键/手势)

## 12.2 UI 开发注意事项

1. **没有触摸屏** — 所有 UI 必须支持 D-Pad 焦点导航
2. **使用 `android:focusable="true"`** — 所有可交互元素必须可聚焦
3. **焦点视觉反馈** — 必须有清晰的聚焦状态样式
4. **分辨率 480×640** — 竖屏设计，内容要简洁
5. **音效反馈** — 建议在焦点切换时播放音效
6. **防抖** — 建议 200ms 防抖 (`FuncDebounce`)

## 12.3 监听按键事件

```java
// 在 Activity 中重写
@Override
public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_UP) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:    // 触控板向上滑
            case KeyEvent.KEYCODE_DPAD_DOWN:   // 触控板向下滑
            case KeyEvent.KEYCODE_DPAD_LEFT:   // 触控板向左滑
            case KeyEvent.KEYCODE_DPAD_RIGHT:  // 触控板向右滑
            case KeyEvent.KEYCODE_ENTER:       // 触控板单击/确认
            case KeyEvent.KEYCODE_BACK:        // 返回
                break;
        }
    }
    return super.dispatchKeyEvent(event);
}

// 监听功能键广播
IntentFilter filter = new IntentFilter();
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_UP");       // 短按
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS"); // 长按
registerReceiver(receiver, filter);
```

## 12.4 与 AssistServer 通信

通过 AIDL 绑定 `MasterAssistService`：

```java
// 绑定服务
Intent intent = new Intent("com.rokid.p007os.sprite.assist.MasterAssistService");
intent.setPackage("com.rokid.os.sprite.assistserver");
bindService(intent, connection, BIND_AUTO_CREATE);

// 使用 IAssistServer 接口
iAssistServer.registerClient(packageName, callback);
iAssistServer.controlMsgJson(packageName, jsonCommand);
```

## 12.5 系统属性读取

```java
// 佩戴状态
String takeOn = SystemProperties.get("vendor.rkd.glasses.is_take_on"); // "1"=佩戴
String spread = SystemProperties.get("vendor.rkd.glasses.is_spread");   // "1"=展开

// 设备信息
String model = SystemProperties.get("ro.product.model");      // "RG-glasses"
String version = SystemProperties.get("ro.build.version.incremental");
```

## 12.6 安装 APK 到眼镜

```bash
# 通过 ADB 安装
adb install your_app.apk

# 安装为系统应用 (需要 root 或 system 权限)
adb push your_app.apk /product/app/YourApp/
adb shell pm install -r /product/app/YourApp/your_app.apk

# 启动应用
adb shell am start -n com.your.package/.MainActivity
```

## 12.7 关键注意事项

1. **系统级权限** — Rokid 核心应用使用 `android.uid.system`，第三方应用通常没有此权限
2. **CXR 通信** — 与手机通信需要通过 `CXRService` 的 Caps 协议，需要 Rokid SDK
3. **TTS** — 可通过 `ITtsServer` AIDL 调用本地 TTS
4. **相机** — 使用 CameraX/Camera2 标准 API，注意与 AssistServer 的相机冲突
5. **传感器** — 标准 Android SensorManager API 即可访问 IMU 数据
6. **蓝牙** — A2DP/HFP 由系统管理，GATT 通信需通过 CXR 协议
7. **没有 Google Play 服务** — 不能依赖 GMS

---

# 第十三部分：系统 APK 清单

## Rokid 核心应用 (11个)

| APK | 包名 | 大小 | 功能 |
|-----|------|------|------|
| RokidSpriteAssistServer | `com.rokid.os.sprite.assistserver` | 224MB | AI 核心大脑 |
| RokidSpriteLauncher | `com.rokid.os.sprite.launcher` | 28MB | 主界面 |
| RokidSpriteLive | `com.rokid.os.sprite.live` | 12MB | RTMP直播推流 |
| RokidSysConfig | `com.rokid.sysconfig` | <1MB | 系统配置 |
| CXRService | `com.rokid.cxrservice` | <1MB | 蓝牙通信桥梁 |
| RokidOtaUpgrade | `com.rokid.glass.ota` | 3.4MB | OTA升级 |
| RokidScreenRecord | `com.rokid.os.master.screenstream` | 11MB | 屏幕录制 |
| Alipay | `com.eg.android.AlipayGGlasses` | 96MB | 支付宝眼镜版 |
| AntGroup | `com.antgroup.glasses` | 48MB | 蚂蚁集团 |
| AntPay | `com.iap.mobile.ar_pay` | 19MB | 蚂蚁支付 |
| JdPay | `com.jd.jr.joyaibuy` | 28MB | 京东支付 |

## 系统标准应用 (83个)

包含 Android 12 标准系统应用：Settings, Bluetooth, Camera2, 各种 Provider 等。

---

# 附录

## A. 关键文件路径

| 路径 | 用途 |
|------|------|
| `/sys/bus/spi/devices/spi0.0/hfp_on` | HFP 音频通道开关 |
| `ro.boot.i2c.device_node` → `/sys/devices/platform/soc/a90000.i2c/i2c-1/1-0008` | I2C 设备节点 (PSOC) |
| `/sdcard/DCIM/Camera/` | 拍照存储 |
| `/sdcard/Movies/Camera/` | 录像存储 |
| `/sdcard/Recordings/` | 录音存储 |
| `/sdcard/ScreenRecorder/` | 屏幕录制 |
| `/sdcard/SpatialPhotos/` | 空间照片 |
| `/sdcard/Download/` | Web 传输目录 |
| `Settings.Global["rokid_er_remote_transmission"]` | Web 服务器开关 |
| `Settings.System["rkd_shutdown_timeout"]` | 折叠关机倒计时 |

## B. 系统属性速查

| 属性 | 用途 |
|------|------|
| `vendor.rkd.glasses.is_take_on` | 佩戴状态 (0/1) |
| `vendor.rkd.glasses.is_spread` | 镜腿展开状态 (0/1) |
| `persist.rkd.enablePsensor` | 接近传感器开关 |
| `persist.rkd.leg.fold_timeout.enable` | 折叠关机功能开关 |
| `persist.rkd.local.tts.init.flag` | TTS 初始化标志 |
| `rokid.psensor.mode` | 接近传感器模式 (sensitive) |
| `rokid.debug.two_finger_click` | 双指点击开关 |
| `rokid.debug.two_finger_flick` | 双指轻弹开关 |
| `rokid.cxr-service.version` | CXR 服务版本 |
| `debug.rokid.screenrecord_state` | 屏幕录制状态 |
| `persist.rokid.sprite.ota.running` | OTA 运行状态 |
| `persist.rokid.stream.state` | 流状态 |

## C. 广播 Action 速查

| Action | 用途 |
|--------|------|
| `com.android.action.ACTION_SPRITE_BUTTON_UP` | 功能键短按 |
| `com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS` | 功能键长按 |
| `com.android.action.ACTION_SPRITE_BUTTON_VERY_VERY_LONG_PRESS` | 超长按 |
| `com.android.action.ACTION_SPRITE_BUTTON_DOWN` | 功能键按下 |
| `com.android.action.ACTION_AI_START` | AI 启动 |
| `com.android.action.ACTION_SETTINGS_KEY` | 设置键 |
| `com.rokid.os.sprite.action.LIVE_STREAM_ON` | 直播开始 |
| `com.rokid.os.sprite.action.LIVE_STREAM_OFF` | 直播停止 |
| `com.rokid.yodaos.action.SCREENRECORD_START` | 录屏开始 |
| `com.rokid.yodaos.action.SCREENRECORD_STOP` | 录屏停止 |
