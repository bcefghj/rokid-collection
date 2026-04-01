package com.rokid.mahjong

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.KeyEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rokid.mahjong.databinding.ActivityMainBinding
import com.rokid.mahjong.repository.ChatRepository
import com.rokid.mahjong.service.GameApiService
import com.rokid.mahjong.service.RokidDeviceManager
import com.rokid.mahjong.utils.RokidAudioRecorder
import com.rokid.mahjong.viewmodel.ChatViewModel
import com.rokid.mahjong.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var cameraExecutor: ExecutorService

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var backHandler: Handler
    private lateinit var backHandlerThread: HandlerThread
    private var cameraJob: Job? = null
    private var previewSize: Size? = null
    private var currentPhotoFile: File? = null

    private var audioRecorder: RokidAudioRecorder? = null
    private var isRecordingAudio = false

    enum class GameState { IDLE, GAMING, CAMERA_PREVIEW, PHOTO_REVIEW }
    private var currentState = GameState.IDLE

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                showCustomToast("需要相机和存储权限")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        backHandlerThread = HandlerThread("background")
        backHandlerThread.start()
        backHandler = Handler(backHandlerThread.looper)

        setupDependencies()
        setupUI()
        observeViewModel()
        checkPermissions()

        updateGameState(GameState.IDLE)
    }

    override fun onResume() {
        super.onResume()
        if (currentState == GameState.CAMERA_PREVIEW) {
            startCamera()
        }
    }

    override fun onPause() {
        closeCamera()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
        audioRecorder?.stop()
        cameraExecutor.shutdown()
        backHandlerThread.quitSafely()
    }

    /**
     * Rokid touchpad sends D-Pad key events.
     * DPAD_CENTER = confirm/tap
     * DPAD_UP/DOWN = scroll
     * DPAD_LEFT/RIGHT = swipe forward/backward
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                handleCenterTap()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                handleSwipeForward()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                handleSwipeBackward()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                binding.svContentWaiting.smoothScrollBy(0,
                    -(binding.tvContentWaiting.textSize * 3).toInt())
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                binding.svContentWaiting.smoothScrollBy(0,
                    (binding.tvContentWaiting.textSize * 3).toInt())
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                handleTripleClick()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                handleDoubleClick()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun setupDependencies() {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(GameApiService::class.java)

        val repository = ChatRepository(apiService)
        val deviceManager = RokidDeviceManager(this)

        val factory = ChatViewModelFactory(repository, deviceManager)
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        audioRecorder = RokidAudioRecorder(this) { file ->
            viewModel.uploadAudio(file)
        }
    }

    private fun setupUI() {
        binding.tvStatus.text = "已连接"
        binding.tvStatus.setTextColor(getColor(R.color.neon_green))
        binding.tvContentHand.text = ""
        binding.tvContentSuggested.text = ""
        binding.tvContentWaiting.text = ""

        binding.btnScrollUp.setOnClickListener {
            val scrollAmount = (binding.tvContentWaiting.textSize * 3).toInt()
            binding.svContentWaiting.smoothScrollBy(0, -scrollAmount)
        }
        binding.btnScrollDown.setOnClickListener {
            val scrollAmount = (binding.tvContentWaiting.textSize * 3).toInt()
            binding.svContentWaiting.smoothScrollBy(0, scrollAmount)
        }
    }

    private fun updateGameState(newState: GameState) {
        if (currentState == GameState.CAMERA_PREVIEW && newState != GameState.CAMERA_PREVIEW && newState != GameState.PHOTO_REVIEW) {
            closeCamera()
        }
        if (currentState == GameState.PHOTO_REVIEW && newState != GameState.PHOTO_REVIEW) {
            closeCamera()
        }

        currentState = newState

        binding.tvInstructionIdle.visibility = View.GONE
        binding.layoutInstructionGaming.visibility = View.GONE
        binding.cardCameraPreview.visibility = View.GONE
        binding.tvInstructionCameraPreview.visibility = View.GONE
        binding.layoutPhotoReviewContainer.visibility = View.GONE

        when (newState) {
            GameState.IDLE -> {
                binding.tvInstructionIdle.visibility = View.VISIBLE
                binding.tvStatus.text = "已连接 - 等待开始"
            }
            GameState.GAMING -> {
                binding.layoutInstructionGaming.visibility = View.VISIBLE
                binding.tvStatus.text = "对局中"
            }
            GameState.CAMERA_PREVIEW -> {
                binding.cardCameraPreview.visibility = View.VISIBLE
                binding.tvInstructionCameraPreview.visibility = View.VISIBLE
                binding.tvStatus.text = "拍照模式"
                startCamera()
            }
            GameState.PHOTO_REVIEW -> {
                binding.layoutPhotoReviewContainer.visibility = View.VISIBLE
                binding.tvStatus.text = "确认照片"
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.mappedResult.collect { result ->
                result?.let {
                    binding.tvContentHand.text = formatMahjongText(it.userHand.joinToString(" "))
                    binding.tvContentSuggested.text = formatMahjongText(it.meldedTiles.joinToString(" "))
                    binding.tvContentWaiting.text = formatMahjongText(it.suggestedPlay)
                }
            }
        }
    }

    private fun formatMahjongText(originalText: String): SpannableString {
        val spannable = SpannableString(originalText)

        val mahjongTypeface = if (AppConfig.USE_COLOR_FONT) {
            try {
                resources.getFont(R.font.mahjong_color)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load mahjong font", e)
                null
            }
        } else {
            null
        }

        val scaleFactor = if (AppConfig.USE_COLOR_FONT) AppConfig.FONT_SCALE_COLOR else AppConfig.FONT_SCALE_DEFAULT

        var index = 0
        while (index < originalText.length) {
            val codePoint = originalText.codePointAt(index)
            val charCount = Character.charCount(codePoint)

            if (codePoint in 0x1F000..0x1F02B) {
                spannable.setSpan(
                    RelativeSizeSpan(scaleFactor),
                    index,
                    index + charCount,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (mahjongTypeface != null) {
                    spannable.setSpan(
                        TypefaceSpan(mahjongTypeface),
                        index,
                        index + charCount,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            index += charCount
        }
        return spannable
    }

    private fun handleCenterTap() {
        when (currentState) {
            GameState.CAMERA_PREVIEW -> takePhoto()
            GameState.GAMING -> updateGameState(GameState.CAMERA_PREVIEW)
            GameState.PHOTO_REVIEW -> {
                currentPhotoFile?.let { file ->
                    showCustomToast("正在分析手牌...")
                    viewModel.uploadPhoto(file)
                }
                updateGameState(GameState.GAMING)
            }
            else -> {}
        }
    }

    private fun handleDoubleClick() {
        onBackPressedDispatcher.onBackPressed()
    }

    private fun handleTripleClick() {
        when (currentState) {
            GameState.IDLE -> {
                lifecycleScope.launch {
                    viewModel.startNewSession()
                    clearGameData()
                    updateGameState(GameState.GAMING)
                }
            }
            GameState.GAMING -> {
                viewModel.endCurrentSession()
                updateGameState(GameState.IDLE)
                clearGameData()
            }
            else -> {}
        }
    }

    private fun handleSwipeForward() {
        when (currentState) {
            GameState.GAMING -> {
                if (isRecordingAudio) {
                    audioRecorder?.stop()
                    isRecordingAudio = false
                    showCustomToast("录音停止")
                } else {
                    audioRecorder?.start()
                    isRecordingAudio = true
                    showCustomToast("录音开始")
                }
            }
            GameState.PHOTO_REVIEW, GameState.CAMERA_PREVIEW -> {
                showCustomToast("取消")
                updateGameState(GameState.GAMING)
            }
            else -> {}
        }
    }

    private fun handleSwipeBackward() {
        if (currentState == GameState.PHOTO_REVIEW) {
            currentPhotoFile?.let { file ->
                showCustomToast("正在分析手牌...")
                viewModel.uploadPhoto(file)
            }
            updateGameState(GameState.GAMING)
        }
    }

    private fun clearGameData() {
        binding.tvContentHand.text = ""
        binding.tvContentSuggested.text = ""
        binding.tvContentWaiting.text = ""
    }

    private fun startCamera() {
        binding.viewCameraPreview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                setupCamera2()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        if (binding.viewCameraPreview.isAvailable) {
            setupCamera2()
        }
    }

    private fun setupCamera2() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.first()

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (map != null) {
                val supportedSizes = map.getOutputSizes(SurfaceTexture::class.java).toList()
                previewSize = chooseOptimalSize(supportedSizes.toTypedArray(), 480, 640)
                Log.d("Camera2", "Selected preview size: ${previewSize?.width}x${previewSize?.height}")
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backHandler)
            }
        } catch (e: Exception) {
            Log.e("Camera2", "Failed to open camera", e)
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int, textureViewHeight: Int): Size {
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()

        for (option in choices) {
            if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }

        return if (bigEnough.size > 0) {
            Collections.min(bigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        } else if (notBigEnough.size > 0) {
            Collections.max(notBigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        } else {
            Log.e("Camera2", "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraJob = lifecycleScope.launch {
                cameraDevice = camera
                delay(100L)
                if (cameraDevice == camera) {
                    setUpImageReader(camera)
                }
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
            cameraJob?.cancel()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun setUpImageReader(camera: CameraDevice) {
        imageReader?.close()
        val pWidth = previewSize?.width ?: 480
        val pHeight = previewSize?.height ?: 640

        imageReader = ImageReader.newInstance(pWidth, pHeight, ImageFormat.JPEG, 2)

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            lifecycleScope.launch(Dispatchers.IO) {
                var imageClosed = false
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    imageClosed = true

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    val matrix = Matrix()
                    matrix.postRotate(90f)
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    val cropY = (rotatedBitmap.height * 0.50).toInt()
                    val cropHeight = (rotatedBitmap.height * 0.50).toInt()

                    val finalY = cropY.coerceIn(0, rotatedBitmap.height)
                    val finalHeight = cropHeight.coerceAtMost(rotatedBitmap.height - finalY)

                    val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, 0, finalY, rotatedBitmap.width, finalHeight)

                    val photoFile = File(
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                            .format(System.currentTimeMillis()) + ".jpg"
                    )

                    FileOutputStream(photoFile).use { fos ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }

                    bitmap.recycle()
                    rotatedBitmap.recycle()

                    currentPhotoFile = photoFile

                    val uri = Uri.fromFile(photoFile)
                    withContext(Dispatchers.Main) {
                        showPhotoReview(uri)
                    }
                } catch (e: Exception) {
                    Log.e("Camera2", "Save photo failed", e)
                    withContext(Dispatchers.Main) {
                        showCustomToast("保存照片失败: ${e.message}")
                    }
                } finally {
                    if (!imageClosed) {
                        try { image.close() } catch (_: Exception) {}
                    }
                }
            }
        }, backHandler)

        try {
            val previewSurface = Surface(binding.viewCameraPreview.surfaceTexture)

            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(15, 30))
            }

            val outputConfigs = mutableListOf<OutputConfiguration>()
            imageReader?.let { outputConfigs.add(OutputConfiguration(it.surface)) }
            outputConfigs.add(OutputConfiguration(previewSurface))

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                cameraExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            session.close()
                            return
                        }
                        cameraCaptureSession = session
                        try {
                            session.setRepeatingRequest(captureRequestBuilder.build(), null, backHandler)
                        } catch (e: Exception) {
                            Log.e("Camera2", "Capture request failed", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("Camera2", "Session configuration failed")
                    }
                }
            )
            camera.createCaptureSession(sessionConfig)

        } catch (e: Exception) {
            Log.e("Camera2", "Create capture session failed", e)
        }
    }

    private fun takePhoto() {
        try {
            val session = cameraCaptureSession ?: return
            val device = cameraDevice ?: return
            val reader = imageReader ?: return

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)

            val previewSurface = Surface(binding.viewCameraPreview.surfaceTexture)
            captureBuilder.addTarget(previewSurface)

            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)

            session.capture(captureBuilder.build(), null, backHandler)

        } catch (e: Exception) {
            Log.e("Camera2", "Take photo failed", e)
            showCustomToast("拍照失败")
        }
    }

    private fun closeCamera() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            previewSize = null
        } catch (e: Exception) {
            Log.e("Camera2", "Close camera failed", e)
        }
    }

    private fun showPhotoReview(uri: Uri) {
        updateGameState(GameState.PHOTO_REVIEW)
        binding.layoutPhotoReviewContainer.visibility = View.VISIBLE
        binding.imagePhotoReview.setImageURI(uri)
    }

    private fun showCustomToast(message: String) {
        binding.tvCustomToast.text = message
        binding.tvCustomToast.visibility = View.VISIBLE
        binding.tvCustomToast.postDelayed({
            binding.tvCustomToast.visibility = View.GONE
        }, 2000)
    }
}
