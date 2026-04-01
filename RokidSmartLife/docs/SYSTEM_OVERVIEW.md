# Rokid AI 眼镜系统完整分析

> 提取日期：2026-03-13
> 设备序列号：1901092544011861

---

## 一、硬件概览

| 项目 | 详情 |
|------|------|
| **型号** | RG-glasses (Rokid AI 眼镜) |
| **品牌** | Rokid |
| **OEM 型号** | RV101 (OEM ID: 101) |
| **硬件版本** | DVT3 |
| **芯片平台** | Qualcomm "neo" (QCS6490/QCM6490 系列) |
| **CPU** | 4 核 ARM Cortex-A55 (ARMv8, 38.40 BogoMIPS) |
| **GPU** | Adreno (OpenGL ES 3.2) |
| **内存** | 1.78 GB (1,816,600 KB) |
| **存储** | 约 20GB (系统分区 ~2.3GB + 数据分区 ~19GB) |
| **显示屏** | 480×640 像素 (竖屏, 内置 INTERNAL 类型) |
| **电池** | 锂离子 (Li-ion), 4.501V |
| **摄像头** | 1 个后置摄像头 (Camera HAL v3.7), 无闪光灯 |
| **传感器** | 16 个硬件传感器 (详见传感器章节) |
| **连接** | USB, Bluetooth, Wi-Fi, MFi 支持 |

## 二、软件概览

| 项目 | 详情 |
|------|------|
| **Android 版本** | 12 (API 32) |
| **内核版本** | Linux 5.10.209-perf |
| **构建版本** | 1.15.004-20260228-150202 |
| **构建指纹** | Rokid/glasses/glasses:12/SKQ1.240613.001/1.15.004-20260228-150202:user/release-keys |
| **安全补丁** | 2024-07-05 |
| **VNDK 版本** | 32 |
| **CXR 服务版本** | 1.109 |
| **编译器** | Android clang 12.0.5 |
| **构建类型** | user (正式发布版) |
| **已安装应用** | 94 个包 |
| **系统服务** | 203 个服务 |

## 三、分区结构与拉取状态

| 分区 | 设备大小 | 已拉取 | 说明 |
|------|---------|--------|------|
| `/system` | 795 MB | ~854 MB | 核心系统 (app/priv-app/lib/framework/etc/fonts) |
| `/product` | 639 MB | ~654 MB | Rokid 定制应用与配置 |
| `/vendor` | 571 MB | ~607 MB | 高通 HAL/驱动/固件 |
| `/system_ext` | 291 MB | ~311 MB | 系统扩展应用 |
| `/vendor_dlkm` | 61 MB | ~62 MB | 厂商可动态加载内核模块 |
| `/odm` | ~1 MB | ~4 KB | ODM 定制 (最小化) |
| `/sdcard` | - | ~12 MB | 用户存储空间 |
| `/data` | 19 GB | 无权限 | 需要 root 才能完整拉取 |

## 四、Rokid 核心应用详解

### 4.1 RokidSpriteAssistServer (AI 助手服务器)
- **包名**: `com.rokid.os.sprite.assistserver`
- **大小**: 224 MB (最大的 Rokid 应用)
- **位置**: `/product/app/RokidSpriteAssistServer/`
- **运行进程**: `com.rokid.os.sprite.assistserver` (UID: 1000/system)
- **核心服务**:
  - `InstructService` — 语音/手势指令处理
  - `MasterAssistService` — 主助手协调服务
  - `SpriteMediaService` — 媒体播放服务
  - `PaymentService` — 支付集成服务
  - `TtsService` — 文字转语音 (独立进程 `:tts`)
  - `WebServerService` — 内置 Web 服务器
  - `SystemFuncService` — 系统功能服务
- **角色**: 眼镜的核心 AI 大脑，协调所有其他服务和功能

### 4.2 RokidSpriteLauncher (启动器)
- **包名**: `com.rokid.os.sprite.launcher`
- **大小**: 28 MB
- **位置**: `/product/app/RokidSpriteLauncher/`
- **运行进程**: `com.rokid.os.sprite.launcher` (UID: 1000/system)
- **角色**: 眼镜主界面/启动器，通过 MasterAssistService 与助手服务器通信

### 4.3 RokidSpriteLive (实时服务)
- **包名**: `com.rokid.os.sprite.live`
- **大小**: 12 MB
- **位置**: `/product/app/RokidSpriteLive/`
- **核心服务**: `LiveService`
- **角色**: 实时交互功能 (可能包括实时翻译、实时字幕等)

