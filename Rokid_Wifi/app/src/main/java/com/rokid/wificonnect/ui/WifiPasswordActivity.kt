package com.rokid.wificonnect.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rokid.wificonnect.R
import com.rokid.wificonnect.databinding.ActivityWifiPasswordBinding
import com.rokid.wificonnect.service.VoiceRecognizer
import com.rokid.wificonnect.service.WifiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WifiPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WifiPassword"
    }

    private lateinit var binding: ActivityWifiPasswordBinding
    private lateinit var wifiHelper: WifiHelper
    private var voiceRecognizer: VoiceRecognizer? = null
    private var isModelReady = false
    private var isListening = false
    private var isConnecting = false

    private var password = StringBuilder()
    private var ssid = ""
    private val handler = Handler(Looper.getMainLooper())

    private val keys = listOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
        "🎤", "⌫", "连接"
    )

    private var selectedKeyIndex = 0
    private val keyViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(6815872)
        binding = ActivityWifiPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiHelper = WifiHelper(this)
        ssid = intent.getStringExtra(WifiListActivity.EXTRA_SSID) ?: ""
        binding.tvSsid.text = ssid

        buildKeyboard()
        updatePasswordDisplay()
        updateKeySelection()

        // 禁止 HorizontalScrollView 拦截 DPAD 按键，全部由 Activity 处理
        binding.keyboardScroll.isFocusable = false
        binding.keyboardScroll.isFocusableInTouchMode = false
        binding.keyboardScroll.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS

        initVoice()
    }

    private fun buildKeyboard() {
        val container = binding.keyboardContainer
        container.removeAllViews()
        keyViews.clear()

        for ((index, key) in keys.withIndex()) {
            val tv = TextView(this).apply {
                text = key
                textSize = when {
                    key == "连接" -> 9f
                    key == "⌫" || key == "🎤" -> 11f
                    else -> 12f
                }
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)

                val bgColor = when (key) {
                    "⌫" -> "#CC4444"
                    "🎤" -> "#4488CC"
                    "连接" -> "#44AA44"
                    else -> "#333333"
                }
                background = GradientDrawable().apply {
                    cornerRadius = 8f
                    setColor(Color.parseColor(bgColor))
                }

                val size = if (key == "连接") 70 else 48
                layoutParams = LinearLayout.LayoutParams(size, 48).apply {
                    marginEnd = 6
                }
                setPadding(4, 4, 4, 4)
            }
            container.addView(tv)
            keyViews.add(tv)
        }
    }

    private fun updateKeySelection() {
        for ((i, tv) in keyViews.withIndex()) {
            val key = keys[i]
            val isSelected = i == selectedKeyIndex

            val bgColor = when {
                isSelected -> when (key) {
                    "⌫" -> "#FF6666"
                    "🎤" -> "#66AAEE"
                    "连接" -> "#66CC66"
                    else -> "#00FF88"
                }
                else -> when (key) {
                    "⌫" -> "#CC4444"
                    "🎤" -> "#4488CC"
                    "连接" -> "#44AA44"
                    else -> "#333333"
                }
            }
            val textColor = if (isSelected && key != "⌫" && key != "🎤" && key != "连接")
                Color.BLACK else Color.WHITE

            (tv.background as? GradientDrawable)?.setColor(Color.parseColor(bgColor))
            tv.setTextColor(textColor)

            if (isSelected) {
                tv.scaleX = 1.3f
                tv.scaleY = 1.3f
            } else {
                tv.scaleX = 1.0f
                tv.scaleY = 1.0f
            }
        }

        // 滚动到选中的按键
        val selectedView = keyViews.getOrNull(selectedKeyIndex) ?: return
        binding.keyboardScroll.post {
            val scrollX = selectedView.left - binding.keyboardScroll.width / 2 + selectedView.width / 2
            binding.keyboardScroll.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }

        binding.tvStatus.text = "当前: 「${keys[selectedKeyIndex]}」"
    }

    private fun updatePasswordDisplay() {
        val display = if (password.isEmpty()) "（请输入密码）" else password.toString()
        binding.tvPassword.text = display

        val color = if (password.isEmpty())
            resources.getColor(R.color.green_normal_40, null)
        else
            resources.getColor(R.color.green_normal, null)
        binding.tvPassword.setTextColor(color)
    }

    private fun onKeySelected() {
        val key = keys[selectedKeyIndex]
        when (key) {
            "⌫" -> {
                if (password.isNotEmpty()) {
                    password.deleteCharAt(password.length - 1)
                    updatePasswordDisplay()
                }
            }
            "🎤" -> {
                startListening()
            }
            "连接" -> {
                connectWifi()
            }
            else -> {
                password.append(key)
                updatePasswordDisplay()
            }
        }
    }

    // === 语音相关 ===

    private fun initVoice() {
        lifecycleScope.launch(Dispatchers.IO) {
            val vr = VoiceRecognizer(assets)
            val ok = vr.init()
            withContext(Dispatchers.Main) {
                if (ok) {
                    voiceRecognizer = vr
                    isModelReady = true
                    Log.d(TAG, "Voice model ready")
                }
            }
        }
    }

    private fun startListening() {
        if (isListening || isConnecting || !isModelReady) {
            if (!isModelReady) {
                binding.tvStatus.text = "语音模型加载中…"
            }
            return
        }
        isListening = true

        voiceRecognizer?.stop()
        voiceRecognizer?.start(object : VoiceRecognizer.Listener {
            override fun onPartial(text: String) {
                runOnUiThread {
                    binding.tvStatus.text = "🎤 识别中: $text"
                }
            }

            override fun onResult(text: String) {
                runOnUiThread {
                    isListening = false
                    val cleaned = cleanVoicePassword(text)
                    if (cleaned.isNotEmpty()) {
                        password.append(cleaned)
                        updatePasswordDisplay()
                        binding.tvStatus.text = "已追加: $cleaned"
                    } else {
                        binding.tvStatus.text = "未识别到内容"
                    }
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    isListening = false
                    binding.tvStatus.text = message
                }
            }
        })
        binding.tvStatus.text = "🎤 正在聆听…说出密码"
    }

    private fun stopListening() {
        isListening = false
        voiceRecognizer?.stop()
    }

    private fun cleanVoicePassword(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                '零', '〇' -> sb.append('0')
                '一', '幺' -> sb.append('1')
                '二', '两' -> sb.append('2')
                '三' -> sb.append('3')
                '四' -> sb.append('4')
                '五' -> sb.append('5')
                '六' -> sb.append('6')
                '七' -> sb.append('7')
                '八' -> sb.append('8')
                '九' -> sb.append('9')
                ' ', '，', '。', '！', '？', '、' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // === WiFi 连接 ===

    private fun connectWifi() {
        if (isConnecting || password.isEmpty()) {
            if (password.isEmpty()) binding.tvStatus.text = "请先输入密码"
            return
        }
        isConnecting = true
        stopListening()

        binding.tvStatus.text = "正在连接 $ssid…"
        binding.loading.visibility = View.VISIBLE

        wifiHelper.connectWpa(ssid, password.toString(), object : WifiHelper.ConnectCallback {
            override fun onSuccess() {
                runOnUiThread {
                    isConnecting = false
                    binding.loading.visibility = View.GONE
                    binding.tvStatus.text = "✓ 已连接 $ssid"
                    binding.tvStatus.setTextColor(resources.getColor(R.color.connected, null))
                    handler.postDelayed({ finish() }, 2000)
                }
            }

            override fun onFailed(reason: String) {
                runOnUiThread {
                    isConnecting = false
                    binding.loading.visibility = View.GONE
                    binding.tvStatus.text = "连接失败: $reason"
                }
            }
        })
    }

    // === 按键处理 ===

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isConnecting) return true

        // 只响应初次按下，过滤掉手势持续触发的 repeat 事件
        if (event?.repeatCount != 0) return true

        if (isListening) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                stopListening()
                binding.tvStatus.text = "当前: 「${keys[selectedKeyIndex]}」"
                return true
            }
            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (selectedKeyIndex < keys.size - 1) {
                    selectedKeyIndex++
                } else {
                    selectedKeyIndex = 0
                }
                updateKeySelection()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedKeyIndex > 0) {
                    selectedKeyIndex--
                    updateKeySelection()
                } else {
                    finish()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                onKeySelected()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stopListening()
        voiceRecognizer?.release()
        wifiHelper.destroy()
        super.onDestroy()
    }
}
