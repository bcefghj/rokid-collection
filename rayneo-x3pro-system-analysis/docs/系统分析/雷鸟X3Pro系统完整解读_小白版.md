# 雷鸟 X3 Pro 系统完整解读（小白版）

> 把一副 AR 眼镜拆解到每个零件，让你彻底搞懂它是怎么运行的。

---

## 第一章：这副眼镜到底是什么？

### 1.1 一句话概括

**雷鸟 X3 Pro 本质上是一台"戴在脸上的 Android 手机"。**

它跑的是完整的 Android 12 系统，有 CPU、内存、存储、WiFi、蓝牙、摄像头、传感器——手机有的它基本都有。只是它没有手机那块触摸屏，取而代之的是两块投射到你眼前的 MicroLED 微型显示屏。

### 1.2 核心硬件一览

```
┌─────────────────────────────────────────────────────────────┐
│                    雷鸟 X3 Pro 硬件架构                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐   │
│  │   大脑       │    │   眼睛        │    │   耳朵         │   │
│  │ 骁龙AR1芯片  │    │ 双MicroLED屏  │    │ 3个麦克风      │   │
│  │ 4核 ARM64   │    │ 640×480×2    │    │ 2个立体声扬声器 │   │
│  │ 3.7GB内存   │    │ FOV 30°      │    │               │   │
│  │ 32GB存储    │    │ 峰值6000nit  │    │               │   │
│  └─────────────┘    └──────────────┘    └───────────────┘   │
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐   │
│  │   摄像头     │    │   感知系统    │    │   连接        │   │
│  │ 1200万RGB   │    │ 加速度计      │    │ WiFi 6       │   │
│  │ VGA空间相机  │    │ 陀螺仪       │    │ 蓝牙5.2      │   │
│  │             │    │ 磁力计       │    │ USB-C 2.0    │   │
│  │             │    │ 光线传感器    │    │              │   │
│  └─────────────┘    └──────────────┘    └───────────────┘   │
│                                                             │
│  电池：245mAh 锂离子  │  重量：76g  │  系统：Android 12        │
└─────────────────────────────────────────────────────────────┘
```

---

## 第二章：CPU 和内存——大脑怎么工作的

### 2.1 CPU：4 核 ARM 处理器

X3 Pro 搭载**高通骁龙 AR1 Gen1**芯片，代号 `neo`。

CPU 有 **4 个核心**，全部是 ARM Cortex-A55 架构（`CPU part: 0xd05`），64 位（aarch64）。

**什么意思？**
- 4 核 = 可以同时做 4 件事（比如一边运行系统界面，一边跑 AI 语音，一边处理相机画面，一边播放音频）
- Cortex-A55 = ARM 的高效率核心（省电但性能不极端强悍，适合 AR 眼镜这种需要长时间使用的设备）
- 主频约 1.9GHz（从 BogoMIPS 38.40 推算）

**和手机对比：**
- 手机通常是 8 核（4 大核 + 4 小核），X3 Pro 只有 4 个效率核
- 这是因为 AR 眼镜最重要的是**省电和不发热**，而不是极致性能

### 2.2 内存：3.7GB

```
总内存：3,865,588 KB ≈ 3.7GB
可用内存：2,258,084 KB ≈ 2.2GB
交换空间(Swap)：2GB（当内存不够时，临时借用存储当内存用）
```

**什么意思？**
- 系统标注 4GB RAM，实际可用约 3.7GB（一部分被硬件保留了）
- 开机后系统本身占约 1.5GB，剩余约 2.2GB 给应用使用
- 系统标记为 `ro.config.low_ram=true`——这意味着系统知道自己内存有限，会更积极地杀后台应用来省内存

### 2.3 存储：32GB eMMC

```
/data 分区（装应用和用户数据）：22GB，已用 6.8GB
/system（系统文件）：931MB
/vendor（驱动程序）：619MB
/system_ext（系统扩展）：1.7GB
/product（预装应用）：418MB
```

**注意：** 存储芯片连接方式是 eMMC（`7c4000.sdhci`），不是 UFS。eMMC 读写速度比 UFS 慢，但对于 AR 眼镜来说完全够用。

---

## 第三章：显示系统——左右两块屏幕怎么工作

### 3.1 双屏原理

这是 X3 Pro 和手机**最大的区别**。

```
普通手机：                    X3 Pro：

┌──────────┐              ┌─────┐  ┌─────┐
│          │              │ 左屏 │  │ 右屏 │
│  一块屏幕  │              │     │  │     │
│          │              │     │  │     │
└──────────┘              └─────┘  └─────┘
                              ↓        ↓
                           左眼看到  右眼看到
```

