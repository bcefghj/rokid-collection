#!/usr/bin/env python3
# 键盘控制眼镜：左键=向后滑动，右键=向前滑动，空格=单击，连续两次空格=双击

import subprocess
import re
import time

try:
    from pynput import keyboard
except ImportError:
    print("请先安装 pynput：pip3 install pynput")
    exit(1)

# 双击间隔（秒）
DOUBLE_TAP_INTERVAL = 0.35
last_space_time = 0

def adb(cmd):
    subprocess.run(['adb'] + cmd, capture_output=True, timeout=5)

def get_screen_size():
    r = subprocess.run(['adb', 'shell', 'wm', 'size'], capture_output=True, text=True, timeout=5)
    m = re.search(r"(\d+)\s*[x×]\s*(\d+)", r.stdout or r.stderr or "")
    if m:
        return int(m.group(1)), int(m.group(2))
    return 960, 540  # 默认

def tap(x, y):
    adb(['shell', 'input', 'tap', str(int(x)), str(int(y))])

def swipe(x1, y1, x2, y2, duration_ms=150):
    adb(['shell', 'input', 'swipe', str(int(x1)), str(int(y1)), str(int(x2)), str(int(y2)), str(duration_ms)])

def on_space():
    global last_space_time
    now = time.time()
    if now - last_space_time < DOUBLE_TAP_INTERVAL:
        # 双击
        w, h = get_screen_size()
        cx, cy = w / 2, h / 2
        tap(cx, cy)
        time.sleep(0.05)
        tap(cx, cy)
        last_space_time = 0
        return
    last_space_time = now
    w, h = get_screen_size()
    tap(w / 2, h / 2)

def on_left():
    w, h = get_screen_size()
    x1, x2 = w * 0.1, w * 0.9
    y = h / 2
    swipe(x1, y, x2, y)  # 向左键 → 滑动向后（左→右）

def on_right():
    w, h = get_screen_size()
    x1, x2 = w * 0.9, w * 0.1
    y = h / 2
    swipe(x1, y, x2, y)  # 向右键 → 滑动向前（右→左）

def on_press(key):
    try:
        if key == keyboard.Key.left:
            on_left()
        elif key == keyboard.Key.right:
            on_right()
        elif key == keyboard.Key.space:
            on_space()
    except Exception:
        pass

def main():
    print("键盘控制已开启（左=向后滑，右=向前滑，空格=单击，连按两次空格=双击）")
    print("按 Ctrl+C 结束")
    with keyboard.Listener(on_press=on_press) as listener:
        listener.join()

if __name__ == "__main__":
    main()
