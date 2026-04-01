# LC Hot100 完整开发文档

> Rokid AR 眼镜 LeetCode Hot100 离线助手 — 开发过程完整记录
>
> 开发者：bcefghj (bcefghj@163.com)
> 开发日期：2026 年 3 月

---

## 目录

1. [项目背景](#1-项目背景)
2. [设备特性分析](#2-设备特性分析)
3. [架构设计](#3-架构设计)
4. [开发环境搭建](#4-开发环境搭建)
5. [版本演进历程](#5-版本演进历程)
6. [核心技术实现](#6-核心技术实现)
7. [关键 Bug 与修复](#7-关键-bug-与修复)
8. [Prompt Engineering 设计](#8-prompt-engineering-设计)
9. [最终产物](#9-最终产物)
10. [面试价值点](#10-面试价值点)

---

## 1. 项目背景

### 1.1 目标

为 Rokid AR 眼镜开发一款面向算法学习的助手应用，作为 AI 应用工程师实习面试的演示项目。

**核心需求**：
- 完全离线运行（眼镜算力有限，不适合实时调用 LLM）
- 内置 LeetCode Hot100 题目的 AI 讲解
- 适配眼镜特殊交互（触控板，无键盘鼠标）
- 可分享（APK 形式，他人可直接安装）
- 支持个性化（用户可添加自定义题目）

### 1.2 技术栈选型

| 组件 | 选型 | 原因 |
|------|------|------|
| AI 讲解生成 | MiniMax-M2 (OpenAI 兼容 API) | 有可用 API Key，中文效果好 |
| Android 前端 | WebView + HTML/JS | 快速迭代，避免复杂 Android 开发 |
| 数据格式 | JSON 内嵌 HTML | 完全离线，无需数据库 |
| 持久化存储 | SharedPreferences via JavascriptInterface | 原生存储，比 localStorage 可靠 |
| APK 构建 | 命令行工具（aapt2/javac/d8/zipalign/apksigner） | 无需 Android Studio，快速迭代 |
| 内容生成 | Python + openai SDK | 批量生成，支持断点续跑 |

---

## 2. 设备特性分析

### 2.1 硬件参数

```
型号: RG-glasses
Android 版本: 12 (API 32)
物理屏幕: 480×640 像素（竖屏面板）
显示颜色: 单色绿光（Monochrome Green）
处理器: 轻量级 ARM，算力有限
存储: 约 17GB 可用（共 19GB）
```

### 2.2 屏幕方向机制（关键！）

这是开发中最重要也最容易踩坑的地方：

```
物理面板方向: 竖屏（480×640，Portrait）
光学投射方向: 横屏（在用户眼睛里看到的是横屏画面）
系统 screencap 截图: 480×640（竖屏，即物理面板方向）
```

**正确行为（VLC、KOReader 的做法）**：
- 不设置 `android:screenOrientation`
- 使用系统默认的 portrait 方向（rotation=0）
- screencap 截图为 480×640
- 用户看到的是横屏画面（光学系统旋转）

**错误行为（初版 LC 助手的做法）**：
- 设置 `android:screenOrientation="landscape"`
- Android 系统主动旋转画面 90°
- screencap 截图为 640×480
- 用户看到的是再次旋转后的错误方向（顺时针偏转 90°）

### 2.3 输入映射

Rokid 触控板映射为 Android DPAD 键码：

| 操作 | 键码 | 数值 |
|------|------|------|
| 向前滑（右滑） | KEYCODE_DPAD_RIGHT | 22 |
| 向后滑（左滑） | KEYCODE_DPAD_LEFT | 21 |
| 单击 | KEYCODE_DPAD_CENTER | 23 |
| 上滑 | KEYCODE_DPAD_UP | 19 |
| 下滑 | KEYCODE_DPAD_DOWN | 20 |
| 功能键 | KEYCODE_BACK / KEYCODE_HOME | 系统级 |

**注意**：长按触控板中键（KEYCODE_DPAD_CENTER 长按）在 Rokid 系统上不可靠，
会触发系统级通知栏，因此将"标记已会"改为上滑（UP）操作。

### 2.4 UI 设计约束

```
有效显示区域: 约 480×600（竖屏坐标系）
颜色方案: 黑底绿字（#000000 背景，#00ff41 前景）
字体大小: 主内容 ≥ 20px，状态栏 ≤ 12px
单屏内容: ≤ 45 汉字（约 4-5 行）
```

---

## 3. 架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────┐
│  开发阶段（MacBook Air）                              │
│  ┌─────────────┐    ┌──────────────┐                 │
│  │ MiniMax API │───▶│ generate_    │                 │
│  │ (MiniMax-M2)│    │ hot100.py    │                 │
│  └─────────────┘    └──────┬───────┘                 │
│                            │ hot100_data.json         │
│                     ┌──────▼───────┐                 │
│                     │  build.py    │──▶ APK 文件      │
│                     └──────────────┘                 │
│  ┌──────────────┐                                    │
│  │  build_apk.sh│ (aapt2+javac+d8+zipalign+apksigner)│
│  └──────────────┘                                    │
└─────────────────────────────────────────────────────┘
                    │ adb install
┌─────────────────────────────────────────────────────┐
│  运行阶段（Rokid Glasses）                            │
│  ┌──────────────────────────────────────────────┐   │
│  │ LC-Hot100.apk                                │   │
│  │  ┌─────────────────────────────────────────┐ │   │
│  │  │ MainActivity.java (Android Activity)    │ │   │
│  │  │  - WebView 容器                          │ │   │
│  │  │  - dispatchKeyEvent → JS onKey()         │ │   │
│  │  │  - JavascriptInterface (SharedPrefs)     │ │   │
│  │  └────────────────┬────────────────────────┘ │   │
│  │                   │ loadDataWithBaseURL        │   │
│  │  ┌────────────────▼────────────────────────┐ │   │
│  │  │ index.html (内嵌 100 题数据 + UI 逻辑)   │ │   │
│  │  │  - PROBLEMS_DATA: JSON 数组              │ │   │
│  │  │  - showList() / showPage()               │ │   │
│  │  │  - onKey(k) 处理触控板事件               │ │   │
│  │  │  - NativeBridge.save/load (持久化)       │ │   │
│  │  └─────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### 3.2 数据流

```
LeetCode Hot100 题目列表（hardcoded in generate_hot100.py）
        │
        ▼ MiniMax-M2 API（批量生成，断点续跑）
hot100_data.json（100 题 × 4页讲解）
        │
        ▼ build.py（JSON → JS 变量注入）
app_index.html（数据内嵌）
        │
        ▼ build_apk.sh（命令行工具链）
com-rokid-lchot100.apk
        │
        ▼ adb install
Rokid Glasses（离线运行）
```

---

## 4. 开发环境搭建

### 4.1 工具安装（macOS）

```bash
# ADB 工具
brew install android-platform-tools

# Java（APK 构建需要）
brew install openjdk@17
export JAVA_HOME="$(brew --prefix openjdk@17)"
export PATH="$JAVA_HOME/bin:$PATH"

# Android SDK
brew install --cask android-commandlinetools
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/build-tools/33.0.2:$PATH"

# 接受许可证
yes | sdkmanager --licenses

# 安装 SDK 组件
sdkmanager "platforms;android-33" "build-tools;33.0.2"

# 屏幕镜像（调试用）
brew install scrcpy
```

### 4.2 Python 依赖

```bash
pip3 install openai
```

### 4.3 连接设备

```bash
# USB 连接
adb devices
# 输出: 1901092544011861  device

# 屏幕镜像调试（Mac 桌面实时看眼镜画面）
scrcpy --window-title "Rokid Glasses"
```

---

## 5. 版本演进历程

### v0.1 — 在线版（已废弃）

**方案**：FastAPI 后端 + Android WebView 客户端

```
Rokid Glasses
  WebView → http://localhost:8080/glasses (adb reverse)
                ↑
  Mac FastAPI Server → MiniMax LLM API
```

**问题**：
1. 需要网络连接和 Mac 常驻服务，不方便分享
2. adb reverse 连接不稳定
3. Android 9+ 默认禁止明文 HTTP，需要额外配置

**代码**（保留参考）：见 `../rokid-leetcode-agent/server.py`

### v0.2 — 离线版（当前）

**方案**：预生成数据 + 完全离线 APK

**核心改进**：
- 用 `generate_hot100.py` 批量预生成所有讲解，存为 JSON
- `build.py` 将 JSON 内嵌到 HTML 中
- APK 完全独立，无外部依赖
- 用 `android:screenOrientation` 缺失（默认值）修复旋转问题

---

## 6. 核心技术实现

### 6.1 Android WebView 配置

```java
// MainActivity.java 关键配置

WebSettings ws = wv.getSettings();
ws.setJavaScriptEnabled(true);   // 必须
ws.setDomStorageEnabled(true);   // localStorage 备用
ws.setTextZoom(100);             // 禁止系统字体缩放（否则布局乱）

// 加载 res/raw/index.html（离线 HTML）
// 使用 file:///android_res/raw/ 作为 base URL
// - 保证 localStorage 能正常工作（需要 origin）
// - 保证 JavascriptInterface 能正常通信
wv.loadDataWithBaseURL(
    "file:///android_res/raw/",
    htmlContent,
    "text/html",
    "UTF-8",
    null
);
```

### 6.2 触控板事件拦截

```java
// 关键：使用 dispatchKeyEvent 而非 onKeyDown
// dispatchKeyEvent 在 Activity 分发链中更早执行，
// 能在 WebView 系统处理之前拦截按键

@Override
public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
        int kc = event.getKeyCode();
        // DPAD_LEFT=21, RIGHT=22, CENTER=23, UP=19, DOWN=20
        if (kc == 21) { wv.evaluateJavascript("onKey('left')", null); return true; }
        if (kc == 22) { wv.evaluateJavascript("onKey('right')", null); return true; }
        if (kc == 23) { wv.evaluateJavascript("onKey('center')", null); return true; }
        if (kc == 19) { wv.evaluateJavascript("onKey('up')", null); return true; }
        if (kc == 20) { wv.evaluateJavascript("onKey('down')", null); return true; }
    }
    return super.dispatchKeyEvent(event);
}
```

### 6.3 持久化存储（双重保障）

```javascript
// 优先使用 JavascriptInterface（更可靠）
// 降级到 localStorage（兼容浏览器调试）

function loadMastered() {
    try {
        var s = NativeBridge.load('mastered');
        if (s) masteredSet = JSON.parse(s);
    } catch(e) {
        try {
            var s = localStorage.getItem('lc_mastered');
            if (s) masteredSet = JSON.parse(s);
        } catch(e2) {}
    }
}

function saveMastered() {
    var s = JSON.stringify(masteredSet);
    try { NativeBridge.save('mastered', s); } catch(e) {}
    try { localStorage.setItem('lc_mastered', s); } catch(e) {}
}
```

### 6.4 UI 状态机

```javascript
// 两种 mode：list（题目列表）和 page（讲解页面）
var mode = 'list', ci = 0, pi = 0;

function onKey(k) {
    if (mode === 'list') {
        if (k === 'right')  { ci = Math.min(D.length-1, ci+1); showList(); }
        if (k === 'left')   { ci = Math.max(0, ci-1); showList(); }
        if (k === 'center') { pi = 0; showPage(); }       // 进入讲解
        if (k === 'up')     { toggleMastered(); showList(); } // 标记已会
    } else if (mode === 'page') {
        if (k === 'right')  { pi = Math.min(D[ci].pages.length-1, pi+1); showPage(); }
        if (k === 'left')   {
            if (pi > 0) { pi--; showPage(); }
            else showList();       // 第一页左滑=返回列表
        }
        if (k === 'center') { showList(); }               // 返回列表
        if (k === 'up')     { toggleMastered(); showPage(); } // 标记已会（在页面内也可以）
    }
}
```

### 6.5 APK 命令行构建流程

```bash
# build_apk.sh 的核心步骤（简化）

# 1. 编译资源
aapt2 compile -o compiled.flata res/raw/index.html
aapt2 link -o base.apk --manifest AndroidManifest.xml \
    --java gen/ -I android.jar compiled.flata

# 2. 编译 Java
javac -cp android.jar:gen/ -d obj/ src/.../MainActivity.java

# 3. 转换为 DEX（关键：用 wildcard 包含匿名内部类）
d8 $(find obj/ -name "*.class") --output obj/

# 4. 合并 APK
zip -j final.apk obj/classes.dex
zip -r final.apk res/

# 5. 对齐和签名
zipalign -v 4 final.apk aligned.apk
apksigner sign --ks ~/.rokid-debug.keystore aligned.apk
```

---

## 7. 关键 Bug 与修复

### Bug 1：屏幕方向旋转 90°（最重要！）

**现象**：在眼镜上显示时，画面顺时针旋转了 90°

**根因分析**：

```
错误逻辑：
  开发者设置 android:screenOrientation="landscape"
  → Android 系统将物理竖屏（480×640）旋转 90° 渲染
  → 光学系统再旋转一次投射到眼睛
  → 结果：二次旋转，方向错误

正确逻辑：
  不设置 screenOrientation（默认 portrait）
  → Android 系统在竖屏（480×640）上正常渲染
  → 光学系统旋转一次投射到眼睛
  → 结果：正确方向
```

**诊断方法**：对比截图分辨率

```bash
# VLC（已知正常）截图
adb shell screencap /sdcard/vlc.png && adb pull /sdcard/vlc.png .
# 结果: 480×640（竖屏）→ VLC 未设置 screenOrientation

# 问题版 LC 助手截图
adb shell screencap /sdcard/lc.png && adb pull /sdcard/lc.png .
# 结果: 640×480（横屏）→ 证明应用强制横屏了
```

**修复**：从 `AndroidManifest.xml` 中删除 `android:screenOrientation="landscape"`

```xml
<!-- 修复前 -->
<activity android:name="..."
    android:screenOrientation="landscape"  <!-- ← 删除这行 -->
    android:configChanges="orientation|screenSize|...">

<!-- 修复后 -->
<activity android:name="..."
    android:configChanges="orientation|screenSize|...">
```

**教训**：在 Rokid 眼镜上开发时，永远不要显式设置 `screenOrientation`，让系统和光学系统自己处理。

---

### Bug 2：`d8` 编译失败（匿名内部类）

**现象**：

```
Error: main class Main could not be found MainActivity$1.class
```

**根因**：`MainActivity$1.class` 是 Java 匿名内部类编译产物（如 `new JavascriptInterface() {...}`），
d8 命令只指定了 `MainActivity.class`，未包含这个文件。

**修复**：使用通配符查找所有 `.class` 文件

```bash
# 错误
d8 obj/com/rokid/lchot100/MainActivity.class --output obj/

# 正确
d8 $(find obj/ -name "*.class") --output obj/
```

---

### Bug 3：APK 签名 FileNotFoundException

**现象**：

```
FileNotFoundException: /path/with space/app.apk (No such file or directory)
```

**根因 1**：keystore 存放在临时构建目录，每次重建时消失

**修复 1**：将 keystore 存放到用户主目录的持久位置

```bash
KEYSTORE="$HOME/.rokid-debug.keystore"  # 而非 $BUILD/debug.keystore
```

**根因 2**：`cd` 命令改变工作目录后，相对路径失效

**修复 2**：脚本开头将项目目录转为绝对路径

```bash
PROJECT_DIR="$(cd "$1" && pwd)"  # 转为绝对路径
```

**根因 3**：输出 APK 文件名包含中文，某些系统无法处理

**修复 3**：用包名（全 ASCII）作为 APK 文件名

```bash
OUT_NAME=$(echo "$PACKAGE" | tr '.' '-')  # com.rokid.lchot100 → com-rokid-lchot100
```

---

### Bug 4：标记已会 - 长按不可靠

**现象**：`adb shell input keyevent --longpress 23` 触发系统通知栏，而非应用内长按

**根因**：Rokid 系统对长按 DPAD_CENTER 有全局拦截，用于弹出系统 UI

**修复**：将"标记已会"从长按中键改为上滑（DPAD_UP）

```javascript
// 修复前
else if (k === 'longcenter') { toggleMastered(); }

// 修复后
else if (k === 'up') { toggleMastered(); showList(); }
```

---

### Bug 5：MiniMax API 余额不足（模型选择）

**现象**：

```
RateLimitError: insufficient balance (1008)
```

**原因**：`MiniMax-M1` 模型（最新旗舰）余额不足

**修复**：切换到 `MiniMax-M2` 模型（同等能力，可用）

```python
MODEL_NAME = "MiniMax-M2"  # 而非 "MiniMax-M1"
```

**注意**：`MiniMax-M2` 的回复中可能包含 `<think>...</think>` 推理过程标签，
需要用正则表达式清除：

```python
import re
text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
```

---

## 8. Prompt Engineering 设计

### 8.1 设计约束

AR 眼镜的显示限制对 Prompt 设计有严格要求：

| 约束 | 参数值 |
|------|--------|
| 每页最大汉字数 | ≤ 45 字 |
| 页面分隔符 | `[PAGE]` |
| 固定页数 | 4 页/题 |
| 输出语言 | 极简中文 |

### 8.2 最终 Prompt

```python
SYSTEM_PROMPT = """你是算法讲师。请用极简中文讲解这道LeetCode题，严格按以下格式输出，用[PAGE]分隔每页：

[PAGE]题号 题名(难度)
一句话概括题意
[PAGE]核心思路：XX
2-3句关键思路
[PAGE]关键代码(伪代码)
3-5行核心逻辑
[PAGE]复杂度
时间O(?) 空间O(?)
易错点提醒

每页不超过45个汉字。不要输出多余内容。"""

# User prompt 示例
prompt = "#1 两数之和（Easy）标签：数组, 哈希表"
```

### 8.3 解析逻辑

```python
# 去除推理过程，按分隔符切割
text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
pages = [p.strip() for p in text.split("[PAGE]") if p.strip()]
```

### 8.4 Prompt 演进

**v1（失败）**：
```
"请用中文讲解 {题目内容}"
→ 输出格式不固定，无法分页，内容过长
```

**v2（改进）**：
```
"请分页讲解，每页不超过50字"
→ 模型不严格遵守字数，分页格式不稳定
```

**v3（最终）**：
```
提供明确的格式示例（few-shot），用 [PAGE] 作为分隔符
→ 稳定输出 4 页固定格式，适合 AR 眼镜显示
```

**关键经验**：
1. 在 System Prompt 中提供 **示例格式** 比仅描述格式效果好
2. **分隔符** 选择 `[PAGE]`（不常见字符串，避免与正文冲突）
3. 字数限制用 **每页独立约束** 而非全局约束

---

## 9. 最终产物

### 9.1 文件清单

| 文件 | 说明 |
|------|------|
| `LC-Hot100.apk` | 可直接安装的 APK（预构建） |
| `data/hot100_data.json` | 100 题 AI 讲解数据 |
| `android/res/raw/index.html` | AR 眼镜 UI（含交互逻辑） |
| `android/src/.../MainActivity.java` | Android WebView 容器 |
| `android/AndroidManifest.xml` | 应用清单（无 screenOrientation） |
| `scripts/generate_hot100.py` | AI 批量生成讲解（支持断点续跑） |
| `scripts/build.py` | 数据嵌入 + APK 构建 |
| `scripts/add_problems.py` | 自定义题目添加工具 |

### 9.2 应用截图（screencap 视角）

调试截图文件存放在 `docs/screenshots/`，由 ADB 截图命令获取：

```bash
adb shell screencap /sdcard/lc_debug.png
adb pull /sdcard/lc_debug.png docs/screenshots/
```

注意：截图呈竖屏（480×640），但在眼镜中用户看到的是横屏。

### 9.3 测试命令

```bash
# 安装
adb install -r LC-Hot100.apk

# 启动
adb shell am start -n com.rokid.lchot100/.MainActivity

# 模拟触控板操作
adb shell input keyevent 22   # 向右（下一题）
adb shell input keyevent 21   # 向左（上一题）
adb shell input keyevent 23   # 单击（进入讲解）
adb shell input keyevent 19   # 上滑（标记已会）

# 查看日志
adb logcat | grep -E "chromium|WebView"

# 截图
adb shell screencap /sdcard/sc.png && adb pull /sdcard/sc.png .

# 卸载
adb uninstall com.rokid.lchot100
```

---

## 10. 面试价值点

### 10.1 LLM 应用开发

- **Prompt Engineering**：设计面向小屏的极简分页 Prompt，控制输出格式和长度
- **API 集成**：使用 OpenAI 兼容接口（MiniMax-M2），处理流式输出和错误重试
- **批量处理**：断点续跑机制，避免大批量 API 调用失败导致全部重做

### 10.2 AI 辅助开发

- **离线优先设计**：LLM 计算在部署时完成，运行时完全离线
- **边缘设备适配**：针对低算力 AR 设备的架构选型（WebView + 内嵌数据）

### 10.3 工程实践

- **跨端开发**：Python 数据生成 → Android APK 打包 → AR 设备运行
- **调试技巧**：screencap + scrcpy 在不戴眼镜情况下调试 AR 应用
- **问题诊断**：通过对比截图分辨率定位屏幕方向 Bug（480×640 vs 640×480）

### 10.4 产品思维

- **可分享**：完整独立 APK，他人无需开发环境直接安装
- **可定制**：提供工具脚本让用户扩展题库
- **用户体验**：适配眼镜特殊交互（改长按为上滑，更可靠）

---

## 附录：build_apk.sh 关键代码

```bash
#!/bin/bash
# 通用 Android WebView APK 构建脚本

PROJECT_DIR="$(cd "$1" && pwd)"   # 转绝对路径（关键！）
PACKAGE="${2:-com.rokid.app}"
APP_NAME="${3:-RokidApp}"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$ANDROID_HOME/build-tools/33.0.2"
PLATFORM="$ANDROID_HOME/platforms/android-33/android.jar"
KEYSTORE="$HOME/.rokid-debug.keystore"   # 持久化位置（关键！）
BUILD="$PROJECT_DIR/build"

# 生成 keystore（首次）
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" -alias rokid-debug \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Rokid Dev,O=Debug,C=CN"
fi

# AndroidManifest.xml（注意：不设置 screenOrientation）
cat > "$BUILD/AndroidManifest.xml" << MANIFEST
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$PACKAGE" android:versionCode="1" android:versionName="1.0">
  <uses-sdk android:minSdkVersion="28" android:targetSdkVersion="33" />
  <uses-permission android:name="android.permission.INTERNET" />
  <application android:label="$APP_NAME" android:usesCleartextTraffic="true"
      android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
    <activity android:name="$PACKAGE.MainActivity" android:exported="true"
        android:configChanges="orientation|screenSize|keyboard|keyboardHidden">
      <!-- ★ 无 screenOrientation ★ -->
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
MANIFEST

# 编译
"$BUILD_TOOLS/aapt2" compile -o "$BUILD/compiled.flata" \
    "$PROJECT_DIR/res/raw/index.html"
"$BUILD_TOOLS/aapt2" link -o "$BUILD/base.apk" \
    --manifest "$BUILD/AndroidManifest.xml" \
    --java "$BUILD/gen/" -I "$PLATFORM" \
    "$BUILD/compiled.flata"

javac -cp "$PLATFORM:$BUILD/gen/" -d "$BUILD/obj/" \
    "$PROJECT_DIR/src/$PACKAGE_PATH/MainActivity.java"

# 包含所有 .class 文件（含匿名内部类）
"$BUILD_TOOLS/d8" $(find "$BUILD/obj" -name "*.class") \
    --output "$BUILD/obj/"

# 打包、对齐、签名
OUT_NAME=$(echo "$PACKAGE" | tr '.' '-')
zip -j "$BUILD/base.apk" "$BUILD/obj/classes.dex"
"$BUILD_TOOLS/zipalign" -v 4 "$BUILD/base.apk" "$PROJECT_DIR/$OUT_NAME.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" \
    --ks-pass pass:android --key-pass pass:android \
    "$PROJECT_DIR/$OUT_NAME.apk"

echo "✅ 构建完成: $PROJECT_DIR/$OUT_NAME.apk"
```

---

*本文档由 bcefghj (bcefghj@163.com) 编写，记录了 LC Hot100 Rokid AR 眼镜助手的完整开发过程。*