系统把两块屏幕拼成一块**逻辑屏幕**，分辨率 640×480（每只眼睛的物理分辨率）。

**如果你直接装一个普通手机 App 上去，会发生什么？**
- App 的界面会被拆成左半和右半，分别显示在两只眼睛上
- 结果就是画面撕裂——左眼看到左半部分，右眼看到右半部分，叠在一起很难看

**解决方案：合目（Mirror）**
- 雷鸟 SDK 提供了"合目组件"，自动把**同样的画面**复制到左右两边
- 左眼和右眼看到一样的 UI，叠加起来就正常了
- 还可以让左右画面有**微小偏移**，形成 3D 立体效果（视差）

### 3.2 显示参数详解

| 参数 | 值 | 含义 |
|------|-----|------|
| 屏幕密度 | 160 dpi (系统层) / 480 dpi (硬件层) | 系统用 160dpi 来布局，实际硬件 480dpi |
| FOV | 30° 对角线 | 你能看到的画面大小，约等于 2 米外一个 27 寸显示器 |
| 峰值亮度 | 6000 nit | 非常亮，户外阳光下也能看清 |
| 刷新率 | 最高 60Hz | 每秒刷新 60 次画面 |
| 渲染引擎 | Adreno GPU (OpenGL ES 3.2, Vulkan 1.1) | 高通自家图形处理器 |
| SurfaceFlinger | 系统合成器 | 负责把各个应用的画面合成为最终显示帧 |

### 3.3 显示链路（画面从代码到你眼前的全过程）

```
App 绘制 UI
    ↓
OpenGL ES / Vulkan 渲染
    ↓
SurfaceFlinger（Android 显示合成器）
    ↓
HWC（Hardware Composer，高通显示硬件合成器）
    ↓
DSI 接口（Display Serial Interface）
    ↓
双路 MicroLED 光引擎
    ↓
衍射光波导（把光线导入你的眼睛）
    ↓
你看到的画面 ✨
```

---

## 第四章：传感器系统——眼镜怎么感知世界

X3 Pro 内置了 **23 个硬件传感器**。这些传感器让眼镜知道自己的姿态、环境和用户状态。

### 4.1 核心传感器详解

#### 加速度计（Accelerometer）
- **芯片**：STMicro LSM6DSR（意法半导体出品）
- **采样率**：最高 415.97Hz（每秒测量 416 次）
- **作用**：测量眼镜在三个方向上的加速度（前后、左右、上下）
- **实际用途**：
  - 检测你是否在走路、跑步
  - 判断你的头是朝上还是朝下
  - 配合陀螺仪实现头部追踪

#### 陀螺仪（Gyroscope）
- **芯片**：同一块 LSM6DSR（加速度计和陀螺仪集成在一颗芯片里）
- **采样率**：最高 415.97Hz
- **作用**：测量眼镜的旋转速度（点头、摇头、歪头）
- **实际用途**：
  - 3DoF 头部追踪——当你转头时，AR 画面跟着移动
  - 防抖——你稍微晃动头部，画面不会乱跳

#### 磁力计（Magnetometer）
- **芯片**：MEMSIC MMC56X3X
- **采样率**：最高 100Hz
- **作用**：感知地球磁场，相当于电子罗盘
- **实际用途**：
  - 确定方向（东南西北）
  - 导航应用需要知道你面朝哪个方向

#### 光线传感器（Ambient Light Sensor）
- **芯片**：Sensortek STK3X3X
- **作用**：测量环境光亮度
- **实际用途**：
  - 自动亮度调节——室外更亮，室内更暗
  - 系统的 `AutomaticBrightnessController` 每 250ms 读取一次

#### 抬手检测（Pick Up Gesture）
- **芯片**：LSM6DSR 的内置功能
- **类型**：一次性触发（检测到就报告一次）
- **作用**：检测你是否拿起/戴上眼镜
- **实际用途**：
  - 戴上眼镜自动亮屏
  - 系统中 `HeadTrigger`（头部触发器）用这个来唤醒/休眠

### 4.2 软件融合传感器

除了硬件传感器，系统还通过算法**融合**多个传感器数据，得到更高级的感知：

| 融合传感器 | 数据来源 | 作用 |
|-----------|---------|------|
| 重力传感器 | 加速度计 + 陀螺仪 | 精确知道"下方"在哪里 |
| 线性加速度 | 加速度计 - 重力 | 只测移动加速度，去掉重力干扰 |
| 旋转向量 | 加速度计 + 陀螺仪 + 磁力计 | 9 轴融合，精确知道眼镜指向哪里 |
| 游戏旋转向量 | 加速度计 + 陀螺仪（不含磁力计）| 6 轴融合，不受磁场干扰，适合 AR 内容 |
| 方向传感器 | 全部融合 | 给出俯仰角、偏航角、翻滚角 |

