# 交互系统完整解析

> 没有触摸屏的眼镜，是怎么让你操控的？

---

## 概述

Rokid AI 眼镜**没有触摸屏**，所有操作通过以下方式完成：

1. **镜腿触控板** — 滑动、点击
2. **物理按键** — 功能键、音量键
3. **语音命令** — 19 个离线命令
4. **头部手势** — 点头、摇头
5. **佩戴检测** — 自动感知戴上/摘下

---

## 触控板系统

### 硬件

| 项目 | 详情 |
|------|------|
| 芯片 | Cypress PSOC |
| 总线 | I2C |
| 设备名 | `ROKID,PSOC-TP-R` |
| 位置 | 镜腿侧面 |

### 手势映射

触控板上的手势操作会被 PSOC 固件转换成 Android 标准按键事件：

| 手势操作 | 系统按键码 | 在主界面的效果 | 在应用中的效果 |
|---------|-----------|--------------|--------------|
| 单击 | `KEYCODE_ENTER (66)` | 展开/折叠卡片 | 确认/选择 |
| 双击 | `key 202` (自定义) | 自定义动作 | 自定义动作 |
| 向前滑动 | `key 183` (自定义) | — | 自定义 |
| 向后滑动 | `key 184` (自定义) | — | 自定义 |
| 向上滑动 | `KEYCODE_DPAD_UP (19)` | 切到消息列表页 | 焦点上移 |
| 向下滑动 | `KEYCODE_DPAD_DOWN (20)` | 切到应用列表页 | 焦点下移 |
| 向左滑动 | `KEYCODE_DPAD_LEFT (21)` | — | 返回/左移 |
| 向右滑动 | `KEYCODE_DPAD_RIGHT (22)` | — | 前进/右移 |
| 长按 | 长按 ENTER | — | 上下文菜单 |

### 双指操作

系统还支持双指手势（通过系统属性开关）：
- `rokid.debug.two_finger_click = 1` → 双指点击已启用
- `rokid.debug.two_finger_flick = 1` → 双指轻弹已启用

---

## 功能键系统

### 按键类型

| 按键 | 芯片/实现 | 功能 |
|------|---------|------|
| 功能键 | Qualcomm PMIC (`qpnp_pon`) | 拍照/录像/AI |
| 音量+ | GPIO | 增大音量 |
| 音量- | GPIO | 减小音量 |
| HOME | GPIO | 返回主页 |
| CAMERA | GPIO | 相机快捷键 |
| FOCUS | GPIO | 对焦 |

### 功能键操作详解

| 操作 | 系统广播 | 默认行为 | 可配置为 |
|------|---------|---------|---------|
| 短按 | `ACTION_SPRITE_BUTTON_UP` | 拍照 | 录像 / 录音 |
| 长按 | `ACTION_SPRITE_BUTTON_LONG_PRESS` | 录像 | 拍照 / 录音 |
| 超长按 | `ACTION_SPRITE_BUTTON_VERY_VERY_LONG_PRESS` | 强制停止录像 | — |
| 按下 | `ACTION_SPRITE_BUTTON_DOWN` | 按下事件 | — |

配置方式：
- 短按功能：`settings_interaction_shortPressFun` → 默认 `"picture"`, 可选 `"video"` / `"audio"`
- 长按功能：`settings_interaction_longPressFun` → 默认 `"video"`, 可选 `"picture"` / `"audio"`

### AI 键

| 操作 | 广播 | 行为 |
|------|------|------|
| AI 键 | `ACTION_AI_START` | 根据模式选择本地 AI 或手机端助手 |
| 设置键 | `ACTION_SETTINGS_KEY` | 蓝牙已连接时触发快捷指令 |

AI 模式判断：
- 模式 ≠ "2" → 打开本地 AI 助手（`ai_assist` 场景）
- 模式 = "2" → 调用手机端语音助手 (`startVoiceRecognition`)

### 按键处理流程

