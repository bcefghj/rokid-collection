# LC Hot100 - Rokid AR 眼镜 LeetCode 助手

> 一款专为 Rokid AR 眼镜设计的离线 LeetCode Hot100 刷题助手。
> 内置 100 道经典算法题 AI 讲解，支持触控板翻页浏览，无需联网。

**开发者**：bcefghj | **邮箱**：bcefghj@163.com

---

## 目录

- [效果展示](#效果展示)
- [功能特性](#功能特性)
- [快速安装](#快速安装)
- [操作说明](#操作说明)
- [自定义题目](#自定义题目)
- [从源码构建](#从源码构建)
- [项目结构](#项目结构)
- [开发文档](#开发文档)

---

## 效果展示

在 Rokid 眼镜上显示效果（单色绿光显示）：

```
剩余87题            LC Hot100           100题
▶ #1   两数之和    Easy
  #2   两数相加    Medium
  #3   无重复字符… Medium
  #4   寻找两个正… Hard
  ...
← 上一题 | ● 查看讲解 | ↑ 标记已会 | → 下一题
```

讲解页面：
```
#1 Easy             两数之和           1/4
#1 两数之和(Easy)
给定整数数组和目标值，
找出两数之和等于目标的下标
← 上一页 | ● 返回列表 | 下一页 →
```

---

## 功能特性

- **100 道经典题**：覆盖数组、链表、树、图、动态规划等核心算法分类
- **AI 生成讲解**：每题 4 页内容（题意→思路→伪代码→复杂度）
- **完全离线**：APK 内嵌所有数据，无需网络
- **标记已会**：上滑触控板标记/取消标记，已会题目置灰显示
- **进度显示**：顶栏实时显示剩余未会题目数量
- **可定制**：支持添加自定义题目，重新打包分发
- **可分享**：APK 可直接发给他人安装

---

## 快速安装

### 前提条件

- Rokid Glasses（RG-glasses，Android 12）
- 电脑安装 ADB（`brew install android-platform-tools`）
- USB 线连接眼镜，开启 USB 调试

### 安装步骤

```bash
# 1. 检查设备连接
adb devices
# 应看到: 1901092544011861  device

# 2. 安装 APK（本目录下已有预构建版）
adb install -r LC-Hot100.apk

# 3. 启动应用
adb shell am start -n com.rokid.lchot100/.MainActivity
```

---

## 操作说明

### 触控板手势映射

| 手势 | Android 键码 | 功能 |
|------|-------------|------|
| 向前滑（→） | KEYCODE_DPAD_RIGHT (22) | 下一题 / 下一页 |
| 向后滑（←） | KEYCODE_DPAD_LEFT (21) | 上一题 / 上一页 / 返回列表 |
| 单击（●） | KEYCODE_DPAD_CENTER (23) | 查看当前题讲解 / 返回列表 |
| 上滑（↑） | KEYCODE_DPAD_UP (19) | 标记当前题"已会" / 取消标记 |
| 功能键 | 系统键 | 返回主页 |

### 题目列表视图

- 屏幕显示 8 道题，当前选中题目用 `▶` 标记
- 已掌握的题目显示为灰色并带删除线和 `✓` 标记
- 顶栏左侧显示"剩余 XX 题"，右侧显示总题数

### 讲解视图

每道题分 4 页：

| 页 | 内容 |
|----|------|
| 第 1 页 | 题号、题名、难度 + 一句话题意 |
| 第 2 页 | 核心解题思路 |
| 第 3 页 | 关键代码（伪代码） |
| 第 4 页 | 时间/空间复杂度 + 易错点 |

---

## 自定义题目

### 添加单道题目（交互式）

```bash
cd scripts/
python3 add_problems.py
# 选择"1. 手动添加单道题目"
# 输入题目 ID、名称、难度等
# 可选择 AI 自动生成讲解（需要 MiniMax API Key）
```

### 批量导入（JSON 文件）

准备 JSON 文件，格式如下：

```json
[
  {
    "id": "200",
    "title": "岛屿数量",
    "difficulty": "Medium",
    "tags": ["图", "DFS", "BFS"],
    "pages": [
      "#200 岛屿数量(Medium)\n统计网格中由1组成的独立岛屿个数",
      "核心思路：DFS淹没\n遍历每个1，DFS把相邻1全变0\n每次DFS即一个完整岛屿",
      "for each cell(i,j)==1:\n  count++\n  dfs(i,j)  //把连通1变0",
      "时间O(MN) 空间O(MN)\n易错：八方向?→只需上下左右"
    ]
  }
]
```

如果不提供 `pages`，将自动调用 AI 生成。

```bash
python3 add_problems.py
# 选择"2. 批量从 JSON 文件导入"
# 输入 JSON 文件路径
```

### 重新生成所有讲解

```bash
cd scripts/
# 修改 generate_hot100.py 中的 API_KEY
python3 generate_hot100.py  # 支持断点续跑
python3 build.py            # 嵌入数据并构建 APK
adb install -r ../LC-Hot100.apk
```

---

## 从源码构建

### 环境要求

```bash
# macOS
brew install openjdk@17 android-platform-tools

# 安装 Android SDK
brew install --cask android-commandlinetools
export ANDROID_HOME="$HOME/Library/Android/sdk"
sdkmanager "platforms;android-33" "build-tools;33.0.2"
```

### 构建步骤

```bash
# 1. 确保数据文件存在
ls data/hot100_data.json

# 2. 运行构建脚本
cd scripts/
python3 build.py

# 3. 安装到眼镜
adb install -r ../LC-Hot100.apk
```

### 手动构建 APK（不依赖 build.py）

```bash
# 使用通用构建脚本（需要 ../build_apk.sh）
bash ../build_apk.sh android/ com.rokid.lchot100 "LC Hot100"
```

---

## 项目结构

```
LC助手/
├── LC-Hot100.apk                    # 预构建的 APK（可直接安装）
├── README.md                        # 本文档
├── DEVELOPMENT.md                   # 完整开发文档
│
├── android/                         # Android 项目源码
│   ├── AndroidManifest.xml          # 应用清单
│   ├── res/
│   │   └── raw/
│   │       └── index.html           # AR 眼镜 UI（含交互逻辑）
│   └── src/
│       └── com/rokid/lchot100/
│           └── MainActivity.java    # Android 主 Activity（WebView 容器）
│
├── data/
│   └── hot100_data.json             # 100 题 AI 讲解数据（JSON）
│
├── scripts/
│   ├── generate_hot100.py           # AI 批量生成讲解数据
│   ├── build.py                     # 构建 APK
│   └── add_problems.py              # 自定义题目添加工具
│
└── docs/
    └── screenshots/                 # 调试截图
```

---

## 开发文档

详细开发过程请参阅 [DEVELOPMENT.md](DEVELOPMENT.md)，包含：

- 架构设计决策
- 屏幕方向问题诊断与修复
- 踩坑记录
- Prompt Engineering 设计

---

## 题目分类

| 分类 | 题数 | 代表题目 |
|------|------|---------|
| 数组 & 哈希 | 21 | 两数之和、螺旋矩阵 |
| 双指针 & 滑窗 | 9 | 接雨水、最小覆盖子串 |
| 链表 | 14 | 反转链表、LRU缓存 |
| 二叉树 | 15 | 最近公共祖先、最大路径和 |
| 图 & 回溯 | 12 | 岛屿数量、N皇后 |
| 二分查找 | 6 | 寻找中位数、旋转数组 |
| 栈 & 堆 | 8 | 每日温度、数据流中位数 |
| 贪心 | 4 | 跳跃游戏、划分字母区间 |
| 动态规划 | 15 | 零钱兑换、编辑距离 |
| 位运算 & 其他 | 5 | 只出现一次的数字 |

---

## 许可证

MIT License © 2026 bcefghj

欢迎 Star、Fork 和 PR！如有问题请联系 bcefghj@163.com