### 4.4 RokidSysConfig (系统配置)
- **包名**: `com.rokid.sysconfig`
- **位置**: `/system/priv-app/RokidSysConfig/` (特权应用)
- **核心服务**: `ConfigService`
- **角色**: 系统级配置管理，以系统权限运行

### 4.5 CXRService (Cloud XR 服务)
- **包名**: `com.rokid.cxrservice`
- **位置**: `/system/app/CXRService/`
- **版本**: 1.109
- **核心服务**: `CXRService`
- **角色**: Cloud XR 渲染服务，可能用于远程渲染/AR 内容投射

### 4.6 RokidOtaUpgrade (OTA 升级)
- **包名**: `com.rokid.glass.ota`
- **大小**: 3.4 MB
- **位置**: `/product/app/RokidOtaUpgrade/`
- **核心服务**: `CheckService`, `OtaService`
- **OTA 服务器**: `https://ota.rokid.com` (API: `/v1/extended/ota/check`)
- **角色**: 系统在线升级

### 4.7 RokidScreenRecord (屏幕录制)
- **包名**: `com.rokid.os.master.screenstream`
- **大小**: 11 MB
- **位置**: `/product/app/RokidScreenRecord/`
- **角色**: 屏幕录制/投屏

## 五、支付生态

| 应用 | 包名 | 大小 | 服务 |
|------|------|------|------|
| **支付宝 (眼镜版)** | `com.eg.android.AlipayGGlasses` | 96 MB | `Glass2PayService` |
| **蚂蚁集团** | `com.antgroup.glasses` | 48 MB | `GPassService` |
| **蚂蚁支付** | `com.iap.mobile.ar_pay` | 19 MB | AR 支付 |
| **京东支付** | `com.jd.jr.joyaibuy` | 28 MB | 京东购物/支付 |

> 所有支付服务都由 `RokidSpriteAssistServer` 统一调度和绑定。

## 六、传感器系统

| 传感器 | 型号 | 类型 | 频率范围 |
|--------|------|------|---------|
| **加速度计** | ICM4x6xx (TDK-Invensense) | accelerometer | 1-500 Hz |
| **陀螺仪** | ICM4x6xx (TDK-Invensense) | gyroscope | 1-500 Hz |
| **接近传感器** | UCS146E0 (Sensortek) | proximity | 事件触发 |
| **重力传感器** | QTI 融合算法 | gravity | 5-200 Hz |
| **线性加速度** | QTI 融合算法 | linear_acceleration | 5-200 Hz |
| **游戏旋转矢量** | QTI 融合算法 | game_rotation_vector | 5-200 Hz |
| **未校准陀螺仪** | ICM4x6xx | gyroscope_uncalibrated | 1-500 Hz |
| **未校准加速度计** | ICM4x6xx | accelerometer_uncalibrated | 1-500 Hz |

### 传感器特性
- 共 16 个硬件传感器 (每种同时有 Non-wakeup 和 Wakeup 版本)
- **接近传感器** (psensor) 用于检测是否佩戴眼镜，模式：`sensitive`
- 支持 **9轴融合** (加速度计 + 陀螺仪 + 磁力计)
- FIFO 缓冲区最大 10000 事件，支持 gralloc 共享内存

## 七、交互系统

### 7.1 手势操作
- **双指点击**: `rokid.debug.two_finger_click = 1` (已启用)
- **双指轻弹**: `rokid.debug.two_finger_flick = 1` (已启用)

### 7.2 佩戴检测
- **接近传感器模式**: `rokid.psensor.mode = sensitive`
- **展开状态**: `vendor.rkd.glasses.is_spread = 1`
- **佩戴状态**: `vendor.rkd.glasses.is_take_on = 1`

### 7.3 自定义 HAL 服务
- **灯光控制**: `lights_ctrl` 服务 (`com.rokid.light.ILightsCtrl`)
- **振动器**: `android.hardware.vibrator.IVibrator`
- **传感器 HAL**: `android.hardware.sensors@2.1-service-multihal`

## 八、音频系统

### 配置文件
- `audio_policy_configuration.xml` — 主音频策略
- `mixer_paths_neo_idp.xml` / `mixer_paths_neo_idp_sg.xml` — 混音器路径
- `bluetooth_qti_audio_policy_configuration.xml` — 蓝牙音频
- `backend_conf.xml` — 后端配置

### 音频特性
- 支持 A2DP 蓝牙音频
- 支持 USB 音频
- 支持 r_submix (远端混音)
- 集成 MusicFX 音效
- TTS 独立进程运行

## 九、摄像头系统