### 4.3 传感器实际运作过程

从实际日志可以看到，传感器是这样被使用的：

```
                            ┌────────────────────────┐
                            │     传感器硬件芯片       │
                            │ LSM6DSR / MMC56X3X等    │
                            └──────────┬─────────────┘
                                       ↓
                            ┌────────────────────────┐
                            │   Sensor HAL（硬件抽象层）│
                            │ 高通 QTI 传感器服务      │
                            └──────────┬─────────────┘
                                       ↓
                            ┌────────────────────────┐
                            │   SensorService         │
                            │ Android 传感器管理器     │
                            └──────────┬─────────────┘
                                       ↓
              ┌─────────────────┬──────┴─────┬────────────────┐
              ↓                 ↓            ↓                ↓
    ┌─────────────────┐ ┌──────────┐ ┌──────────────┐ ┌────────────┐
    │ Mercury Launcher │ │ XR Runtime│ │ 导航（高德） │ │ 亮度控制器 │
    │ HeadTrigger     │ │ 头部追踪  │ │ IMU 定位     │ │ 光感读取  │
    │ 抬头唤醒         │ │ 3DoF渲染  │ │ 步行检测     │ │ 自动调光  │
    └─────────────────┘ └──────────┘ └──────────────┘ └────────────┘
```

---

## 第五章：系统架构——软件怎么分层运作

### 5.1 系统分层图

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层（Apps）                             │
│  Launcher │ 导航 │ 翻译 │ 相机 │ AI语音 │ 提词器 │ 第三方App      │
├─────────────────────────────────────────────────────────────────┤
│                    雷鸟定制层（RayNeo Layer）                     │
│  XR Runtime │ AI Runtime │ Mercury Launcher │ App Container     │
├─────────────────────────────────────────────────────────────────┤
│                  Android Framework（系统框架）                    │
│  Activity Manager │ Window Manager │ SensorService │ AudioServer │
│  CameraService │ SurfaceFlinger │ PackageManager │ ...          │
│  共 218 个系统服务                                               │
├─────────────────────────────────────────────────────────────────┤
│                     HAL（硬件抽象层）                             │
│  Audio HAL │ Camera HAL │ Sensor HAL │ Display HAL │ Power HAL  │
│  Bluetooth HAL │ WiFi HAL │ USB HAL │ Vibrator HAL              │
├─────────────────────────────────────────────────────────────────┤
│                   Linux 内核（Kernel 5.10.226）                  │
│  ARM SMMU │ eMMC驱动 │ GPU(kgsl) │ 传感器驱动 │ 显示驱动(DSI)   │
│  WiFi(cnss) │ 蓝牙 │ USB(dwc3) │ 相机(cam_cpas) │ 音频(aw883xx) │
├─────────────────────────────────────────────────────────────────┤
│                      硬件（Hardware）                            │
│  骁龙AR1 │ MicroLED │ IMU │ 摄像头 │ 麦克风 │ 扬声器 │ 电池      │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 各分区是干什么的

把 X3 Pro 的存储想象成一栋大楼，每层住着不同的"住户"：

```
┌──────────────────────────────────────────────────────┐
│ 6F: /data (22GB)                                     │
│     "你的房间"——所有用户数据、安装的第三方 App、       │
│     照片、下载的文件都在这里                            │
├──────────────────────────────────────────────────────┤
│ 5F: /system_ext (1.7GB)                              │
│     "雷鸟定制办公室"——雷鸟自己开发的所有核心应用：      │
│     Launcher、XR Runtime、AI Runtime、相机、导航、     │
│     翻译、提词器、音乐、笔记、电话、录音、SystemUI      │
├──────────────────────────────────────────────────────┤
│ 4F: /system (931MB)                                  │
│     "Android 总部"——Android 原生系统文件、蓝牙、       │
│     基础框架(framework.jar)、字体、命令行工具           │
├──────────────────────────────────────────────────────┤
│ 3F: /product (418MB)                                 │
│     "预装区"——搜狗输入法、支付宝眼镜版、通讯录、       │
│     WebView、各种系统界面覆盖层                        │
├──────────────────────────────────────────────────────┤
│ 2F: /vendor (619MB)                                  │
│     "高通驱动中心"——所有硬件驱动、高通相机服务、        │
│     显示合成器、音频 DSP、传感器 HAL、GPU 驱动          │
├──────────────────────────────────────────────────────┤
│ 1F: Linux 内核                                       │
│     "地基"——操作系统的核心，管理一切硬件资源            │
└──────────────────────────────────────────────────────┘
```

