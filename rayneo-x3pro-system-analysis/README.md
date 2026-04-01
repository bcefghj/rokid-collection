# RayNeo X3 Pro 系统深度解析

> **雷鸟X3 Pro AR智能眼镜**的系统级逆向分析与学习笔记

## 项目简介

本项目通过 ADB（Android Debug Bridge）对雷鸟X3 Pro AR智能眼镜进行了系统级的信息采集与分析，旨在学习和理解这款AR设备的软硬件架构。

**所有数据均通过ADB公开接口获取，不涉及Root、破解或任何非法操作。**

## 设备概览

| 项目 | 详情 |
|------|------|
| 设备 | 雷鸟 RayNeo X3 Pro |
| 系统 | Android 12（API 31） |
| 芯片 | 高通骁龙 AR1 Gen1（4核 ARM64） |
| 内核 | Linux 5.10.226 |
| 内存 | 3.7GB RAM |
| 存储 | 32GB eMMC |
| 显示 | 双目 MicroLED 640×480 @60Hz |
| 传感器 | 加速度计、陀螺仪（LSM6DSR）、磁力计（MMC56X3X）、环境光 |
| 连接 | Wi-Fi 6 + 蓝牙 5.2 |

## 文档目录

### 核心文档（推荐阅读顺序）

1. **[系统完整解读（小白版）](docs/系统分析/雷鸟X3Pro系统完整解读_小白版.md)** — 用通俗语言解释整个系统架构
2. **[系统总览](docs/系统分析/README_系统总览.md)** — 技术总览与数据索引
3. **[应用深度解析](docs/应用解析/雷鸟X3Pro应用深度解析.md)** — 全部应用的组件、权限、交互分析
4. **[智慧生活/地图应用解析](docs/应用解析/智慧生活_地图/智慧生活_地图应用深度解析.md)** — AR导航应用的详细逆向

### 原始数据

```
docs/
├── 系统分析/
│   ├── 系统属性/          # getprop 全量属性（版本、硬件、网络、运行时）
│   ├── 目录结构/          # /system /vendor /product /system_ext 分区文件树
│   ├── 硬件信息/          # CPU、GPU、内存、传感器、摄像头、电池等
│   ├── 系统服务/          # ActivityManager、WindowManager、Audio、Power等
│   └── 已安装应用/        # 应用列表、包名、路径（不含APK二进制）
├── 应用解析/
│   ├── 雷鸟核心应用/      # 17个雷鸟自研应用的 dumpsys 数据
│   ├── 第三方应用/        # 21个第三方/系统应用数据
│   ├── Manifest解析/      # 关键应用的完整组件注册信息
│   ├── 应用交互关系/      # Activity栈、Service绑定、ContentProvider
│   └── 智慧生活_地图/     # AR导航应用专项分析（Unity/高德/HERE）
```

## 关键发现

- **Mercury Launcher** 是整个系统的核心，拥有 60+ 个系统级权限
- **XR Runtime** 的 MonitorService 同时被 4 个应用绑定，是所有AR数据的枢纽
- **AI Speech** 采用三进程架构（主服务 + 交互 + 唤醒词检测），是最复杂的单一应用
- **智慧生活（地图）** 实际是 Unity + 高德地图 + HERE地图 构建的AR导航应用
- 系统使用了 A/B 双分区方案，支持无缝 OTA 更新
- 支付宝眼镜版使用 IMU 传感器进行活体检测

## 分析方法

所有数据通过以下 ADB 命令获取：

```bash
adb shell getprop                      # 系统属性
adb shell pm list packages -f          # 应用列表
adb shell dumpsys package <pkg>        # 应用详细信息
adb shell dumpsys activity/window/...  # 系统服务状态
adb shell cat /proc/cpuinfo            # 硬件信息
adb shell ls -laR /system/             # 文件系统结构
```

## 免责声明

⚠️ **本项目仅供学习和研究使用。**

- 雷鸟X3 Pro（RayNeo X3 Pro）是**雷鸟创新（TCL集团旗下）**的产品，所有商标、专利、软件著作权归**雷鸟创新技术有限公司**及相关权利人所有
- 本项目中涉及的高德地图SDK归**高德软件有限公司**所有
- 本项目中涉及的 HERE 地图服务归 **HERE Global B.V.** 所有
- 本项目中涉及的 Unity 引擎归 **Unity Technologies** 所有
- 本项目中涉及的高通骁龙平台技术归 **Qualcomm Technologies, Inc.** 所有
- 本项目**不包含任何APK二进制文件、反编译源码或受保护的知识产权内容**
- 所有数据均通过 Android 标准 ADB 调试接口获取，属于设备公开信息
- **如有侵权，请联系我立即删除**

## License

本项目文档内容采用 [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/) 许可协议。

仅限非商业用途的学习和研究。
