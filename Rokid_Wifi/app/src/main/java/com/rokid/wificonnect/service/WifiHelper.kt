package com.rokid.wificonnect.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log

class WifiHelper(private val context: Context) {

    companion object {
        private const val TAG = "WifiHelper"
    }

    data class WifiItem(
        val ssid: String,
        val level: Int,
        val isSecured: Boolean,
        val isConnected: Boolean,
        val capabilities: String
    )

    interface ScanCallback {
        fun onResults(list: List<WifiItem>)
    }

    interface ConnectCallback {
        fun onSuccess()
        fun onFailed(reason: String)
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var scanReceiver: BroadcastReceiver? = null

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    fun enableWifi() {
        if (!wifiManager.isWifiEnabled) {
            @Suppress("DEPRECATION")
            val ok = try { wifiManager.isWifiEnabled = true; true } catch (_: Exception) { false }
            if (!ok) {
                try {
                    Runtime.getRuntime().exec(arrayOf("svc", "wifi", "enable")).waitFor()
                    Log.d(TAG, "WiFi enabled via shell")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enable WiFi", e)
                }
            }
        }
    }

    /**
     * 等待 WiFi 硬件完全启用，最多等 maxWaitMs 毫秒，间隔 intervalMs 轮询一次。
     * 在子线程中调用（会阻塞）。
     */
    fun waitUntilEnabled(maxWaitMs: Long = 8000L, intervalMs: Long = 500L): Boolean {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            if (wifiManager.isWifiEnabled) return true
            Thread.sleep(intervalMs)
        }
        return wifiManager.isWifiEnabled
    }

    fun scan(callback: ScanCallback) {
        scanReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }

        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                try { context.unregisterReceiver(this) } catch (_: Exception) {}
                scanReceiver = null
                val results = getFilteredResults()
                callback.onResults(results)
            }
        }

        context.registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (!started) {
            val results = getFilteredResults()
            callback.onResults(results)
        }
    }

    private fun getFilteredResults(): List<WifiItem> {
        val currentSsid = getCurrentSsid()
        val results = wifiManager.scanResults ?: emptyList()
        return results
            .filter { it.SSID.isNotEmpty() }
            .distinctBy { it.SSID }
            .sortedByDescending { it.level }
            .map { sr ->
                WifiItem(
                    ssid = sr.SSID,
                    level = WifiManager.calculateSignalLevel(sr.level, 4),
                    isSecured = isSecured(sr),
                    isConnected = sr.SSID == currentSsid,
                    capabilities = sr.capabilities
                )
            }
    }

    private fun isSecured(sr: ScanResult): Boolean {
        return sr.capabilities.contains("WPA") ||
                sr.capabilities.contains("WEP") ||
                sr.capabilities.contains("PSK") ||
                sr.capabilities.contains("EAP")
    }

    fun getCurrentSsid(): String? {
        val info = wifiManager.connectionInfo ?: return null
        val ssid = info.ssid ?: return null
        return ssid.removePrefix("\"").removeSuffix("\"").let {
            if (it == "<unknown ssid>" || it.isEmpty()) null else it
        }
    }

    fun connectWpa(ssid: String, password: String, callback: ConnectCallback) {
        Log.d(TAG, "Connecting to $ssid")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectAndroid10(ssid, password, callback)
        } else {
            connectLegacy(ssid, password, callback)
        }
    }

    fun connectOpen(ssid: String, callback: ConnectCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectOpenAndroid10(ssid, callback)
        } else {
            connectOpenLegacy(ssid, callback)
        }
    }

    @Suppress("DEPRECATION")
    private fun connectLegacy(ssid: String, password: String, callback: ConnectCallback) {
        try {
            val config = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
            }

            val existing = wifiManager.configuredNetworks?.find { it.SSID == "\"$ssid\"" }
            if (existing != null) {
                wifiManager.removeNetwork(existing.networkId)
            }

            val netId = wifiManager.addNetwork(config)
            if (netId == -1) {
                callback.onFailed("添加网络失败")
                return
            }

            wifiManager.disconnect()
            val ok = wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

            if (ok) callback.onSuccess() else callback.onFailed("连接失败")
        } catch (e: Exception) {
            callback.onFailed(e.message ?: "未知错误")
        }
    }

    @Suppress("DEPRECATION")
    private fun connectOpenLegacy(ssid: String, callback: ConnectCallback) {
        try {
            val config = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }

            val existing = wifiManager.configuredNetworks?.find { it.SSID == "\"$ssid\"" }
            if (existing != null) {
                wifiManager.removeNetwork(existing.networkId)
            }

            val netId = wifiManager.addNetwork(config)
            if (netId == -1) {
                callback.onFailed("添加网络失败")
                return
            }

            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onFailed(e.message ?: "未知错误")
        }
    }

    private fun connectAndroid10(ssid: String, password: String, callback: ConnectCallback) {
        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    cm.bindProcessToNetwork(network)
                    Log.d(TAG, "Connected to $ssid via Android 10+ API")
                    callback.onSuccess()
                }

                override fun onUnavailable() {
                    Log.e(TAG, "Network unavailable")
                    callback.onFailed("连接失败或超时")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Connect error", e)
            // Fallback to legacy + shell command
            connectViaShell(ssid, password, callback)
        }
    }

    private fun connectOpenAndroid10(ssid: String, callback: ConnectCallback) {
        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    cm.bindProcessToNetwork(network)
                    callback.onSuccess()
                }
                override fun onUnavailable() {
                    callback.onFailed("连接失败")
                }
            })
        } catch (e: Exception) {
            callback.onFailed(e.message ?: "未知错误")
        }
    }

    private fun connectViaShell(ssid: String, password: String, callback: ConnectCallback) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "cmd", "wifi", "connect-network", ssid, "wpa2", password
            ))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                callback.onSuccess()
            } else {
                callback.onFailed("Shell连接失败 (code=$exitCode)")
            }
        } catch (e: Exception) {
            callback.onFailed("Shell命令失败: ${e.message}")
        }
    }

    fun getSignalIcon(level: Int): String {
        return when (level) {
            3 -> "▂▄▆█"
            2 -> "▂▄▆░"
            1 -> "▂▄░░"
            else -> "▂░░░"
        }
    }

    fun destroy() {
        scanReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
            scanReceiver = null
        }
    }
}