---

## 第六章：启动过程——按下电源键后发生了什么

### 6.1 完整启动流程

```
按下电源键
    ↓ (约0.5秒)
[1] Bootloader 引导加载器启动
    - 检查 AVB（验证启动，确保系统没被篡改）
    - 加载 Linux 内核到内存
    ↓ (约2秒)
[2] Linux 内核启动 (Kernel 5.10.226)
    - 初始化 CPU（4核全部激活）
    - 挂载各个分区 (system/vendor/data等)
    - 启动 init 进程（PID=1，所有进程的老祖宗）
    ↓ (约3秒)
[3] init 进程启动核心服务
    - servicemanager (系统服务管家)
    - hwservicemanager (硬件服务管家)
    - vold (存储管理)
    - logd (日志系统)
    - lmkd (低内存杀手，内存不够时杀后台应用)
    ↓ (约2秒)
[4] 硬件 HAL 服务启动
    - audioserver (音频)
    - cameraserver (相机)
    - surfaceflinger (显示合成)
    - sensorservice (传感器)
    - wificond (WiFi)
    - 蓝牙、GPU、USB 等等
    ↓ (约3秒)
[5] Zygote 进程启动（Android 应用的"孵化器"）
    - Zygote 预加载 Java 框架
    - 启动 system_server（Android 的"大脑"）
    ↓ (约5秒)
[6] system_server 启动（PID=1288，最重要的进程！）
    - 启动全部 218 个系统服务
    - ActivityManager（管理应用启动/切换）
    - WindowManager（管理窗口显示）
    - PackageManager（管理应用安装/卸载）
    - PowerManager（电源管理）
    - 等等...
    ↓ (约2秒)
[7] Launcher 启动！（你看到眼镜主界面了）
    - com.ffalconxr.mercury.launcher (Mercury Launcher)
    - 占用内存约 230MB（最吃内存的应用之一）
    - 同时启动 SystemUI（状态栏、通知等）
    ↓
[8] 后台服务陆续启动
    - XR Runtime (com.rayneo.xr.runtime) → AR 运行时
    - AI Runtime (com.rayneo.airuntime) → AI 引擎
    - AI Speech (com.rayneo.aispeech) → 语音助手
    - 相机、音乐、录音等服务待命
    ↓
[9] 启动完成！整个过程约 15 秒
    (从 system_server 日志：start_elapsed=15191ms)
```

### 6.2 A/B 分区（双系统保险）

X3 Pro 使用 **A/B 分区方案**（`ro.build.ab_update=true`）：

```
存储上实际有两套系统：
┌─────────────────┐  ┌─────────────────┐
│   Slot A (当前)   │  │   Slot B (备用)   │
│  system_a        │  │  system_b        │
│  vendor_a        │  │  vendor_b        │
│  boot_a          │  │  boot_b          │
└─────────────────┘  └─────────────────┘
```

**为什么？** OTA 系统更新时：
1. 新系统写入 Slot B（不影响当前系统）
2. 写完后，重启切换到 Slot B
3. 如果新系统有问题，自动回滚到 Slot A
4. 这样更新永远不会变砖

---

## 第七章：雷鸟定制应用——每个 App 在做什么

### 7.1 核心应用架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     用户可见的应用                            │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐    │
│  │ 相机  │ │ 导航  │ │ 翻译  │ │提词器 │ │ 笔记 │ │ 音乐  │    │
│  └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘    │
├─────┼────────┼────────┼────────┼────────┼────────┼─────────┤
│     ↓        ↓        ↓        ↓        ↓        ↓         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Mercury Launcher（主桌面）                │   │
│  │   - 应用启动器、主界面管理                              │   │
│  │   - HeadTrigger：检测戴上/摘下自动亮屏/息屏              │   │
│  │   - 占用内存 ~230MB（最大的应用进程）                     │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         ↓                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ XR Runtime   │  │ AI Runtime   │  │ App Container    │   │
│  │ AR运行时核心  │  │ AI引擎       │  │ Android虚拟机容器│   │
│  │ - 管理相机    │  │ - AI大模型   │  │ - 运行手机App   │   │
│  │ - 3DoF追踪   │  │ - 视觉AI     │  │ - 隔离环境      │   │
│  │ - 传感器数据  │  │ - 推理引擎   │  │                 │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    AI 语音系统                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ AI Speech (com.rayneo.aispeech)                      │   │
│  │ - 主服务进程 + interactor子进程 + 唤醒词检测子进程      │   │
│  │ - 3个独立进程协同工作                                  │   │
│  │ - rhotword: 始终监听唤醒词                             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 各应用详细解读

