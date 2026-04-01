package com.rokidnav.util;

import android.view.KeyEvent;

public class KeyHelper {
    public static final int PROG1 = KeyEvent.KEYCODE_PROG_RED;
    public static final int PROG2 = KeyEvent.KEYCODE_PROG_GREEN;
    public static final int PROG3 = KeyEvent.KEYCODE_PROG_YELLOW;
    public static final int PROG4 = KeyEvent.KEYCODE_PROG_BLUE;

    public static boolean isUp(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP;
    }

    public static boolean isDown(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    public static boolean isLeft(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    public static boolean isRight(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    public static boolean isConfirm(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
    }

    public static boolean isBack(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK;
    }

    public static boolean isVoice(int keyCode) {
        return keyCode == PROG1
                || keyCode == KeyEvent.KEYCODE_MEDIA_RECORD;
    }
}
