# 开发者指南

> 如何为 Rokid AI 眼镜开发你自己的应用？

---

## 开发环境要求

| 项目 | 要求 |
|------|------|
| **目标 SDK (targetSdk)** | Android 12 (API 32) |
| **编译 SDK (compileSdk)** | 建议 34 |
| **最低 SDK (minSdk)** | Android 10 (API 29) |
| **屏幕分辨率** | 480×640 竖屏 |
| **输入方式** | D-Pad KeyEvent + 自定义 KeyCode |
| **Google Play 服务** | 不可用（无 GMS） |

---

## UI 开发注意事项

### 1. 没有触摸屏

这是最重要的一点：**Rokid 眼镜没有触摸屏**，所有交互都是通过 D-Pad 焦点导航完成的。

你需要：
- 所有可交互元素必须设置 `android:focusable="true"`
- 提供清晰的焦点状态样式（高亮边框、颜色变化等）
- 使用 `android:nextFocusUp/Down/Left/Right` 控制焦点顺序

### 2. 屏幕很小

480×640 像素，而且是竖屏。内容要做到：
- 文字要大，易于阅读
- 布局要简洁，不要堆太多信息
- 一个屏幕只做一件事

### 3. 音效反馈

建议在以下时机播放音效：
- 焦点切换时
- 按钮点击时
- 到达列表边界时
- 操作成功/失败时

### 4. 按键防抖

建议使用 200ms 防抖（Rokid 提供了 `FuncDebounce` 工具类）。

---

## 监听按键事件

### 触控板事件

在 Activity 中重写 `dispatchKeyEvent`：

```java
@Override
public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_UP) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:     // 触控板向上滑
                handleScrollUp();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:    // 触控板向下滑
                handleScrollDown();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:    // 触控板向左滑
                handleGoBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:   // 触控板向右滑
                handleGoForward();
                return true;
            case KeyEvent.KEYCODE_ENTER:        // 触控板单击/确认
                handleConfirm();
                return true;
            case KeyEvent.KEYCODE_BACK:         // 返回
                handleBack();
                return true;
        }
    }
    return super.dispatchKeyEvent(event);
}
```

### 功能键事件

功能键通过系统广播发送，需要注册 BroadcastReceiver：

```java
// 注册广播接收器
IntentFilter filter = new IntentFilter();
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_UP");             // 短按
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS");     // 长按
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_VERY_VERY_LONG_PRESS"); // 超长按
filter.addAction("com.android.action.ACTION_SPRITE_BUTTON_DOWN");           // 按下
filter.addAction("com.android.action.ACTION_AI_START");                     // AI 键
filter.addAction("com.android.action.ACTION_SETTINGS_KEY");                 // 设置键
registerReceiver(new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // 处理不同的按键事件
    }
}, filter);
```

### 自定义 KeyCode

| KeyCode | 值 | 手势 |
|---------|-----|------|
| `SPRITE_DOUBLE_TAP` | 202 | 双击 |
| `SPRITE_SWIPE_FORWARD` | 183 | 前滑 |
| `SPRITE_SWIPE_BACK` | 184 | 后滑 |

---

## 与 AssistServer 通信

### 绑定 MasterAssistService

```java
Intent intent = new Intent("com.rokid.p007os.sprite.assist.MasterAssistService");
intent.setPackage("com.rokid.os.sprite.assistserver");
bindService(intent, new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        IAssistServer assistServer = IAssistServer.Stub.asInterface(service);
        // 注册客户端
        assistServer.registerClient(getPackageName(), callback);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // 服务断开
    }
}, BIND_AUTO_CREATE);
```

### 发送控制命令

```java
// 通过 JSON 命令控制功能
String jsonCommand = "{\"action\":\"openScene\",\"sceneKey\":\"your_scene\"}";
assistServer.controlMsgJson(getPackageName(), jsonCommand);
```

---

## 读取系统属性

