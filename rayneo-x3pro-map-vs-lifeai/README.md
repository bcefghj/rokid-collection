# 雷鸟X3 Pro「地图」与「智慧生活」深度对比解析

> 两个看似相同的 AR 导航应用，到底有什么区别？

## 项目简介

雷鸟X3 Pro 上同时存在两个导航相关应用：
- **地图**（`com.ffalcon.navigation`）— 系统预装
- **智慧生活**（`com.rayneo.lifeai`）— 后装更新

本项目通过 ADB 接口采集两个应用的完整信息，对比分析它们的技术架构、功能差异和演进关系。

## 核心结论

**这两个应用是同一个产品的不同版本。** 智慧生活是地图的升级版替代品。

| 对比项 | 地图（旧版） | 智慧生活（新版） |
|--------|-------------|-----------------|
| 包名 | `com.ffalcon.navigation` | `com.rayneo.lifeai` |
| 版本 | V2026.01.29.14 | V2026.03.10.15 |
| APK大小 | **277MB** | **120MB** |
| 安装位置 | `/system_ext/priv-app/`（系统预装） | `/data/app/`（后装更新） |
| 安装时间 | 出厂预装（2009-01-01） | 2026-03-18 由 Launcher 安装 |
| 高德导航SDK | **v9.8.3**（32MB） | **v10.1.302**（60MB） |
| HERE地图SDK | **有**（72MB `libheresdk.so`） | **无** |
| 高德Agent SDK | **无** | **有**（2MB `libAMapSDK_AGENT`） |
| 蓝牙权限 | 无 | BLUETOOTH_CONNECT |
| 配置切换 | 无 | CHANGE_CONFIGURATION |
| 高德导航权限 | 无 | `autonavi.minimap.permission.NAVIGATION_SERVICE` |
| 镜腿按键 | 无 | `mercury.support_key` |
| HERE用户协议 | 有 ConsentActivity | 无 |
| 海外不可用提示 | 有 | 无 |
| 扫街榜POI缓存 | 无 | **499个离线POI XML** |
| 独有UI | connectionissueview, overseasunavailableregiontipview | rayneolife_assets |

## 文档目录

- **[完整技术对比分析](docs/地图与智慧生活完整对比分析.md)** — 核心文档，详细对比两个应用的方方面面
- `data/地图_navigation/` — 地图应用原始采集数据
- `data/智慧生活_lifeai/` — 智慧生活应用原始采集数据

## 关键发现

### 1. HERE地图被砍了
地图（旧版）包含 72MB 的 `libheresdk.so`（HERE Maps SDK），还有 HERE 用户协议界面、HERE 暗色样式定义等完整的海外地图支持。智慧生活（新版）完全去掉了 HERE，**只保留高德地图**。这让 APK 从 277MB 缩小到 120MB。

### 2. 高德SDK大幅升级
高德导航SDK从 v9.8.3 升级到 v10.1.302，版本跨度巨大。新增了独立的 `libAMapSDK_AGENT` 库（2MB），说明新版走的是高德"智能体"（Agent）路线——对应 AWE 2026 发布的 LBS Agent 能力。

### 3. 新增扫街榜离线数据
智慧生活内置了 499 个 `eyrie/cloudres/*.xml` 文件，这些是高德 POI（兴趣点）的离线缓存数据，用于"扫街榜"功能——无需联网即可显示附近商户评分。

### 4. 从国际版变成中国专版
地图（旧版）是国际化版本（有 HERE + 海外不可用提示），智慧生活（新版）去掉了所有国际化组件，变成**纯中国市场版本**。

### 5. 代码几乎相同
两个应用的 C# DLL 列表（ScriptingAssemblies.json）完全一致（131个DLL），Unity引擎版本、OpenXR配置、boot.config 都一模一样。**说明是同一个代码库构建的不同配置。**

## 分析方法

```bash
adb shell dumpsys package com.ffalcon.navigation   # 应用详细信息
adb shell dumpsys package com.rayneo.lifeai
adb pull <apk_path>                                  # 拉取APK
unzip -l *.apk                                       # 分析APK结构
# 解压并对比 language_zh.txt / ScriptingAssemblies.json / AndroidManifest 等
```

## 免责声明

⚠️ **本项目仅供学习和研究使用。**

- 雷鸟X3 Pro（RayNeo X3 Pro）是**雷鸟创新技术有限公司**（TCL集团旗下）的产品
- 高德地图SDK归**高德软件有限公司**所有
- HERE地图服务归 **HERE Global B.V.** 所有
- Unity引擎归 **Unity Technologies** 所有
- 所有商标、专利、软件著作权归相关权利人所有
- 本项目**不包含任何APK二进制文件、反编译源码或受保护的知识产权内容**
- 所有数据均通过 Android 标准 ADB 调试接口获取
- **如有侵权，请联系我立即删除**

## License

[CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/) — 仅限非商业用途的学习和研究。