- **数量**: 1 个摄像头
- **朝向**: 后置 (Back facing)
- **方向**: 270°
- **HAL 版本**: Camera HAL v3.7, Provider v2.7
- **支持帧率**: 15fps, 24fps, 30fps, 60fps
- **调校版本**: 4.1.4
- **传感器**: Sunny IMX681 (索尼图像传感器)
- **无闪光灯**
- **主要调用者**: `com.rokid.os.sprite.assistserver` (AI 助手用于视觉分析)

## 十、网络与连接

### Wi-Fi
- QCA (Qualcomm Atheros) WLAN 驱动
- WiFi Direct (P2P) 支持
- WiFi Display 支持

### 蓝牙
- 高通蓝牙 HAL
- BLE (低功耗蓝牙) 支持
- A2DP 音频配置

### USB
- USB 供电 (当前连接状态)
- MTP 文件传输支持
- ADB 调试支持

### MFi
- `ro.boot.glassesWithMfi = 1` (支持 Apple MFi 认证)

## 十一、显示系统

- **分辨率**: 480×640 像素
- **类型**: INTERNAL (内置显示器)
- **方向**: 竖屏 (Portrait)
- **显示 Panel**: `ro.boot.glassesWithPanel = 1`
- **GPU**: Adreno (Vulkan + OpenGL ES 3.2)
- **色彩服务**: `com.qti.service.colorservice` (高通色彩管理)
- **QDCM**: 高通显示色彩管理

## 十二、系统服务架构

```
                    ┌─────────────────────┐
                    │  RokidSpriteLauncher │ (主界面)
                    └──────────┬──────────┘
                               │ bind
                    ┌──────────▼──────────┐
                    │ RokidSpriteAssist   │ (AI 中枢)
                    │      Server         │
                    ├─────────────────────┤
                    │ • InstructService    │ ← 语音/手势
                    │ • MasterAssistService│ ← 主调度
                    │ • SpriteMediaService │ ← 媒体
                    │ • PaymentService     │ ← 支付
                    │ • TtsService (:tts)  │ ← 语音合成
                    │ • WebServerService   │ ← Web 服务
                    │ • SystemFuncService  │ ← 系统功能
                    └───┬────┬────┬───┬───┘
                        │    │    │   │
        ┌───────────────┘    │    │   └────────────────┐
        ▼                    ▼    ▼                    ▼
┌──────────────┐  ┌──────────┐  ┌───────────┐  ┌──────────────┐
│ SpriteLive   │  │ Alipay   │  │ AntGroup  │  │ CXRService   │
│ (实时服务)    │  │ Glass2Pay│  │ GPass     │  │ (Cloud XR)   │
└──────────────┘  └──────────┘  └───────────┘  └──────────────┘

        ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
        │ RokidSysConf │  │ RokidOTA     │  │ ScreenRecord │
        │ (系统配置)    │  │ (OTA升级)     │  │ (屏幕录制)    │
        └──────────────┘  └──────────────┘  └──────────────┘
```

## 十三、目录结构说明

