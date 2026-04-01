# 通信协议详解

> 眼镜和手机之间是怎么"对话"的？

---

## 概述

Rokid AI 眼镜和手机之间的所有通信都通过 **CXRService** 这个通信桥梁来完成。

CXR 的全称可能是 "Cloud XR"（云端扩展现实），但在实际使用中，它更像是一个**全能通信中间件**。

---

## CXRService — 通信中枢

### 基本信息

| 项目 | 详情 |
|------|------|
| 包名 | `com.rokid.cxrservice` |
| 版本 | 1.109 |
| 位置 | `/system/app/CXRService/` |
| 权限 | 系统级 (UID: system) |
| 运行方式 | persistent（常驻不退出） |

### 三种连接方式

| 方式 | clientType | 适用场景 | 速度 |
|------|-----------|---------|------|
| **BT RFCOMM Socket** | 1 | Android 手机 | 较快 |
| **BLE GATT** | 2 | iPhone（无 MFi） | 较慢 |
| **MFI iAP2 Socket** | 3 | iPhone（有 MFi 认证） | 较快 |

---

## 蓝牙连接流程

### BLE 广播

眼镜会不断发送 BLE（低功耗蓝牙）广播，让手机能发现它：

广播内容包括：
- Compound Service UUID（服务标识）
- 设备序列号（`ro.serialno`）

### GATT 特征

当手机通过 BLE 连接到眼镜后，可以读写以下特征值：

| 特征 UUID | 属性 | 用途 |
|-----------|------|------|
| `IOS_CONNECTION_CHAR` | Read | iOS 读取连接信息 |
| `RFCOMM_CONNECTION_CHAR` | Read | Android 读取 RFCOMM UUID 和 MAC |
| `IOS_WRITE_CHAR` | Write | iOS 向眼镜写入数据 |
| `IOS_NOTIFY_CHAR` | Notify | 眼镜向 iOS 推送数据 |
| `IOS_MFI_PAIRING_CHAR` | Write | MFI 配对请求 |
| `IOS_MFI_NOTIFY_CHAR` | Notify | MFI 通知 |

### Android 手机连接流程

```
步骤 1: 打开 Rokid App，扫描 BLE 蓝牙广播
  ↓
步骤 2: 发现眼镜 → 建立 BLE GATT 连接
  ↓
步骤 3: 读取 RFCOMM_CONNECTION_CHAR
  │      获取 serviceRecord UUID + MAC 地址
  ↓
步骤 4: 使用获取的 UUID 建立 RFCOMM Socket 连接
  │      (经典蓝牙，传输速度更快)
  ↓
步骤 5: 发送认证握手 (Caps 协议，版本 1.4)
  ↓
步骤 6: 通信就绪 ✓
```

为什么先用 BLE 再切换到 RFCOMM？
- BLE 功耗低，适合发现和初始连接
- RFCOMM (SPP) 传输速度更快，适合实际数据传输

### iPhone 连接流程

```
步骤 1: BLE 扫描 → 发现眼镜
  ↓
步骤 2: 写入 MFI_PAIRING_CHAR → 触发经典蓝牙配对
  ↓
步骤 3: 通过 Apple External Accessory 框架建立 iAP2 Socket
  ↓
步骤 4: 使用协议:
  │      com.rokid.aiglasses
  │      com.rokid.bolonglasses
  ↓
步骤 5: 通信就绪 ✓
```

---

## Caps 序列化协议

### 什么是 Caps？

Caps 是 Rokid 自研的**二进制序列化格式**，用于在蓝牙通道上高效传输数据。

类似于 Google 的 Protocol Buffers，但更轻量级。

### 技术细节

| 项目 | 详情 |
|------|------|
| 实现方式 | JNI native 代码 |
| 序列化 | `serialize()` → 二进制数据 |
| 反序列化 | `parse()` → 结构化数据 |
| 读写 | `write()` / `read()` |

### 支持的数据类型

| 类型 | 说明 |
|------|------|
| Int32 | 32 位整数 |
| UInt32 | 无符号 32 位整数 |
| Int64 | 64 位整数 |
| UInt64 | 无符号 64 位整数 |
| Float | 单精度浮点数 |
| Double | 双精度浮点数 |
| String | 字符串 |
| Binary | 二进制数据 |
| Object | 嵌套对象 |

---

## CXR 命令体系

### 消息格式

所有命令都是 JSON 格式：

```json
{
  "cmd": "命令分类",
  "key": "具体操作",
  "param": "参数(JSON字符串)"
}
```

### 示例

