package com.rokidsmartlife.location;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WifiLocationHelper {
    private static final String TAG = "WifiLocation";
    private final WifiManager wifiManager;

    public WifiLocationHelper(Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * @return 已连接 WiFi 的 "mac,signal,ssid"，或 null
     */
    public String getConnectedWifiMac() {
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getBSSID() != null
                    && !"02:00:00:00:00:00".equals(info.getBSSID())) {
                String bssid = info.getBSSID();
                int rssi = info.getRssi();
                String ssid = info.getSSID();
                if (ssid != null) {
                    ssid = ssid.replace("\"", "");
                }
                Log.d(TAG, "Connected: " + bssid + " " + rssi + " " + ssid);
                return bssid + "," + rssi + "," + (ssid != null ? ssid : "");
            }
        } catch (Exception e) {
            Log.e(TAG, "getConnectedWifiMac error", e);
        }
        return null;
    }

    /**
     * 获取周边 WiFi 列表（排除移动热点），格式为 "mac,signal,ssid|mac,signal,ssid|..."
     * 至少需要 2 个才能定位
     */
    public String getNearbyWifiMacs() {
        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults == null || scanResults.isEmpty()) {
                wifiManager.startScan();
                scanResults = wifiManager.getScanResults();
            }

            if (scanResults == null || scanResults.isEmpty()) {
                Log.w(TAG, "No WiFi scan results");
                return null;
            }

            Collections.sort(scanResults, (a, b) -> b.level - a.level);

            WifiInfo connInfo = wifiManager.getConnectionInfo();
            String connBssid = connInfo != null ? connInfo.getBSSID() : null;

            List<String> macList = new ArrayList<>();
            for (ScanResult sr : scanResults) {
                if (sr.BSSID == null) continue;
                if (sr.BSSID.equals(connBssid)) continue;
                if (isMobileHotspot(sr)) continue;

                String ssid = sr.SSID != null ? sr.SSID : "";
                macList.add(sr.BSSID + "," + sr.level + "," + ssid);

                if (macList.size() >= 20) break;
            }

            Log.d(TAG, "Nearby WiFi count: " + macList.size());
            if (macList.size() < 2) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < macList.size(); i++) {
                if (i > 0) sb.append("|");
                sb.append(macList.get(i));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "getNearbyWifiMacs error", e);
            return null;
        }
    }

    private boolean isMobileHotspot(ScanResult sr) {
        if (sr.SSID == null || sr.SSID.isEmpty()) return false;
        String ssid = sr.SSID.toLowerCase();
        return ssid.contains("android") || ssid.contains("iphone")
                || ssid.contains("的iphone") || ssid.contains("手机")
                || ssid.contains("hotspot");
    }

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }
}
