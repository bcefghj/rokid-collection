package com.rokid.transit.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.rokid.transit.R
import com.rokid.transit.databinding.ActivityTransitMainBinding
import com.rokid.transit.service.AmapTransitService
import kotlinx.coroutines.launch
import com.rokid.transit.service.VoiceRecognizer

class TransitMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransitMain"
        private const val REQ_PERMISSIONS = 100
    }

    private lateinit var binding: ActivityTransitMainBinding
    private val transitService = AmapTransitService()
    private var originLonLat: String? = null
    private var originCity: String = ""
    private var backPressedTime: Long = 0

    private var voiceRecognizer: VoiceRecognizer? = null
    private var isModelReady = false
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(6815872)
        binding = ActivityTransitMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dest = intent.getStringExtra("destination")
        val originName = intent.getStringExtra("origin_name")
        if (dest != null && originName != null) {
            processFromTo(originName, dest)
        } else if (dest != null) {
            originLonLat = intent.getStringExtra("origin")
            originCity = intent.getStringExtra("city") ?: ""
            handleDestination(dest)
        } else {
            checkPermissionsAndStart()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.getStringExtra("destination")?.let { dest ->
            stopListening()
            originLonLat = intent.getStringExtra("origin") ?: originLonLat
            originCity = intent.getStringExtra("city") ?: originCity
            handleDestination(dest)
        }
    }

    private fun checkPermissionsAndStart() {
        val perms = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.RECORD_AUDIO)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQ_PERMISSIONS)
        } else {
            initVoiceAndLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            initVoiceAndLocation()
        }
    }

    private fun initVoiceAndLocation() {
        getLocation()
        showLoading("正在加载语音模型…")

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val vr = VoiceRecognizer(assets)
            val ok = vr.init()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (ok) {
                    voiceRecognizer = vr
                    isModelReady = true
                    showStatus("请说出路线\n例如：\"从西单到中关村\"\n或：\"从武汉站到光谷\"\n\n前滑 开始说话")
                } else {
                    showStatus("语音模型加载失败")
                }
            }
        }
    }

    private fun startListening() {
        if (isSearching || !isModelReady) return

        voiceRecognizer?.stop()
        voiceRecognizer?.start(object : VoiceRecognizer.Listener {
            override fun onPartial(text: String) {
                runOnUiThread { binding.statusText.text = "正在识别…\n\n「$text」" }
            }
            override fun onResult(text: String) {
                runOnUiThread { processVoiceResult(text) }
            }
            override fun onError(message: String) {
                runOnUiThread { showStatus("$message\n\n前滑 重试") }
            }
        })
        showStatus("正在聆听…\n\n请说出目的地")
    }

    private fun stopListening() {
        voiceRecognizer?.stop()
    }

    private fun processFromTo(origin: String, dest: String) {
        isSearching = true
        showLoading("$origin → $dest")
        lifecycleScope.launch {
            try {
                val originResult = transitService.geocodeFull(origin)
                if (originResult != null) {
                    originLonLat = originResult.location
                    if (originResult.city.isNotEmpty()) originCity = originResult.city
                }
            } catch (_: Exception) {}
            handleDestination(dest)
        }
    }

    private fun processVoiceResult(text: String) {
        stopListening()
        isSearching = true
        Log.d(TAG, "Voice: $text")

        val (origin, dest) = parseOriginAndDest(text)

        if (dest.isEmpty()) {
            isSearching = false
            showStatus("未识别到目的地\n你说的是：\"$text\"\n\n前滑 重新说")
            return
        }

        if (origin.isNotEmpty()) {
            showLoading("$origin → $dest")
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Geocoding origin: '$origin'")
                    val originResult = transitService.geocodeFull(origin)
                    Log.d(TAG, "Geocode result: ${originResult?.location}, city=${originResult?.city}")
                    if (originResult != null) {
                        originLonLat = originResult.location
                        if (originResult.city.isNotEmpty()) {
                            originCity = originResult.city
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocode origin failed: ${e.message}", e)
                }
                handleDestination(dest)
            }
        } else {
            handleDestination(dest)
        }
    }

    private fun parseOriginAndDest(text: String): Pair<String, String> {
        val cleaned = text.replace(" ", "")

        val biPatterns = listOf(
            Regex("从(.+?)到(.+)"),
            Regex("(.+?)到(.+)"),
        )
        for (p in biPatterns) {
            val match = p.find(cleaned)
            if (match != null && match.groupValues.size >= 3) {
                val o = match.groupValues[1]
                val d = match.groupValues[2]
                if (o.length in 2..15 && d.length in 2..15) {
                    return Pair(o, d)
                }
            }
        }

        val destPatterns = listOf(
            Regex("去(.+)"),
            Regex("地铁去(.+)"),
            Regex("到(.+)"),
        )
        for (p in destPatterns) {
            val match = p.find(cleaned)
            if (match != null && match.groupValues[1].length in 2..15) {
                return Pair("", match.groupValues[1])
            }
        }

        if (cleaned.length in 2..15) {
            return Pair("", cleaned)
        }

        return Pair("", "")
    }

    private fun handleDestination(dest: String) {
        Log.d(TAG, "handleDestination: dest=$dest, originLonLat=$originLonLat, originCity=$originCity")
        if (originLonLat != null) {
            startRouteSearch(dest)
        } else {
            showLoading("正在获取位置…")
            getLocation()
            lifecycleScope.launch {
                var tries = 0
                while (originLonLat == null && tries < 10) {
                    kotlinx.coroutines.delay(500)
                    tries++
                }
                if (originLonLat != null) {
                    startRouteSearch(dest)
                } else {
                    isSearching = false
                    showStatus("无法获取当前位置\n\n请说完整路线\n例如：\"从天安门到中关村\"\n\n前滑 重新说")
                }
            }
        }
    }

    private fun startRouteSearch(dest: String) {
        showLoading("正在查询路线…\n→ $dest")

        lifecycleScope.launch {
            try {
                val origin = originLonLat!!
                var destLonLat: String
                var destName: String
                var destCity = originCity

                val geoResult = transitService.geocodeFull(dest, originCity)
                if (geoResult != null) {
                    destLonLat = geoResult.location
                    destName = dest
                    if (geoResult.city.isNotEmpty()) destCity = geoResult.city
                } else {
                    val pois = transitService.searchPOI(dest, originCity)
                    if (pois.isNotEmpty()) {
                        destLonLat = pois[0].second
                        destName = pois[0].first.substringBefore(" (")
                    } else {
                        isSearching = false
                        showStatus("未找到\"$dest\"\n\n前滑 重新说")
                        return@launch
                    }
                }

                val city = originCity.ifEmpty { destCity }
                val plans = transitService.searchTransitRoute(origin, destLonLat, city, destCity)
                if (plans.isEmpty()) {
                    isSearching = false
                    showStatus("未找到地铁路线\n$dest\n\n前滑 重新说")
                    return@launch
                }

                TransitDataHolder.plans = plans
                TransitDataHolder.destName = destName
                isSearching = false
                startActivity(Intent(this@TransitMainActivity, TransitResultActivity::class.java))
                finish()
            } catch (e: Exception) {
                isSearching = false
                showStatus("查询失败\n${e.message}\n\n前滑 重新说")
            }
        }
    }

    private fun getLocation() {
        if (originLonLat != null) return
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

        if (last != null) {
            onLocationObtained(last)
            return
        }

        val providers = listOfNotNull(
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null,
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else null,
        )
        for (provider in providers) {
            lm.requestLocationUpdates(provider, 2000, 5f, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationObtained(location)
                    lm.removeUpdates(this)
                }
                @Deprecated("Deprecated") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            })
        }
    }

    private fun onLocationObtained(location: Location) {
        originLonLat = "${location.longitude},${location.latitude}"
        lifecycleScope.launch {
            try {
                val address = transitService.reverseGeocode(location.latitude, location.longitude)
                originCity = extractCity(address)
            } catch (_: Exception) {}
        }
    }

    private fun extractCity(address: String): String {
        val match = Regex("(.+?[市州])").find(address)
        return match?.groupValues?.get(1)?.replace(Regex("^.*?省"), "") ?: ""
    }

    private fun showLoading(text: String) {
        binding.loading.visibility = android.view.View.VISIBLE
        binding.statusText.text = text
    }

    private fun showStatus(text: String) {
        binding.loading.visibility = android.view.View.GONE
        binding.statusText.text = text
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!isSearching && isModelReady) {
                    stopListening()
                    startListening()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (!isSearching && isModelReady) {
                    stopListening()
                    startListening()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BACK -> {
                stopListening()
                val now = SystemClock.elapsedRealtime()
                if (now - backPressedTime > 2000) {
                    Toast.makeText(this, R.string.exit_confirm, Toast.LENGTH_SHORT).show()
                    backPressedTime = now
                    return true
                }
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        voiceRecognizer?.release()
        super.onDestroy()
    }
}