```
20260313_rokid_system/
├── device_info/            # 设备信息转储
│   ├── all_properties.txt  # 所有系统属性
│   ├── all_packages.txt    # 所有已安装包
│   ├── running_services.txt# 运行中的服务
│   ├── package_details.txt # 包详细信息
│   ├── sensors.txt         # 传感器信息
│   ├── camera.txt          # 摄像头信息
│   ├── display.txt         # 显示信息
│   ├── audio.txt           # 音频信息
│   ├── bluetooth.txt       # 蓝牙信息
│   ├── wifi.txt            # Wi-Fi 信息
│   ├── input_devices.txt   # 输入设备
│   ├── battery.txt         # 电池信息
│   ├── power.txt           # 电源信息
│   ├── usb.txt             # USB 信息
│   ├── cpuinfo.txt         # CPU 信息
│   ├── meminfo.txt         # 内存信息
│   ├── kernel_version.txt  # 内核版本
│   ├── service_list.txt    # 系统服务列表
│   ├── activities.txt      # Activity 信息
│   ├── connectivity.txt    # 网络连接
│   ├── location.txt        # 位置服务
│   ├── content_providers.txt# 内容提供者
│   ├── notification.txt    # 通知信息
│   └── window.txt          # 窗口管理
├── system/                 # /system 分区
│   ├── app/                # 系统应用 (14个)
│   ├── priv-app/           # 特权应用 (31个, 含 RokidSysConfig)
│   ├── etc/                # 系统配置文件
│   ├── framework/          # 框架 JAR 和 VDEX
│   ├── lib/ & lib64/       # 系统库 (32/64位)
│   ├── fonts/              # 字体文件
│   ├── apex/               # APEX 模块
│   └── usr/                # 用户空间工具/键盘布局
├── product/                # /product 分区 (Rokid 定制)
│   └── product/
│       ├── app/            # Rokid 应用 (12个)
│       │   ├── RokidSpriteAssistServer/ (224MB - AI 核心)
│       │   ├── RokidSpriteLauncher/     (28MB - 启动器)
│       │   ├── RokidSpriteLive/         (12MB - 实时服务)
│       │   ├── RokidOtaUpgrade/         (3.4MB - OTA)
│       │   ├── RokidScreenRecord/       (11MB - 录屏)
│       │   ├── Alipay/                  (96MB - 支付宝)
│       │   ├── AntGroup/                (48MB - 蚂蚁集团)
│       │   ├── AntPay/                  (19MB - 蚂蚁支付)
│       │   ├── JdPay/                   (28MB - 京东支付)
│       │   ├── Camera2/                 (10MB - 相机)
│       │   └── webview/                 (161MB - WebView)
│       ├── priv-app/       # 特权应用
│       ├── overlay/        # 资源覆盖层 (12个)
│       ├── etc/            # 产品配置
│       └── lib/ & lib64/   # 产品库
├── vendor/                 # /vendor 分区 (高通 HAL)
│   ├── app/                # 厂商应用
│   ├── etc/                # 厂商配置
│   │   ├── sensors/        # 传感器配置 (44个 JSON)
│   │   ├── init/           # init 服务脚本 (65个)
│   │   ├── CaliData_For_RokidGlasses.bin  # Rokid 校准数据
│   │   ├── audio_*         # 音频配置
│   │   ├── media_*         # 媒体编解码配置
│   │   ├── mixer_paths_*   # 混音器路径
│   │   └── thermal-engine.conf  # 热管理
│   ├── firmware/           # 固件文件 (96个)
│   ├── lib/ & lib64/       # HAL 库
│   └── overlay/            # 厂商覆盖层
├── system_ext/             # /system_ext 分区
│   ├── app/                # 扩展应用 (含 Settings)
│   ├── priv-app/           # 扩展特权应用
│   └── framework/          # 扩展框架
├── vendor_dlkm/            # 可加载内核模块 (161个 .ko)
├── odm/                    # ODM 定制 (最小化)
├── sdcard/                 # 用户存储
│   └── sdcard/
│       ├── DCIM/           # 照片
│       ├── Movies/         # 视频
│       ├── Pictures/       # 图片
│       ├── SpatialPhotos/  # 空间照片
│       └── ...
└── SYSTEM_OVERVIEW.md      # 本文档
```

## 十四、值得深入研究的方向

### 14.1 AI 功能
- [ ] 反编译 `RokidSpriteAssistServer.apk` 分析 AI 交互逻辑
- [ ] 研究 `InstructService` 的语音指令处理流程
- [ ] 分析 `WebServerService` 的内置 Web 接口
- [ ] 研究 `TtsService` 的语音合成方案

### 14.2 AR/XR 功能
- [ ] 分析 `CXRService` 的 Cloud XR 渲染管线
- [ ] 研究 IMU 传感器数据在 AR 追踪中的应用
- [ ] 分析 `SpatialPhotos` 空间照片格式

### 14.3 支付系统
- [ ] 研究 Alipay Glass2Pay 的刷脸/码牌支付流程
- [ ] 分析 `PaymentService` 如何统一调度多支付渠道
- [ ] 研究 AntGroup GPassService 的通行证功能

### 14.4 交互体验
- [ ] 研究双指手势的识别与处理逻辑
- [ ] 分析接近传感器的佩戴检测触发流程
- [ ] 研究 `SpriteLauncher` 的 UI 渲染方式 (480×640)

### 14.5 系统定制
- [ ] 分析 Rokid overlay 资源的 UI 定制
- [ ] 研究 `RokidSysConfig` 的系统配置项
- [ ] 分析 `CaliData_For_RokidGlasses.bin` 校准数据格式
- [ ] 研究 `lights_ctrl` 自定义 LED 灯光控制接口

### 14.6 摄像头与视觉
- [ ] 分析 Sony IMX681 传感器的配置与调校
- [ ] 研究 `RokidSpriteAssistServer` 的视觉分析功能
- [ ] 分析摄像头在 AI 场景识别中的应用

## 十五、已知限制

1. **无 root 权限**: `/data` 分区、部分 `build.prop` 和少量厂商文件无法拉取
2. **二进制文件**: APK 需要反编译才能深入分析代码逻辑
3. **加密/混淆**: Rokid 核心应用可能有代码混淆
4. **运行时数据**: SharedPreferences、数据库等需要 root 才能读取
