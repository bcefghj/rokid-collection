# Rokid Glasses Control

通过 ADB + scrcpy 在 macOS 上查看并操控 Rokid AI 眼镜。双击对应的 `.command` 文件即可使用，无需打开终端。

## 文件说明

| 文件 | 用途 |
|------|------|
| `打开眼镜屏幕.command` | 双击：仅查看眼镜画面（投屏） |
| `操作眼镜.command` | 双击：查看画面，并用鼠标/键盘操作眼镜 |
| `键盘操作眼镜.command` | 双击：查看画面，并用方向键+空格键操作眼镜 |
| `key_control_glasses.py` | 键盘控制逻辑（由 `键盘操作眼镜.command` 自动调用） |

## 键盘快捷键（仅限 `键盘操作眼镜.command`）

| 按键 | 效果 |
|------|------|
| `←` 左方向键 | 眼镜向后滑动 |
| `→` 右方向键 | 眼镜向前滑动 |
| `空格` | 单击屏幕中心 |
| `空格` × 2（快速连按） | 双击屏幕中心 |

## 依赖安装

```bash
# 安装 adb（Android 调试工具）
brew install --cask android-platform-tools

# 安装 scrcpy（投屏 + 控制工具）
brew install scrcpy

# 安装 Python 键盘监听库（仅键盘操作模式需要）
pip3 install pynput
```

## 使用前准备

1. 在 Rokid 眼镜上开启 **开发者选项**：「设置 → 关于 → 连续点击版本号 7 次」
2. 开启 **USB 调试**：「设置 → 开发者选项 → USB 调试」
3. 用 **USB 线** 连接眼镜与 Mac，眼镜上弹出授权提示时点「允许」

## 使用方式

连接好眼镜后，在 Finder 中**双击**对应 `.command` 文件即可。

## 系统要求

- macOS（Apple Silicon / Intel 均可）
- Python 3
- Homebrew
