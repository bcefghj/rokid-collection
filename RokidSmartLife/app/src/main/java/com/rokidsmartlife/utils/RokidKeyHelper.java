package com.rokidsmartlife.utils;

import android.view.KeyEvent;

/**
 * Rokid 触控板实际按键映射：
 *
 * 镜腿方向（前后滑动）→ KEY_RIGHT(22) / KEY_LEFT(21)
 * 垂直方向（上下滑动）→ KEY_UP(19) / KEY_DOWN(20) — 但实际上用户很少用
 * 单击 → KEY_ENTER(66)
 * 返回（左滑到底）→ KEY_BACK(4)
 * 前滑自定义事件 → KEY_DASHBOARD(204) / key 183
 * 后滑自定义事件 → key 184
 *
 * 因此列表导航应该优先支持 LEFT/RIGHT 方向，而非 UP/DOWN
 */
public class RokidKeyHelper {

    public static final int KEYCODE_SPRITE_SWIPE_FORWARD = 183;
    public static final int KEYCODE_SPRITE_SWIPE_BACK = 184;
    public static final int KEYCODE_SPRITE_DOUBLE_TAP = 202;
    public static final int KEYCODE_DASHBOARD = 204;

    public static boolean isForwardSwipe(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KEYCODE_SPRITE_SWIPE_FORWARD;
    }

    public static boolean isBackSwipe(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KEYCODE_SPRITE_SWIPE_BACK
                || keyCode == KeyEvent.KEYCODE_BACK;
    }

    public static boolean isConfirm(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
    }

    public static boolean isScrollDown(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KEYCODE_SPRITE_SWIPE_FORWARD;
    }

    public static boolean isScrollUp(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KEYCODE_SPRITE_SWIPE_BACK;
    }
}