```java
// 检查佩戴状态
String takeOn = SystemProperties.get("vendor.rkd.glasses.is_take_on");
// "1" = 已佩戴, "0" = 未佩戴

// 检查镜腿展开状态
String spread = SystemProperties.get("vendor.rkd.glasses.is_spread");
// "1" = 展开, "0" = 折叠

// 获取设备信息
String model = SystemProperties.get("ro.product.model");      // "RG-glasses"
String version = SystemProperties.get("ro.build.version.incremental");
String cxrVersion = SystemProperties.get("rokid.cxr-service.version"); // "1.109"
```

### 常用系统属性一览

| 属性 | 类型 | 说明 |
|------|------|------|
| `vendor.rkd.glasses.is_take_on` | 0/1 | 佩戴状态 |
| `vendor.rkd.glasses.is_spread` | 0/1 | 镜腿展开状态 |
| `persist.rkd.enablePsensor` | bool | 接近传感器开关 |
| `persist.rkd.leg.fold_timeout.enable` | bool | 折叠关机功能开关 |
| `persist.rkd.local.tts.init.flag` | bool | TTS 初始化标志 |
| `rokid.psensor.mode` | string | 接近传感器模式 |
| `rokid.debug.two_finger_click` | 0/1 | 双指点击开关 |
| `rokid.debug.two_finger_flick` | 0/1 | 双指轻弹开关 |
| `rokid.cxr-service.version` | string | CXR 服务版本 |
| `debug.rokid.screenrecord_state` | string | 屏幕录制状态 |
| `persist.rokid.sprite.ota.running` | bool | OTA 运行状态 |
| `persist.rokid.stream.state` | string | 流状态 |

---

## 安装和调试

### 通过 ADB 安装

```bash
# 连接 USB 后
adb devices                          # 确认设备连接
adb install your_app.apk             # 安装应用
adb shell am start -n com.your.package/.MainActivity  # 启动应用
```

### 安装为系统应用

需要 root 或 system 权限：

```bash
adb push your_app.apk /product/app/YourApp/
adb shell pm install -r /product/app/YourApp/your_app.apk
```

### 调试技巧

```bash
# 查看日志
adb logcat | grep "YourTag"

# 查看当前前台 Activity
adb shell dumpsys activity activities | grep mFocusedActivity

# 查看系统属性
adb shell getprop vendor.rkd.glasses.is_take_on

# 发送按键事件 (模拟触控板操作)
adb shell input keyevent 66    # ENTER (单击)
adb shell input keyevent 19    # DPAD_UP (上滑)
adb shell input keyevent 20    # DPAD_DOWN (下滑)
adb shell input keyevent 21    # DPAD_LEFT (左滑)
adb shell input keyevent 22    # DPAD_RIGHT (右滑)

# 截屏
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

---

## 关键注意事项

### 权限限制

| 权限 | 说明 |
|------|------|
| `android.uid.system` | Rokid 核心应用使用系统 UID，第三方应用通常没有 |
| CXR 通信 | 与手机通信需要通过 CXRService 的 Caps 协议，需要 Rokid SDK |
| 相机 | 使用 CameraX/Camera2 标准 API，注意与 AssistServer 的相机冲突 |
| 传感器 | 标准 Android SensorManager API 即可访问 IMU 数据 |

### 已知限制

1. **没有 Google Play 服务** — 不能使用 GMS 相关 API（如 Firebase、Google Maps 等）
2. **没有触摸屏** — 必须支持 D-Pad 焦点导航
3. **屏幕小** — 480×640，UI 必须简洁
4. **电池有限** — 避免高频后台任务
5. **相机冲突** — AssistServer 可能占用相机，需要协调
6. **蓝牙限制** — GATT 通信需通过 CXR 协议

---

## 广播 Action 速查

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

---

## 关键文件路径

| 路径 | 用途 |
|------|------|
| `/sdcard/DCIM/Camera/` | 拍照存储 |
| `/sdcard/Movies/Camera/` | 录像存储 |
| `/sdcard/Recordings/` | 录音存储 |
| `/sdcard/ScreenRecorder/` | 屏幕录制 |
| `/sdcard/SpatialPhotos/` | 空间照片 |
| `/sdcard/Download/` | Web 传输目录 |
| `/sys/bus/spi/devices/spi0.0/hfp_on` | HFP 音频通道开关 |
