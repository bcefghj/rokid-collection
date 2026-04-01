package com.rokid.wificonnect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rokid.wificonnect.R
import com.rokid.wificonnect.databinding.ActivityWifiListBinding
import com.rokid.wificonnect.service.WifiHelper

class WifiListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WifiList"
        private const val REQ_PERMISSIONS = 100
        const val EXTRA_SSID = "ssid"
        const val EXTRA_IS_SECURED = "is_secured"
        const val EXTRA_CAPABILITIES = "capabilities"
    }

    private lateinit var binding: ActivityWifiListBinding
    private lateinit var wifiHelper: WifiHelper
    private var wifiItems = listOf<WifiHelper.WifiItem>()
    private var selectedIndex = 0
    private val itemViews = mutableListOf<View>()
    private var backPressedTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var resumeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(6815872)
        binding = ActivityWifiListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate")

        wifiHelper = WifiHelper(this)

        // 禁止 ScrollView 拦截 DPAD 按键，全部由 Activity 处理
        binding.scrollWifi.isFocusable = false
        binding.scrollWifi.isFocusableInTouchMode = false
        binding.scrollWifi.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val perms = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.RECORD_AUDIO)

        if (perms.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $perms")
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQ_PERMISSIONS)
        } else {
            Log.d(TAG, "All permissions granted, starting scan")
            startScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            Log.d(TAG, "Permission result received, starting scan")
            startScan()
        }
    }

    private fun startScan() {
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "startScan called, wifi enabled=${wifiHelper.isWifiEnabled()}")
        wifiHelper.enableWifi()
        binding.loading.visibility = View.VISIBLE
        binding.tvStatus.text = "正在开启WiFi…"

        lifecycleScope.launch(Dispatchers.IO) {
            // 等待 WiFi 硬件就绪（最多 8 秒）
            val ready = wifiHelper.waitUntilEnabled()
            Log.d(TAG, "WiFi ready=$ready, enabled=${wifiHelper.isWifiEnabled()}")

            withContext(Dispatchers.Main) {
                if (!ready) {
                    binding.loading.visibility = View.GONE
                    binding.tvStatus.text = "WiFi开启失败，请手动开启后前滑重试"
                    return@withContext
                }
                binding.tvStatus.text = "正在扫描…"
            }

            if (!ready) return@launch

            // WiFi 就绪后稍等 1 秒让扫描器初始化
            Thread.sleep(1000)

            withContext(Dispatchers.Main) {
                Log.d(TAG, "Calling wifiHelper.scan, wifi enabled=${wifiHelper.isWifiEnabled()}")
                wifiHelper.scan(object : WifiHelper.ScanCallback {
                    override fun onResults(list: List<WifiHelper.WifiItem>) {
                        Log.d(TAG, "Scan results: ${list.size} networks")
                        for (item in list) {
                            Log.d(TAG, "  WiFi: ${item.ssid} level=${item.level} secured=${item.isSecured} connected=${item.isConnected}")
                        }
                        runOnUiThread {
                            binding.loading.visibility = View.GONE
                            wifiItems = list
                            if (list.isEmpty()) {
                                binding.tvStatus.text = "未找到WiFi网络\n\n前滑 刷新"
                            } else {
                                binding.tvStatus.text = "找到 ${list.size} 个网络"
                                selectedIndex = 0
                                renderList()
                            }
                        }
                    }
                })
            }
        }
    }

    private fun renderList() {
        binding.wifiContainer.removeAllViews()
        itemViews.clear()

        for ((index, item) in wifiItems.withIndex()) {
            val card = createWifiCard(item, index)
            binding.wifiContainer.addView(card)
            itemViews.add(card)
        }

        updateSelection()
    }

    private fun createWifiCard(item: WifiHelper.WifiItem, index: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 16, 20, 16)
            background = resources.getDrawable(R.drawable.bg_wifi_item, null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        val signalIcon = TextView(this).apply {
            text = wifiHelper.getSignalIcon(item.level)
            textSize = 8f
            val color = when (item.level) {
                3 -> resources.getColor(R.color.wifi_strong, null)
                2 -> resources.getColor(R.color.wifi_medium, null)
                else -> resources.getColor(R.color.wifi_weak, null)
            }
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 16 }
        }
        card.addView(signalIcon)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val ssidView = TextView(this).apply {
            text = item.ssid
            textSize = 11f
            setTextColor(resources.getColor(R.color.green_normal, null))
            setTypeface(null, Typeface.BOLD)
            setSingleLine(true)
        }
        textCol.addView(ssidView)

        val statusParts = mutableListOf<String>()
        if (item.isConnected) statusParts.add("已连接")
        if (item.isSecured) statusParts.add("🔒")
        if (statusParts.isNotEmpty()) {
            val subView = TextView(this).apply {
                text = statusParts.joinToString("  ")
                textSize = 8f
                val c = if (item.isConnected)
                    resources.getColor(R.color.connected, null)
                else
                    resources.getColor(R.color.green_normal_60, null)
                setTextColor(c)
            }
            textCol.addView(subView)
        }

        card.addView(textCol)
        return card
    }

    private fun updateSelection() {
        for ((i, view) in itemViews.withIndex()) {
            view.setBackgroundResource(
                if (i == selectedIndex) R.drawable.bg_wifi_selected else R.drawable.bg_wifi_item
            )
        }
        val selectedView = itemViews.getOrNull(selectedIndex) ?: return
        binding.scrollWifi.post {
            binding.scrollWifi.smoothScrollTo(0, selectedView.top - 20)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 只响应初次按下，过滤掉手势持续触发的 repeat 事件
        if (event?.repeatCount != 0) return true

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (wifiItems.isEmpty()) return true
                if (selectedIndex < wifiItems.size - 1) {
                    selectedIndex++
                    updateSelection()
                } else {
                    binding.tvStatus.text = "已是最后一个网络（共 ${wifiItems.size} 个）"
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (wifiItems.isEmpty()) {
                    handleBack()
                    return true
                }
                if (selectedIndex > 0) {
                    selectedIndex--
                    updateSelection()
                } else {
                    handleBack()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (wifiItems.isNotEmpty()) {
                    selectWifi(wifiItems[selectedIndex])
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                handleBack()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            startScan()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    private fun handleBack() {
        val now = SystemClock.elapsedRealtime()
        if (now - backPressedTime > 2000) {
            Toast.makeText(this, R.string.exit_confirm, Toast.LENGTH_SHORT).show()
            backPressedTime = now
        } else {
            finish()
        }
    }

    private fun selectWifi(item: WifiHelper.WifiItem) {
        if (item.isConnected) {
            Toast.makeText(this, "已连接到 ${item.ssid}", Toast.LENGTH_SHORT).show()
            return
        }

        if (!item.isSecured) {
            connectOpen(item.ssid)
            return
        }

        val intent = Intent(this, WifiPasswordActivity::class.java).apply {
            putExtra(EXTRA_SSID, item.ssid)
            putExtra(EXTRA_IS_SECURED, item.isSecured)
            putExtra(EXTRA_CAPABILITIES, item.capabilities)
        }
        startActivity(intent)
    }

    private fun connectOpen(ssid: String) {
        binding.tvStatus.text = "正在连接 $ssid…"
        binding.loading.visibility = View.VISIBLE

        wifiHelper.connectOpen(ssid, object : WifiHelper.ConnectCallback {
            override fun onSuccess() {
                runOnUiThread {
                    binding.loading.visibility = View.GONE
                    binding.tvStatus.text = "已连接 $ssid"
                    handler.postDelayed({ startScan() }, 2000)
                }
            }

            override fun onFailed(reason: String) {
                runOnUiThread {
                    binding.loading.visibility = View.GONE
                    binding.tvStatus.text = "连接失败: $reason"
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
        if (resumeCount > 1) {
            startScan()
        }
    }

    override fun onDestroy() {
        wifiHelper.destroy()
        super.onDestroy()
    }
}