```
物理按键事件发生
  ↓
FunctionKeyReceiver 接收广播
  ↓
┌─── 安全检查 ───────────────────────────────────┐
│ 1. 佩戴延迟检查 → 刚戴上 3 秒内忽略按键          │
│ 2. 防抖检查 → 1 秒内重复按键忽略                  │
│ 3. 设备状态检查 → 镜腿折叠/未佩戴则忽略           │
│ 4. OTA 检查 → 更新中播放"正在更新"TTS 并忽略      │
└───────────────────────────────────────────────┘
  ↓ 通过检查
按键分发:
  ├── 短按 → 发送 pressKey="1" 到手机 → 执行配置功能
  ├── 长按 → 发送 longPressKey="1" 到手机 → 执行配置功能
  ├── 超长按 → 强制停止录像
  ├── AI 键 → 判断 AI 模式 → 打开本地AI 或 手机端助手
  └── 设置键 → 检查蓝牙 → 发送 openAIFunction(2,2)
```

---

## 佩戴检测系统

### 实现原理

`RokidSysConfig` 中的 `PsensorObserver` 通过 Linux `UEventObserver` 监控 I2C extcon 设备节点。

### 检测事件

| 检测项 | UEvent 字段 | 系统属性 | 触发行为 |
|--------|------------|---------|---------|
| 镜腿展开 | `DOCK=1` | `vendor.rkd.glasses.is_spread=1` | 允许正常操作 |
| 镜腿折叠 | `DOCK=0` | `vendor.rkd.glasses.is_spread=0` | 忽略按键；启动关机倒计时 |
| 戴上眼镜 | `JIG=1` | `vendor.rkd.glasses.is_take_on=1` | 唤醒屏幕、播放音效、关闭充电 LED |
| 摘下眼镜 | `JIG=0` | `vendor.rkd.glasses.is_take_on=0` | 忽略按键；3 秒延迟防抖 |

### 折叠自动关机

当镜腿折叠且未在充电时：

```
折叠检测 → 开始倒计时
  │
  │  默认倒计时: 1,200,000ms = 20 分钟
  │  (可通过 rkd_shutdown_timeout 配置)
  │
  ▼  时间到
写入 I2C: auto_startup=1
  ↓
PowerManager.shutdown("leg_fold_timeout")
  ↓
眼镜关机
```

---

## 主界面 (Launcher) 交互

### 三页结构

Launcher 使用 ViewPager2 实现三页结构：

```
         ┌───────────────────────┐
  Page 0 │   消息列表页            │
         │   MsgListFragment      │
         │   显示手机通知消息       │
         └───────────┬───────────┘
                     │ 上下滑动切换
         ┌───────────▼───────────┐
  Page 1 │   ★ 中心主页 ★          │  ← 默认页
         │   CenterFragment       │
         │   时间、日期、计划卡片    │
         └───────────┬───────────┘
                     │ 上下滑动切换
         ┌───────────▼───────────┐
  Page 2 │   应用列表页            │
         │   AppListFragment      │
         │   网格布局的应用图标     │
         └───────────────────────┘
```

### 中心主页按键

| 按键 | 行为 |
|------|------|
| `BACK` | 息屏 (`PowerManagerUtils.goToSleep()`) |
| `DPAD_UP` | 切到消息列表 |
| `DPAD_DOWN` | 切到应用列表 |
| `ENTER` | 展开/折叠计划卡片 |

### 应用列表页导航

- 水平 GridLayout 网格布局
- 焦点驱动导航（用 D-Pad 左右移动焦点）
- 到达左边界第一次 → 显示"返回"提示 + 音效
- 到达左边界第二次 → 返回中心页
- 到达右边界 → 播放边界音效

### 音效反馈

| 事件 | 音效方法 |
|------|---------|
| 焦点切换 | `playFocusChangeSoundEffect()` |
| 边界触达 | `playFocusOutSoundEffect()` |

---

## 头部手势

| 库文件 | 功能 |
|--------|------|
| `librfm3dof.so` | 3DOF 头部姿态追踪 |
| `nodshake` | 点头(确认)/摇头(取消)手势检测 |

---

## 底部状态栏

### 布局

```
[时间] [WiFi信号] [蓝牙图标] [电量图标+%] [天气] [外设] [录音指示]
```

### WiFi 信号强度分级

| 信号强度 | 显示 |
|---------|------|
| ≥ -55 dBm | 满格 (5格) |
| ≥ -66 dBm | 3格 |
| ≥ -88 dBm | 2格 |
| ≥ -100 dBm | 1格 |
| < -100 dBm | 无信号 |

### 场景指示器

状态栏底部可嵌入 Fragment 显示当前活跃场景：
- 音乐播放中 🎵
- 通话中 📞
- 录音中 🔴
- 录像中 📹
- AI 助手运行中 🤖
- 点餐中 🍔
