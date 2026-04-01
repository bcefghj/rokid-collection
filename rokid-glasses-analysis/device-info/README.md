# 设备信息

本目录包含通过 ADB 从 Rokid AI 眼镜 (RG-glasses) 提取的设备信息转储文件。

## 信息来源

所有信息通过 `adb shell` 命令合法提取，不涉及 root 权限。

## 说明

由于设备信息文件较大且包含大量原始数据，这里仅提供概述。如需完整数据，请参考主文档中的相关章节。

### 可提取的信息类别

| 类别 | 命令示例 | 内容说明 |
|------|---------|---------|
| 系统属性 | `adb shell getprop` | 1200+ 条系统属性 |
| 安装包列表 | `adb shell pm list packages` | 94 个已安装包 |
| 运行中服务 | `adb shell dumpsys activity services` | 运行中的服务 |
| 传感器信息 | `adb shell dumpsys sensorservice` | 16 个硬件传感器 |
| 摄像头信息 | `adb shell dumpsys media.camera` | Sony IMX681 配置 |
| 显示信息 | `adb shell dumpsys display` | 480×640 竖屏 |
| 音频信息 | `adb shell dumpsys audio` | 8通道麦克风+WSA功放 |
| 蓝牙信息 | `adb shell dumpsys bluetooth_manager` | BT 5.2+ 配置 |
| WiFi 信息 | `adb shell dumpsys wifi` | QCA WLAN |
| 输入设备 | `adb shell dumpsys input` | 触控板+按键 |
| 电池信息 | `adb shell dumpsys battery` | 锂离子 4.5V |
| CPU 信息 | `adb shell cat /proc/cpuinfo` | 8核高通QCS6490 |
| 内存信息 | `adb shell cat /proc/meminfo` | 1.78GB |
| 内核版本 | `adb shell uname -a` | Linux 5.10.209 |
