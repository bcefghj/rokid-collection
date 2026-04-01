# 乐奇眼镜钢琴谱 — 开发文档

> 设备：Rokid RG-glasses（Android 12）  
> 开发语言：Java + HTML/JS  
> 构建方式：纯命令行（无需 Android Studio）  
> 最后更新：2026-03-13

---

## 目录结构

```
.
├── README.md                        # 本文档
├── build_apk.sh                     # APK 构建脚本（核心）
└── rokid-music-score/               # 钢琴谱应用
    ├── scores_data.json             # 曲谱数据（唯一需要编辑的内容）
    ├── index.html                   # 前端 UI + 交互逻辑（模板）
    ├── index_built.html             # 构建产物（数据已嵌入，用于打包）
    ├── build.py                     # 将数据嵌入 HTML 的脚本
    └── com-rokid-musicscore.apk     # 最终 APK（直接安装到眼镜）
```

---

## 一、设备信息

| 参数 | 值 |
|------|-----|
| 型号 | Rokid RG-glasses |
| 系统 | Android 12 |
| 物理屏幕 | 480×640（竖屏面板） |
| 光学显示 | 衍射波导，投射到眼睛为横屏 |
| 显示颜色 | 单色绿光（Micro LED） |
| 输入方式 | 触控板（前滑/后滑/单击/长按）、功能键 |

**屏幕方向重要说明：** 物理屏为竖屏（480×640），光学系统已将画面旋转为横屏投射。因此代码中**不要设置** `screenOrientation`，HTML 按 480 宽 × 640 高设计即可正确显示。

---

## 二、触控板按键映射

乐奇触控板映射为标准 Android DPAD 键码：

| 触控板操作 | Android 键码 | 数值 |
|-----------|-------------|------|
| 向前滑 | KEYCODE_DPAD_RIGHT | 22 |
| 向后滑 | KEYCODE_DPAD_LEFT | 21 |
| 单击 | KEYCODE_DPAD_CENTER | 23 |
| 上滑 | KEYCODE_DPAD_UP | 19 |
| 下滑 | KEYCODE_DPAD_DOWN | 20 |

**关键：** 使用 `dispatchKeyEvent` 而非 `onKeyDown`，前者更早介入事件分发链，可拦截系统级按键。同时加 `event.getRepeatCount() == 0` 防止长按触发多次翻页。

---

## 三、应用操作逻辑

### 菜单页（歌手/歌曲列表）
| 操作 | 效果 |
|------|------|
| 上滑 / 下滑 | 上下选择 |
| 单击 | 进入 |

### 谱面页（浏览钢琴谱）
| 操作 | 效果 |
|------|------|
| 单击 | 下一页 |
| 右滑 / 下滑 | 下一页 |
| 左滑 / 上滑 | 上一页 |
| 最后一页单击或右滑 | 回到歌曲列表 |

---

## 四、曲谱数据格式

曲谱存储在 `scores_data.json`，为 JSON 数组，每首歌一个对象：

```json
[
  {
    "artist": "周杰伦",
    "title": "晴天",
    "type": "piano",
    "key": "G",
    "bpm": 76,
    "time": "4/4",
    "pages": [
      "第1页内容（封面）",
      "第2页内容（谱面）",
      "..."
    ]
  }
]
```

### 第1页：封面页（固定格式）

```
晴天 - 周杰伦
调: G大调  拍: 4/4  ♩=76
词曲: 周杰伦  专辑: 叶惠美

1=G  2=A  3=B  4=C  5=D
6=E  7=#F  .=低八度
-=延音  0=休止
```

### 第2页起：谱面页（三行对齐格式）

每页放 **3组**（6小节），每组固定 4 行：

```
【段落名称 第X-Y小节】
右:  5   5   6   7  |  2   7   6   5
词:  故  事  的  小  |  黄  花  从  出
左:  G              |  D/F#
```

- `右:` 行 — 右手旋律，数字简谱（1234567），音符等间距排列
- `词:` 行 — 歌词，每个字对应上方一个音符，`-` 表示延续
- `左:` 行 — 左手和弦名称，标注在对应小节下方
- `|` — 小节线，每组包含两小节（4拍+4拍）

### 数字简谱符号说明

| 符号 | 含义 |
|------|------|
| `1` ~ `7` | 对应调式的七个音 |
| `.`（后缀）| 低八度，如 `6.` 表示低音 6 |
| 上方加点 | 高八度（文本模式用上下文表示） |
| `-` | 延音（占一拍，不唱新字） |
| `0` | 休止符 |
| `\|` | 小节线 |
| `#` | 升号，如 `#F` = F# |
| `b` | 降号，如 `bE` = bE |

---

## 五、当前内置曲目

| 歌手 | 歌曲 | 调 | 页数 | 覆盖范围 |
|------|------|----|------|---------|
| 周杰伦 | 晴天 | G 大调 | 17页 | 完整（前奏→主歌→副歌×3→结尾）|
| 周杰伦 | 青花瓷 | C 调 | 18页 | 完整（前奏→A1→A2→副歌→间奏→B1→副歌×2→结尾）|
| 周杰伦 | 稻香 | A 大调 | 15页 | 完整（前奏→说唱A1→副歌→间奏→说唱A2→副歌→尾奏）|

---

## 六、开发环境搭建