| 包名 | 应用名 | 说明 | 运行时内存 |
|------|--------|------|-----------|
| `com.ffalconxr.mercury.launcher` | **Mercury Launcher** | 主桌面系统。代号"水星"(Mercury)。负责应用启动、主界面、锁屏。`HeadTrigger` 功能通过重力传感器+抬手检测实现戴上自动亮屏 | ~234MB |
| `com.rayneo.xr.runtime` | **XR Runtime** | AR 运行时核心。管理摄像头（从日志看它是主要的 camera 客户端）、3DoF 头部追踪、传感器数据分发。所有 AR 应用的基础 | ~60MB |
| `com.rayneo.airuntime` | **AI Runtime** | AI 推理引擎。运行在 system 权限下。负责本地 AI 模型推理（如语音识别、图像识别）。与 CDSP/ADSP（高通 DSP 处理器）配合工作 | ~110MB |
| `com.rayneo.aispeech` | **AI 语音** | 语音助手。有 3 个子进程：主服务 + interactor（交互） + rhotword（唤醒词检测，始终在后台监听"你好雷鸟"之类的唤醒词） | ~98MB+67MB |
| `com.rayneo.live.ai` | **Live AI** | 实时 AI 处理。可能用于实时翻译、实时字幕等需要持续 AI 处理的场景 | ~60MB |
| `com.ffalcon.navigation` | **导航** | 内置导航应用。集成了高德地图 SDK（`com.amap.api`），使用加速度计+磁力计+陀螺仪进行步行导航定位 | - |
| `com.ffalcon.translate` | **翻译** | 实时翻译应用 | - |
| `com.ffalcon.wordprompt` | **提词器** | 在眼前显示演讲/会议提示词 | - |
| `com.leiniao.camera` | **相机** | 拍照/录像。包名 `leiniao` 是"雷鸟"的拼音。使用 GSensorUtil 获取方向来旋转照片 | ~132MB |
| `com.rayneo.media` | **媒体播放** | 音乐/视频播放器 | ~106MB |
| `com.rayneo.gallery` | **图库** | 照片和视频浏览 | ~133MB |
| `com.rayneo.notes` | **笔记** | 简单笔记应用 | ~61MB |
| `com.rayneo.phone` | **电话** | 蓝牙通话功能（通过连接手机） | ~88MB |
| `com.rayneo.record` | **录音/录屏** | 录音和屏幕录制 | ~92MB |
| `com.ffalcon.appcontainer` | **App Container** | Android 应用容器/虚拟机。让普通手机 App 能在眼镜上运行。有个 `:floatball` 子进程（悬浮球入口） | ~61MB |
| `com.ffalcon.calibration` | **校准工具** | 显示校准——让左右两屏的画面对准 | - |

### 7.3 第三方预装应用

| 包名 | 应用 | 安装位置 |
|------|------|---------|
| `com.eg.android.AlipayGGlasses` | **支付宝（眼镜版）** | /product/priv-app（预装）|
| `com.sogou.inputmethod.iot` | **搜狗输入法（IoT版）** | /product/priv-app（预装）|
| `com.tencent.mm` | **微信** | /data（用户安装）|
| `com.tencent.wemeet.app` | **腾讯会议** | /data（用户安装）|
| `tv.danmaku.bili` | **哔哩哔哩** | /data（用户安装）|
| `com.xingin.xhs` | **小红书** | /data（用户安装）|
| `com.sina.weibo` | **微博** | /data（用户安装）|
| `com.tencent.qqmusicpad` | **QQ音乐** | /data（用户安装）|
| `com.phoenix.read` | **凤凰阅读** | /data（用户安装）|
| `com.rayneo.lifeai` | **LifeAI** | /data（用户安装）|
| `com.rayneo.airscreen` | **AirScreen** | /data（用户安装）|
| `com.rayneogame.skiing` | **滑雪游戏** | /data（用户安装）|
| `com.RayneoGame.ARPet` | **AR 宠物** | /data（用户安装）|
| `com.rayneogame.pinball` | **弹球游戏** | /data（用户安装）|
| `com.Ffalcon.BEATSPACE` | **BeatSpace 音游** | /data（用户安装）|

---

## 第八章：触控和交互——没有触摸屏怎么操作

### 8.1 交互方式

X3 Pro 的右侧镜腿有一个**触控板**（TouchPad），支持 4 向操控：

```
                    ↑ 上滑
                    │
        ← 前滑 ────┼──── 后滑 →
                    │
                    ↓ 下滑

        单击 = 确认       双击 = 返回
        长按 = 更多选项    双指点击 = 新手势
```