```json
// 让眼镜播放 TTS
{
  "cmd": "Sys",
  "key": "Tts_SendPlayTts",
  "param": "{\"content\":\"你好，我是若琪\"}"
}

// 同步设备电量
{
  "cmd": "Dev",
  "key": "Dev_Battery",
  "param": "{\"level\":85}"
}

// 推送翻译结果
{
  "cmd": "Trans",
  "key": "Trans_ResponseChangeSceneId",
  "param": "{\"text\":\"Hello World\"}"
}
```

### 完整的 21 个命令分类

#### 系统类

| 前缀 | 功能 | 常用 Key |
|------|------|---------|
| `Sys` | 系统控制 | 音频场景切换、TTS 播放、系统设置 |
| `Dev` | 设备信息 | 电量、亮度、音量、屏幕状态 |
| `Settings` | 设置同步 | 设置项同步 |
| `Ota` | 固件更新 | 更新通知、下载进度 |

#### 通信类

| 前缀 | 功能 | 常用 Key |
|------|------|---------|
| `Wifi` | WiFi 控制 | WiFi 状态同步 |
| `Proxy` | 代理通信 | 通用代理消息 |

#### 媒体类

| 前缀 | 功能 | 常用 Key |
|------|------|---------|
| `Med` | 媒体管理 | 打开/关闭相机、缩略图同步、未同步计数 |
| `Music` | 音乐控制 | 播放/暂停/切歌、歌词推送 |
| `Broadcast` | 直播控制 | 直播推流开始/停止 |
| `ARTC` | 实时通信 | 音视频帧传输、用户说话状态 |

#### AI 类

| 前缀 | 功能 | 常用 Key |
|------|------|---------|
| `Ai` | AI 助手 | AI 按键、拍照、视频帧推送 |
| `Trans` | 翻译(新) | 翻译场景切换、翻译数据 |
| `Tra` | 翻译(旧) | 旧版翻译兼容 |
| `Jsai` | JS AI | JavaScript AI 引擎通信 |

#### 功能类

| 前缀 | 功能 | 常用 Key |
|------|------|---------|
| `Nav` | 导航 | 导航指令、路线数据 |
| `Pay` | 支付 | 支付代理消息 |
| `Ntf` | 通知 | 手机通知转发到眼镜显示 |
| `Schedule` | 日程 | 日程同步/添加/删除 |
| `Memo` | 备忘 | 备忘录同步 |
| `Journey` | 行程 | 火车票/航班管理 |
| `Order` | 点餐 | 餐饮点单 |
| `Custom_View` | 自定义视图 | 第三方自定义界面 |

---

## WiFi P2P 高速传输

### 为什么需要 WiFi P2P？

蓝牙传输速度有限（一般 2-3 Mbps），传输大量照片/视频时太慢。WiFi Direct 可以达到几十 Mbps。

### 实现

通过 `WifiP2pServerManager` 建立 WiFi Direct 连接：

```
眼镜创建 WiFi P2P 热点
  ↓
手机连接热点
  ↓
高速传输照片/视频
  ↓
传输完成 → 断开 WiFi P2P
```

iOS App 的权限说明：*"发现和连接眼镜热点来同步媒体文件"*

---

## 内置 Web 服务器

### 基本信息

| 项目 | 详情 |
|------|------|
| 框架 | AndServer |
| 端口 | 8848 |
| 发现方式 | NSD (mDNS) 局域网发现 |
| 开关 | `Settings.Global["rokid_er_remote_transmission"]` |

### 功能

在同一局域网内，可以通过浏览器访问 `http://眼镜IP:8848` 来传输文件。

### 传输目录

| 路径 | 内容 |
|------|------|
| `/sdcard/Download/` | 下载文件 |
| `/sdcard/SpatialPhotos/` | 空间照片 |

### 支持的文件格式

image, video, 3d_video, audio, document, apk, subtitle, playlist

---

## 云端 API

| 环境 | 域名 |
|------|------|
| 生产环境 | `https://rokid-content-pro.rokid.com` |
| 测试环境 | `https://rokid-content-test.rokid.com` |
| 开发环境 | `https://rokid-content-dev.rokid.com` |
| OTA 服务器 | `https://ota.rokid.com/v1/extended/ota/check` |

---

## 支付代理通信

支付功能使用特殊的代理通信机制：

```
手机 App ←→ 蓝牙 GATT (Pay.* 命令) ←→ PaymentProxyManager ←→ 各 PayCenter
```

消息格式：

```json
{
  "type": "PROXY_TYPE_PAY_DEVICE",
  "requestId": "xxx",
  "reqMessage": {
    "param0": "...",
    "param1": "..."
  }
}
```
