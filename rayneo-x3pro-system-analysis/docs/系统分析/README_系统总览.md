# 雷鸟 X3 Pro 系统分析总览

> 数据采集时间：2026年3月19日  
> 设备序列号：BC94249AF514442  
> ADB 连接方式：USB-C 数据线

---

## 一、设备基本信息

| 项目 | 值 |
|------|-----|
| 设备型号 | ARGF20（雷鸟 X3 Pro）|
| 品牌 | RayNeo |
| 系统版本 | SKQ1.250204.001 release-keys |
| Android 版本 | 12 |
| SDK 版本 | 32 |
| 内核版本 | Linux 5.10.226-perf（2025年11月3日编译）|
| 架构 | aarch64（ARM64）|
| 内存 | 约 3.7GB RAM（物理 4GB）|
| 存储 | 32GB（用户数据分区约22GB，已用6.8GB）|

---

## 二、系统分区结构

| 分区 | 挂载点 | 大小 | 说明 |
|------|--------|------|------|
| system | / | 931MB | 核心系统 |
| system_ext | /system_ext | 1.7GB | 系统扩展分区 |
| product | /product | 418MB | 产品定制层 |
| vendor | /vendor | 619MB | 硬件厂商驱动 |
| vendor_dlkm | /vendor_dlkm | 44MB | 动态内核模块 |
| odm | /odm | 0.9MB | 设备定制层 |
| data | /data | 22GB | 用户数据 |

---

## 三、已安装应用统计

| 类型 | 数量 |
|------|------|
| 全部应用 | 132 个 |
| 系统应用 | 116 个 |
| 第三方应用 | 16 个 |

### 雷鸟自家核心应用（已备份APK）

| 包名 | 功能说明 |
|------|---------|
| com.ffalconxr.mercury.launcher | 主桌面 Launcher |
| com.ffalconxr.mercury.launcher.mini.test | 精简版 Launcher（测试版）|
| com.rayneo.xr.runtime | XR 运行时核心 |
| com.rayneo.airuntime | AI 运行时引擎 |
| com.rayneo.aispeech | AI 语音服务 |
| com.rayneo.live.ai | AI 直播/实时处理 |
| com.rayneo.gallery | 图库 |
| com.rayneo.camera / com.leiniao.camera | 相机 |
| com.rayneo.media | 媒体播放 |
| com.rayneo.notes | 笔记 |
| com.rayneo.phone | 电话 |
| com.rayneo.record | 录音/录屏 |
| com.ffalcon.appcontainer | 应用容器（Android 虚拟机）|
| com.ffalcon.calibration | 显示校准工具 |
| com.ffalcon.navigation | 导航 |
| com.ffalcon.translate | 翻译 |
| com.ffalcon.wordprompt | 提词器 |
| com.android.hotwordenrollment.rayneo | 唤醒词注册（语音助手）|

---

## 四、硬件架构

- **芯片**：高通骁龙 AR1 Gen1（Qualcomm AR1）
- **CPU 架构**：ARM64 / aarch64，Cortex-A55/A78 核心
- **内核**：Linux 5.10.226（perf 定制版，2025-11-03）
- **显示**：双屏 MicroLED，分辨率 640×480 / 屏，FOV 30°
- **摄像头**：1200万像素 RGB 主摄 + VGA 空间摄像头

---

## 五、文件夹内容说明

```
系统分析/
├── README_系统总览.md          ← 本文件
├── 系统属性/
│   ├── 所有系统属性.txt        ← getprop 全部644条属性
│   ├── 版本信息.txt            ← 系统版本相关属性
│   ├── 硬件属性.txt            ← 硬件相关属性
│   ├── 运行时配置.txt          ← Dalvik/ART 运行时配置
│   └── 网络属性.txt            ← WiFi/蓝牙相关属性
├── 目录结构/
│   ├── system分区.txt          ← /system 目录树
│   ├── vendor分区.txt          ← /vendor 目录树
│   ├── product分区.txt         ← /product 目录树
│   └── system_ext分区.txt      ← /system_ext 目录树
├── 已安装应用/
│   ├── 所有应用包名和路径.txt   ← 132个应用完整列表
│   ├── 系统应用列表.txt         ← 116个系统应用
│   ├── 第三方应用列表.txt       ← 16个第三方应用
│   ├── 已禁用应用列表.txt       ← 被禁用的应用
│   ├── 所有应用详细信息.txt     ← dumpsys package 完整信息
│   ├── APK备份/                 ← 第三方APK文件
│   └── 系统APK备份/             ← 18个雷鸟核心系统APK
├── 系统服务/
│   ├── 运行中的进程.txt         ← ps -A 所有进程
│   ├── 系统服务列表.txt         ← service list
│   ├── Activity管理器.txt       ← dumpsys activity
│   ├── 窗口管理器.txt           ← dumpsys window
│   ├── 电源管理.txt             ← dumpsys power
│   ├── 音频服务.txt             ← dumpsys audio
│   └── 显示服务.txt             ← dumpsys display
└── 硬件信息/
    ├── CPU信息.txt              ← /proc/cpuinfo
    ├── 内存信息.txt             ← /proc/meminfo
    ├── 内核版本.txt             ← uname -a
    ├── 传感器服务.txt           ← dumpsys sensorservice
    ├── 摄像头信息.txt           ← dumpsys media.camera
    ├── 电池信息.txt             ← dumpsys battery
    ├── WiFi信息.txt             ← dumpsys wifi
    ├── 蓝牙信息.txt             ← dumpsys bluetooth_manager
    ├── 存储空间.txt             ← df -h
    ├── 设备节点列表.txt         ← /dev/ 设备树
    ├── 系统特性列表.txt         ← pm list features
    ├── 系统共享库列表.txt       ← pm list libraries
    ├── SurfaceFlinger渲染引擎.txt ← 渲染管线信息
    └── AR_XR专项信息.txt        ← XR/AI Runtime 信息
```

---

## 六、研究建议

### 研究系统架构
1. 先看 `系统属性/所有系统属性.txt` 了解整体配置
2. 再看 `目录结构/` 理解各分区职责划分
3. 重点关注 `/vendor` 分区（高通AR1驱动在这里）

### 研究雷鸟定制层
1. 用 [jadx](https://github.com/skylot/jadx) 反编译 `系统APK备份/` 里的 APK
2. 重点研究：
   - `com.ffalconxr.mercury.launcher.apk` — 主桌面系统
   - `com.rayneo.xr.runtime.apk` — XR 运行时
   - `com.rayneo.airuntime.apk` — AI 引擎
   - `com.ffalcon.appcontainer.apk` — Android 虚拟机容器

### 研究系统服务
1. 看 `系统服务/系统服务列表.txt` 了解有哪些系统服务
2. 看 `系统服务/运行中的进程.txt` 了解各进程关系
3. 看 `硬件信息/传感器服务.txt` 了解 IMU/陀螺仪等传感器

---

## 七、无法获取的内容（需root）

- `/system`、`/vendor` 分区的完整文件内容（只能看目录，无法 pull 文件）
- `/data/data/` 各应用私有数据
- 内核驱动源码（需厂商开放）
- SELinux 策略文件完整内容