**系统属性中的关键配置：**
```
debug.rayneo.tp.enable = 1        ← 触控板已启用
debug.rayneo.tp.rotation = 0      ← 触控板方向（0=正常）
debug.rayneo.tp.sleep = 1         ← 支持触控板休眠省电
persist.input.touch_slop = 65     ← 触控灵敏度阈值（像素）
persist.input.double_tap_slop = 470 ← 双击判定范围
```

### 8.2 SDK 中的手势映射

```
触控板原始事件 (MotionEvent)
    ↓
TouchDispatcher（手势识别器）
    ↓
转换为 TempleAction 对象：
    ├── SlideForward  → 前滑（向镜片方向）
    ├── SlideBackward → 后滑（向耳朵方向）
    ├── SlideUp       → 上滑（X3 新增）
    ├── SlideDown     → 下滑（X3 新增）
    ├── Click         → 单击
    ├── DoubleClick   → 双击
    ├── LongPress     → 长按
    └── TwoFingerClick→ 双指点击（X3 新增）
    ↓
应用通过 Kotlin Flow 接收手势事件
```

---

## 第九章：系统服务——218 个服务都在干嘛

系统后台运行着 **218 个服务**。以下是最重要的分类：

### 9.1 雷鸟专属服务

| 服务名 | 作用 |
|--------|------|
| `rayneo_suspend` | 雷鸟休眠管理器——控制眼镜的亮屏/息屏/省电模式 |
| `vendor.xrschedulerservice` | XR 调度服务——为 AR 渲染提供高优先级 CPU 调度 |
| `vendor.audio.vrservice` | VR 音频服务——空间音频处理 |
| `vendor.perfservice` | 高通性能服务——动态调节 CPU/GPU 频率 |

### 9.2 核心 Android 服务（最重要的 10 个）

| 服务 | 说明 |
|------|------|
| `activity` | 管理所有应用的启动、切换、生命周期 |
| `window` | 管理窗口——每个应用占多大、在哪里显示 |
| `SurfaceFlinger` | 把所有应用的画面合成为一帧，送去显示 |
| `package` | 管理所有应用的安装、卸载、权限 |
| `power` | 电源管理——电池、充电、省电模式 |
| `input` | 处理触控板输入事件 |
| `sensorservice` | 管理所有 23 个传感器的数据分发 |
| `media.camera` | 相机服务——控制摄像头硬拍摄/预览 |
| `audio` | 音频服务——扬声器、麦克风、音量控制 |
| `connectivity` | 网络连接管理——WiFi、蓝牙 |

### 9.3 安全相关服务

```
┌─────────────────────────────────────────────┐
│             安全体系                          │
│                                             │
│  Verified Boot（验证启动）                    │
│  ├── 所有分区用 SHA-256 哈希校验              │
│  ├── 启动状态：orange（开发者解锁）            │
│  └── bootloader 锁状态：unlocked（已解锁）    │
│                                             │
│  Keystore2（密钥存储）                        │
│  ├── 硬件级密钥保护（KeyMint）                │
│  └── 安全元素 TrustZone（QSEE）              │
│                                             │
│  SELinux（强制访问控制）                      │
│  └── enforcing 模式（严格限制应用权限）        │
│                                             │
│  文件加密                                    │
│  └── FBE（基于文件的加密，ro.crypto.type=file）│
└─────────────────────────────────────────────┘
```

---

## 第十章：进程运行图——谁在吃资源

从进程列表可以看到，开机后有约 **50+ 个用户空间进程**在运行：

### 10.1 内存占用排行（Top 10）

```
进程                              内存(RSS)    说明
─────────────────────────────────────────────────────────
system_server (PID 1288)          ~304MB      Android 系统大脑
com.ffalconxr.mercury.launcher    ~234MB      主桌面
com.android.systemui              ~134MB      状态栏/通知
com.rayneo.gallery                ~133MB      图库
com.leiniao.camera                ~132MB      相机
com.eg.android.AlipayGGlasses     ~137MB      支付宝
com.rayneo.airuntime              ~110MB      AI 引擎
com.rayneo.media                  ~106MB      媒体播放
com.rayneo.aispeech               ~98MB       语音助手
com.rayneo.record                 ~92MB       录音
```

### 10.2 进程关系（父子进程树）

