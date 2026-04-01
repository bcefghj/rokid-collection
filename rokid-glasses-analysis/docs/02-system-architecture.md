# 系统架构深度解析

> 理解 Rokid AI 眼镜从开机到运行的完整系统架构

---

## 系统基本信息

| 项目 | 详情 |
|------|------|
| **Android 版本** | 12 (API Level 32) |
| **Linux 内核** | 5.10.209-perf |
| **构建版本** | 1.15.004-20260228-150202 |
| **构建指纹** | `Rokid/glasses/glasses:12/SKQ1.240613.001/...` |
| **安全补丁** | 2024-07-05 |
| **已安装应用** | 94 个 |
| **系统服务** | 203 个 |

## 分区结构详解

### 各分区职责

```
/system  (795MB)
├── app/          ── 14 个系统应用 (CXRService 等)
├── priv-app/     ── 31 个特权应用 (RokidSysConfig、Settings 等)
├── etc/          ── 系统配置文件
├── framework/    ── Android 框架 JAR 和 VDEX
├── lib/ & lib64/ ── 系统库 (32/64位)
├── fonts/        ── 字体文件
├── apex/         ── APEX 模块
└── usr/          ── 键盘布局等

/product  (639MB)  ← Rokid 定制内容
└── product/
    ├── app/      ── 12 个 Rokid 应用
    │   ├── RokidSpriteAssistServer/  (224MB - AI 核心)
    │   ├── RokidSpriteLauncher/      (28MB - 启动器)
    │   ├── RokidSpriteLive/          (12MB - 直播)
    │   ├── RokidOtaUpgrade/          (3.4MB - OTA)
    │   ├── RokidScreenRecord/        (11MB - 录屏)
    │   ├── Alipay/                   (96MB - 支付宝)
    │   ├── AntGroup/                 (48MB - 蚂蚁集团)
    │   ├── AntPay/                   (19MB - 蚂蚁支付)
    │   ├── JdPay/                    (28MB - 京东支付)
    │   ├── Camera2/                  (10MB - 相机)
    │   └── webview/                  (161MB - WebView)
    ├── overlay/  ── 12 个资源覆盖层 (UI 定制)
    └── etc/      ── 产品配置

/vendor  (571MB)  ← 高通芯片相关
├── app/          ── 厂商应用
├── etc/
│   ├── sensors/  ── 44 个传感器配置 JSON
│   ├── init/     ── 65 个 init 服务脚本
│   ├── audio_*   ── 音频配置
│   ├── media_*   ── 媒体编解码配置
│   └── thermal-engine.conf  ── 热管理
├── firmware/     ── 96 个固件文件
├── lib/ & lib64/ ── HAL 库
└── overlay/      ── 厂商覆盖层

/system_ext  (291MB)
├── app/          ── Settings 等系统扩展应用
├── priv-app/     ── 扩展特权应用
└── framework/    ── 扩展框架

/vendor_dlkm  (61MB)
└── lib/modules/  ── 161 个可加载内核模块 (.ko)

/odm  (1MB)       ── ODM 定制 (几乎为空)

/data  (19GB)     ── 用户数据 (需 root 访问)
```

## 启动流程详解

### 第一阶段：Linux 内核启动

```
Linux Kernel 5.10.209-perf 启动
  ↓
加载 161 个内核模块 (vendor_dlkm)
  ↓
挂载各分区 (system/product/vendor...)
  ↓
启动 Android init 进程
```

### 第二阶段：HAL 服务启动

Android init 进程读取 vendor 下的 66 个 `.rc` 脚本，启动硬件抽象层服务：

```
init 进程
  ├── audio HAL ──────── 音频硬件驱动
  ├── display HAL ────── 显示硬件驱动
  ├── sensors HAL ────── 传感器驱动 (multi-hal 2.1)
  ├── bluetooth HAL ──── 蓝牙驱动
  ├── camera HAL ─────── 相机驱动 (v3.7)
  ├── wifi HAL ────────── WiFi 驱动 (QCA)
  ├── lights_ctrl ────── Rokid LED 灯光控制
  ├── vibrator HAL ───── 振动器
  └── ... (其他 HAL 服务)
```

### 第三阶段：Rokid 核心服务启动

```
persistent 应用自动启动（不需要等 BOOT_COMPLETED）
  │
  ├── CXRService (com.rokid.cxrservice)
  │   └── 启动蓝牙通信服务，等待手机连接
  │
  ├── RokidSysConfig (com.rokid.sysconfig)
  │   ├── 启动 ConfigService
  │   ├── 初始化 PsensorObserver (佩戴检测)
  │   ├── 初始化 LED 灯光控制
  │   └── 设置折叠关机策略
  │
  └── RokidSpriteAssistServer (com.rokid.os.sprite.assistserver)
      ├── MasterAssistService ── 总协调中心
      ├── InstructService ───── 离线语音引擎初始化
      ├── SpriteMediaService ── 相机/媒体管理
      ├── PaymentService ────── 支付服务初始化
      ├── TtsService ─────────── TTS 引擎初始化 (独立 :tts 进程)
      ├── RokidBluetoothService ── 蓝牙管理 (15 个 Manager)
      ├── WebServerService ───── Web 服务器启动 (端口 8848)
      ├── SpriteWifiService ──── WiFi 管理
      └── JsAiService ─────────── JS AI 引擎
```

### 第四阶段：BOOT_COMPLETED 后启动

```
系统发送 BOOT_COMPLETED 广播
  │
  ├── RokidSpriteLauncher ── 主界面启动，显示中心页
  ├── RokidOtaUpgrade ────── 开始检查是否有系统更新
  └── 支付应用 ────────────── 支付宝/蚂蚁/京东等初始化
```

## Rokid 的架构设计理念

### 为什么用 APK 而不用原生服务？

传统 Android 设备厂商会把核心功能写成 C++ 的 native 服务，但 Rokid 选择了**全部用 APK 应用**来实现：

| 方式 | 优点 | 缺点 |
|------|------|------|
| **Rokid 方式 (APK)** | 易于更新（OTA 可单独更新）、开发效率高、可用 Java/Kotlin | 性能略低、启动略慢 |
| **传统方式 (Native)** | 性能高、启动快 | 更新困难、开发周期长 |

Rokid 通过 `persistent=true` 标记让核心 APK 常驻内存，加上 `android.uid.system` 系统权限，让 APK 拥有和原生服务几乎一样的能力。

### 服务间通信方式

```
                        AIDL 绑定
Launcher ←──────────────────────────→ AssistServer
                                         │
                        Broadcast        │ AIDL
各功能模块 ←─────────────────────────────┤
                                         │ ContentProvider
系统设置 ←───────────────────────────────┤
                                         │ Caps 协议 (蓝牙)
手机 App ←───────────────────────────────┘
```

## 关键系统属性

| 属性 | 值 | 说明 |
|------|-----|------|
| `ro.product.model` | RG-glasses | 设备型号 |
| `ro.product.brand` | Rokid | 品牌 |
| `ro.build.version.release` | 12 | Android 版本 |
| `vendor.rkd.glasses.is_take_on` | 0/1 | 佩戴状态 |
| `vendor.rkd.glasses.is_spread` | 0/1 | 镜腿展开状态 |
| `rokid.psensor.mode` | sensitive | 接近传感器模式 |
| `rokid.cxr-service.version` | 1.109 | CXR 服务版本 |
| `ro.boot.glassesWithMfi` | 1 | 支持 Apple MFi |
| `ro.boot.glassesWithPanel` | 1 | 有显示面板 |