```bash
# 1. 安装 Java 17
brew install openjdk@17

# 2. 安装 Android 命令行工具
brew install --cask android-commandlinetools

# 3. 安装 SDK 组件
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
sdkmanager "platforms;android-33" "build-tools;33.0.2"
sdkmanager --licenses

# 4. 安装 ADB
brew install android-platform-tools
```

---

## 七、构建流程

### 步骤 1：编辑曲谱数据

修改 `rokid-music-score/scores_data.json`，添加或修改歌曲。

### 步骤 2：嵌入数据到 HTML

```bash
cd rokid-music-score
python3 build.py
```

这会将 `scores_data.json` 的内容作为 JS 变量内嵌到 `index.html` 中，生成 `index_built.html`。

### 步骤 3：构建 APK

```bash
cd ..   # 回到项目根目录
bash build_apk.sh rokid-music-score com.rokid.musicscore "钢琴谱" index_built.html
```

参数说明：
- `rokid-music-score` — 项目目录
- `com.rokid.musicscore` — 包名
- `"钢琴谱"` — 应用显示名称
- `index_built.html` — 要打包的 HTML 文件

构建成功后输出：`rokid-music-score/com-rokid-musicscore.apk`

### 步骤 4：安装到眼镜

```bash
# 先确认眼镜已连接（需在 Rokid 手机 App 中开启 ADB 调试，磁吸线连接电脑）
adb devices
# 输出示例: 1901092544011861  device

# 安装
adb install -r rokid-music-score/com-rokid-musicscore.apk

# 启动
adb shell am start -n com.rokid.musicscore/.MainActivity
```

---

## 八、完整构建脚本原理（build_apk.sh）

```
输入: 项目目录 + 包名 + 应用名 + HTML文件
  │
  ├── 1. 生成 AndroidManifest.xml
  ├── 2. 生成 MainActivity.java（内嵌按键处理逻辑）
  ├── 3. aapt2 compile — 编译资源
  ├── 4. aapt2 link   — 链接资源，生成未签名 APK 骨架
  ├── 5. javac        — 编译 Java 源码
  ├── 6. d8           — 将 .class 转为 DEX
  ├── 7. zip          — 将 DEX 塞入 APK
  ├── 8. zipalign     — 对齐 APK
  └── 9. apksigner    — 签名（使用 ~/.rokid-debug.keystore，自动生成）
输出: com-rokid-musicscore.apk
```

---

## 九、技术架构

```
┌─────────────────────────────────────┐
│           MainActivity.java          │
│  - 加载 index_built.html 到 WebView  │
│  - 拦截触控板键码                     │
│  - 转发键码给 JS: onKey('left')等    │
│  - NativeBridge: 提供 SharedPrefs   │
└──────────────┬──────────────────────┘
               │ evaluateJavascript()
┌──────────────▼──────────────────────┐
│           index_built.html           │
│  - 曲谱数据（SCORES_DATA JS变量）    │
│  - 三级菜单：歌手→歌曲→谱面          │
│  - 谱面渲染：三行对齐（右/词/左）    │
│  - 按键处理：onKey() 函数            │
│  - 防抖：300ms 内忽略重复按键        │
└─────────────────────────────────────┘
```

---

## 十、添加新曲谱

1. 打开 `rokid-music-score/scores_data.json`
2. 在数组末尾添加新对象，格式参考第四节
3. 执行 `python3 rokid-music-score/build.py`
4. 执行 `bash build_apk.sh rokid-music-score com.rokid.musicscore "钢琴谱" index_built.html`
5. 执行 `adb install -r rokid-music-score/com-rokid-musicscore.apk`

### 编写谱面页的注意事项

- 每页放 **3组**（6小节），充分利用屏幕空间
- 每组必须有 `右:` / `词:` / `左:` 三行，顺序不能乱
- 数字和汉字保持等间距（各占约4个字符宽度）
- 前奏/间奏/尾奏无歌词，`词:` 行用 `-` 占位
- `|` 两侧各留一个空格

---

## 十一、踩坑记录

### 坑1：screenOrientation 导致画面旋转

**现象：** 设置 `android:screenOrientation="landscape"` 后，画面在眼镜里顺时针旋转 90°。  
**原因：** 眼镜物理屏是竖屏，光学系统已将画面旋转投射成横屏。设置 landscape 后 Android 再旋转一次，方向错误。  
**解决：** 不设置 `screenOrientation`，使用系统默认。

### 坑2：onKeyDown 无法拦截系统级按键

**现象：** 部分 DPAD 键被系统 Launcher 拦截，应用内无响应。  
**解决：** 改用 `dispatchKeyEvent`，在事件分发链更早处执行。

### 坑3：长按/快速滑动导致翻多页

**现象：** 快速滑动时一次触发多个按键事件。  
**解决：**
1. Java 层：`event.getRepeatCount() == 0`，忽略长按重复
2. JS 层：`lastKeyTime` 防抖，300ms 内忽略重复事件

### 坑4：d8 编译漏掉匿名内部类

**现象：** 运行时 `ClassNotFoundException: MainActivity$1`。  
**解决：** 使用 `$(find obj -name "*.class")` 而非单独指定类文件。

### 坑5：中文/数字等宽对齐

**现象：** 中文字符和数字宽度不同，三行谱面对不齐。  
**解决：** 使用 `monospace` 字体 + `white-space: pre`，全部行用相同字号。

---

*开发者：bcefghj | bcefghj@163.com*