```
init (PID=1)
├── logd (日志守护进程)
├── lmkd (低内存杀手)
├── servicemanager (系统服务管理)
├── surfaceflinger (显示合成)
├── audioserver (音频)
├── cameraserver (相机)
├── adbd (ADB调试桥)
├── thermal-engine-v2 (温控引擎)
│
└── zygote64 (PID=692, Android 应用孵化器)
    ├── system_server (PID=1288, 系统核心)
    ├── com.ffalconxr.mercury.launcher (主桌面)
    │   └── logcat (日志捕获子进程)
    ├── com.android.systemui (系统UI)
    ├── com.rayneo.xr.runtime (XR运行时)
    ├── com.rayneo.airuntime (AI引擎)
    │   └── top (性能监控子进程)
    ├── com.rayneo.aispeech (语音)
    │   └── :interactor (交互子进程)
    │   └── :rhotword (唤醒词检测)
    ├── com.leiniao.camera (相机)
    ├── com.rayneo.media (媒体)
    ├── com.rayneo.gallery (图库)
    ├── com.rayneo.notes (笔记)
    ├── com.rayneo.phone (电话)
    ├── com.rayneo.record (录音)
    ├── com.rayneo.live.ai (LiveAI)
    ├── com.ffalcon.appcontainer:floatball (应用容器悬浮球)
    ├── com.eg.android.AlipayGGlasses (支付宝)
    ├── com.sogou.inputmethod.iot (搜狗输入法)
    ├── com.android.bluetooth (蓝牙)
    ├── com.android.webview (网页渲染)
    └── ... 其他应用
```

---

## 第十一章：音频系统——声音怎么采集和播放

### 11.1 硬件

- **扬声器**：2 个立体声扬声器，驱动芯片是 `aw883xx`（AWINIC 艾为电子）
- **麦克风**：3 个，两侧镜腿各一个，前方一个
- **功能**：支持定向拾音、反向消音（减少扬声器声音泄漏到麦克风）

### 11.2 音频链路

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│  3个麦克风 │────→│ ADSP（音频DSP）│────→│ 语音识别  │
└──────────┘     │ 高通音频处理器  │     │ AI降噪    │
                 └──────┬───────┘     └──────────┘
                        │
                 ┌──────↓───────┐
                 │ AudioServer   │
                 │ Android 音频服务│
                 └──────┬───────┘
                        │
                 ┌──────↓───────┐     ┌──────────┐
                 │ Audio HAL     │────→│ 2个扬声器  │
                 │ 硬件抽象层     │     │ aw883xx   │
                 └──────────────┘     └──────────┘
```

---

## 第十二章：电源和温控——怎么保持不烫不断电

### 12.1 电池

```
电池状态（采集时刻）：
  电池技术：锂离子（Li-ion）
  电量：100%
  电压：4.392V
  温度：16.5°C（165 ÷ 10）
  充电方式：USB 供电中
  最大充电电流：2400mA
  最大充电电压：5000mV (5V)
  电池容量：245mAh
```

### 12.2 温控系统

```
thermal-engine-v2 (温控引擎)
    │
    ├── 监控 CPU 温度
    ├── 监控 GPU 温度
    ├── 监控电池温度
    ├── 监控皮肤温度（靠近脸的区域）
    │
    ├── 温度正常 → 全速运行
    ├── 温度偏高 → 降低 CPU/GPU 频率
    ├── 温度过高 → 限制亮度 + 限制帧率
    └── 温度危险 → 强制关机保护
```

**为什么这么重要？**

一般手机发热你可以放下，但 AR 眼镜是戴在脸上的。如果温度过高：
- 太烫会伤害皮肤
- 用户会不舒服直接摘掉

所以温控是 AR 眼镜最关键的系统之一。系统属性中有大量显示相关的功耗优化开关，就是为了省电降温。

---

## 第十三章：网络连接——WiFi 和蓝牙

### 13.1 WiFi

```
类型：WiFi 6 (802.11 a/b/g/n/ac)
频段：2.4GHz + 5GHz 双频
驱动：CNSS（Qualcomm Connected Networking Sub-System）
芯片：高通集成 WiFi 模块
MAC地址：BC:94:24:9A:F5:14
```

### 13.2 蓝牙

```
版本：蓝牙 5.2
库：libbluetooth_qti.so（高通定制蓝牙库）
MAC地址：BC:94:24:04:9D:21
用途：
  - 连接手机（接收通知、GPS 数据推流）
  - 连接手表/戒指配件
  - 蓝牙耳机音频
  - BLE低功耗设备
