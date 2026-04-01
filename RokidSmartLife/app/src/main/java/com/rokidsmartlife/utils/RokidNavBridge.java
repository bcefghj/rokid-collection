package com.rokidsmartlife.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 桥接 Rokid 原生导航系统。
 *
 * Rokid Launcher 的 NavigationPageActivity 是 exported=true 的，
 * 接受 "init_param" extra 传入 NaviStart JSON。
 *
 * 导航页打开后，手机端 Rokid App 通过蓝牙 GATT 发送实际路线数据。
 * 需要手机蓝牙已连接。
 */
public class RokidNavBridge {

    private static final String TAG = "RokidNavBridge";

    private static final String LAUNCHER_PACKAGE = "com.rokid.os.sprite.launcher";
    private static final String NAVI_ACTIVITY =
            "com.rokid.os.sprite.launcher.page.navigation.NavigationPageActivity";
    private static final String NAVI_OVERSEA_ACTIVITY =
            "com.rokid.os.sprite.launcher.page.navigation.NavigationOverseaPageActivity";

    private static final String ASSIST_PACKAGE = "com.rokid.os.sprite.assistserver";
    private static final String ASSIST_ACTION = "com.rokid.os.master.assist.server.cmd";

    public static final int NAVI_TYPE_DRIVE = 0;
    public static final int NAVI_TYPE_WALK = 1;
    public static final int NAVI_TYPE_RIDE = 2;

    /**
     * 启动 Rokid 原生导航，传入目的地名称。
     * 手机端 Rokid App 会通过蓝牙接收到导航请求并启动高德导航。
     */
    public static boolean startNavigation(Context context, String destination, int naviType) {
        try {
            String param = "{\"destination\":\"" + escapeJson(destination) +
                    "\",\"naviType\":" + naviType +
                    ",\"locPermissionTip\":\"\",\"totalDistance\":0}";

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(LAUNCHER_PACKAGE, NAVI_ACTIVITY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("init_param", param);
            context.startActivity(intent);
            Log.d(TAG, "Started navigation to: " + destination);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start navigation", e);
            return false;
        }
    }

    /**
     * 通过广播打开导航场景（不带目的地）
     */
    public static void openNavigationScene(Context context) {
        try {
            Intent intent = new Intent(ASSIST_ACTION);
            intent.setPackage(ASSIST_PACKAGE);
            intent.putExtra("cmd_type", "control_scene");
            intent.putExtra("scene", "navigation");
            intent.putExtra("open", "true");
            context.sendBroadcast(intent);
            Log.d(TAG, "Opened navigation scene via broadcast");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open navigation scene", e);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