```

### 13.3 注意：没有 SIM 卡

```
ro.radio.noril = yes  ← 没有无线电基带
```

X3 Pro **没有蜂窝网络**（不能插 SIM 卡打电话/上 4G）。所有网络都通过 WiFi 或蓝牙连手机实现。

---

## 第十四章：安全系统——层层保护

```
┌────────────────────────────────────────────────────┐
│ Layer 1: Verified Boot（验证启动）                   │
│ - 每次开机验证系统完整性                              │
│ - 用 SHA-256 校验每个分区                            │
│ - 当前状态：bootloader 已解锁（开发用）               │
│ - 正常用户状态应该是 locked                          │
├────────────────────────────────────────────────────┤
│ Layer 2: SELinux（强制访问控制）                      │
│ - 每个进程只能访问被允许的文件/设备                    │
│ - enforcing 模式（违规操作直接拒绝）                  │
├────────────────────────────────────────────────────┤
│ Layer 3: 文件加密（FBE）                             │
│ - 用户数据分区加密                                   │
│ - 每个用户有独立密钥                                 │
├────────────────────────────────────────────────────┤
│ Layer 4: TrustZone (QSEE)                          │
│ - 高通安全执行环境                                   │
│ - 密钥存储在硬件安全芯片中                            │
│ - KeyMint 硬件密钥管理                               │
├────────────────────────────────────────────────────┤
│ Layer 5: 应用沙箱                                   │
│ - 每个应用有独立的 Linux 用户 ID                     │
│ - 应用之间互相隔离                                   │
│ - 权限系统控制访问（相机/麦克风/存储等）               │
└────────────────────────────────────────────────────┘
```

---

## 第十五章：特殊系统设计——X3 Pro 独有的

### 15.1 XR 调度器

```
服务：vendor.xrschedulerservice
包名：com.rayneo.xr.IXRSchedulingPolicyService
```

这个是雷鸟自己写的**XR 专用进程调度器**。它的作用：
- AR 画面渲染必须保持 60fps 流畅
- 这个调度器会给 XR Runtime 进程更高的 CPU 优先级
- 确保即使后台有其他应用在跑，AR 画面也不会卡顿

### 15.2 Mercury 平台

从代码中的命名可以看出，X3 Pro 的内部代号是 **"MercuryLiteXR"**（水星精简版 XR）：

```
ro.build.product = MercuryLiteXR
ro.product.device = MercuryLiteXR
```

"Mercury" 是雷鸟 X 系列的平台代号，贯穿整个系统：
- Launcher 叫 `mercury.launcher`
- 相机叫 `MercuryCamera`
- 显示合成器配置叫 `mercurylitexr`

### 15.3 DSP 处理器

X3 Pro 用了高通的多个 DSP（数字信号处理器）来卸载 CPU 负担：

```
ADSP (Audio DSP)：音频处理——降噪、回声消除、语音唤醒
CDSP (Compute DSP)：计算加速——AI 推理、图像处理
```

这些 DSP 独立于 CPU 运行，可以在 CPU 休眠时仍然工作（比如持续监听唤醒词 "你好雷鸟"）。

### 15.4 共享库 `xros-framework`

```
library:xros-framework
```

这是雷鸟在 Android 框架层注入的自定义库，叫 **XROS Framework**。
它是 RayNeo AI OS 2.0 的核心框架，在 Android 原生框架之上增加了 AR/XR 专用的 API。

---

## 第十六章：快速参考卡片

### ADB 常用命令速查

```bash
# 查看实时日志
adb logcat

# 只看雷鸟相关日志
adb logcat -s Mercury

# 截图
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# 录屏
adb shell screenrecord /sdcard/video.mp4

# 安装应用
adb install app.apk

# 查看当前前台 Activity
adb shell dumpsys activity activities | grep mResumedActivity

# 查看CPU使用率
adb shell top -n 1

# 查看电池状态
adb shell dumpsys battery

# 查看传感器实时数据
adb shell dumpsys sensorservice

# 重启眼镜
adb reboot
```

### 系统关键路径速查

| 路径 | 内容 |
|------|------|
| `/system_ext/priv-app/RayNeoLauncher/` | 主桌面 APK |
| `/system_ext/priv-app/RayNeoRuntime/` | XR 运行时 APK |
| `/system_ext/priv-app/RayNeoAIRuntime/` | AI 引擎 APK |
| `/system_ext/priv-app/RayNeoAISpeech/` | AI 语音 APK |
| `/system_ext/priv-app/MercuryCamera/` | 相机 APK |
| `/vendor/etc/` | 硬件配置文件 |
| `/system/framework/` | Android 框架 JAR 包 |
| `/system/lib64/` | 系统共享库（800+ 个 .so 文件）|

---

> 📖 本文档基于 2026年3月19日 通过 ADB 从设备 BC94249AF514442 采集的实际系统数据编写。
> 每一条信息都来自真实的系统属性、进程列表、传感器数据和目录结构。
