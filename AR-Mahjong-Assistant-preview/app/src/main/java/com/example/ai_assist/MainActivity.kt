package com.example.ai_assist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import java.util.Collections
import kotlin.math.abs
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ai_assist.databinding.ActivityMainBinding
import com.example.ai_assist.repository.ChatRepository
import com.example.ai_assist.service.GameApiService
import com.example.ai_assist.service.RayNeoDeviceManager
import com.example.ai_assist.viewmodel.ChatViewModel
import com.example.ai_assist.viewmodel.ChatViewModelFactory
import com.example.ai_assist.utils.RayNeoAudioRecorder
import com.ffalcon.mercury.android.sdk.touch.TempleAction
import com.ffalcon.mercury.android.sdk.ui.activity.BaseMirrorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseMirrorActivity<ActivityMainBinding>() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var cameraExecutor: ExecutorService

    // Camera2 variables
    private val surfaceList = mutableListOf<Surface>()
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var backHandler: Handler
    private lateinit var backHandlerThread: HandlerThread
    private var cameraJob: Job? = null
    private var previewSize: Size? = null
    private var currentPhotoFile: File? = null
    
    // Audio Recorder
    private var audioRecorder: RayNeoAudioRecorder? = null
    private var isRecordingAudio = false

    // Game State Management
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
        // BaseMirrorActivity handles setContentView

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        backHandlerThread = HandlerThread("background")
        backHandlerThread.start()
        backHandler = Handler(backHandlerThread.looper)

        setupDependencies()
        setupUI()
        observeViewModel()
        checkPermissions()
        
        // Initial State
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

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun setupDependencies() {
        // Network
        // Configured for local real device debugging
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(GameApiService::class.java)
        
        // Data & Device
        val repository = ChatRepository(apiService)
        val deviceManager = RayNeoDeviceManager(this)
        
        val factory = ChatViewModelFactory(repository, deviceManager)
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        // Initialize Audio Recorder
        audioRecorder = RayNeoAudioRecorder(this) { file ->
            viewModel.uploadAudio(file)
        }
    }

    private fun setupUI() {
        mBindingPair.updateView { 
            tvStatus.text = "已连接"
            tvStatus.setTextColor(getColor(R.color.neon_green))
            // Clear test data
            tvContentHand.text = ""
            tvContentSuggested.text = ""
            tvContentWaiting.text = ""

            // Setup scroll buttons
            btnScrollUp.setOnClickListener {
                // Scroll approximately one line (considering 2.5x tile size + spacing)
                val scrollAmount = (tvContentWaiting.textSize * 3).toInt()
                svContentWaiting.smoothScrollBy(0, -scrollAmount)
            }
            btnScrollDown.setOnClickListener {
                val scrollAmount = (tvContentWaiting.textSize * 3).toInt()
                svContentWaiting.smoothScrollBy(0, scrollAmount)
            }
        }
    }

    private fun updateGameState(newState: GameState) {
        // 如果从相机预览模式退出到其他模式（不包括照片预览），需要关闭相机
        if (currentState == GameState.CAMERA_PREVIEW && newState != GameState.CAMERA_PREVIEW && newState != GameState.PHOTO_REVIEW) {
            closeCamera()
        }
        // 如果从照片预览模式退出到其他模式，也确保关闭相机
        if (currentState == GameState.PHOTO_REVIEW && newState != GameState.PHOTO_REVIEW) {
            closeCamera()
        }

        currentState = newState
        mBindingPair.updateView {
            // Reset Visibilities
            tvInstructionIdle.visibility = View.GONE
            layoutInstructionGaming.visibility = View.GONE
            cardCameraPreview.visibility = View.GONE
            tvInstructionCameraPreview.visibility = View.GONE
            layoutPhotoReviewContainer.visibility = View.GONE

            when (newState) {
                GameState.IDLE -> {
                    tvInstructionIdle.visibility = View.VISIBLE
                    tvStatus.text = "已连接 - 等待开始"
                    // Clear tiles logic if needed
                }
                GameState.GAMING -> {
                    layoutInstructionGaming.visibility = View.VISIBLE
                    tvStatus.text = "对局中"
                }
                GameState.CAMERA_PREVIEW -> {
                    cardCameraPreview.visibility = View.VISIBLE
                    tvInstructionCameraPreview.visibility = View.VISIBLE
                    tvStatus.text = "拍照模式"
                    startCamera()
                }
                GameState.PHOTO_REVIEW -> {
                    layoutPhotoReviewContainer.visibility = View.VISIBLE
                    tvStatus.text = "确认照片"
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.mappedResult.collect { result ->
                result?.let {
                    mBindingPair.updateView {
                        tvContentHand.text = formatMahjongText(it.userHand.joinToString(" "))
                        tvContentSuggested.text = formatMahjongText(it.meldedTiles.joinToString(" "))
                        
                        // Process suggested play text to resize unicode characters
                        tvContentWaiting.text = formatMahjongText(it.suggestedPlay)
                    }
                }
            }
        }

        lifecycleScope.launch {
            templeActionViewModel.state.collect { action ->
                when (action) {
                    is TempleAction.Click -> {
                        if (currentState == GameState.CAMERA_PREVIEW) {
                            takePhoto()
                        } else if (currentState == GameState.GAMING) {
                            updateGameState(GameState.CAMERA_PREVIEW)
                        }
                    }
                    is TempleAction.DoubleClick -> handleDoubleClick()
                    is TempleAction.TripleClick -> handleTripleClick()
                    is TempleAction.SlideForward -> handleSwipeForward()
                    is TempleAction.SlideBackward -> handleSwipeBackward()
                    is TempleAction.SlideUpwards -> {
                        mBindingPair.updateView {
                            val scrollAmount = (tvContentWaiting.textSize * 3).toInt()
                            svContentWaiting.smoothScrollBy(0, scrollAmount)
                        }
                    }
                    is TempleAction.SlideDownwards -> {
                        mBindingPair.updateView {
                            val scrollAmount = (tvContentWaiting.textSize * 3).toInt()
                            svContentWaiting.smoothScrollBy(0, -scrollAmount)
                        }
                    }
                    else -> {
                        Log.d("TempleAction", "Received: $action")
                    }
                }
            }
        }
    }

    private fun formatMahjongText(originalText: String): SpannableString {
        val spannable = SpannableString(originalText)

        // Load custom font if enabled
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

        // Reduce scale factor since the bitmap font is quite large
        val scaleFactor = if (AppConfig.USE_COLOR_FONT) AppConfig.FONT_SCALE_COLOR else AppConfig.FONT_SCALE_DEFAULT

        // Find all mahjong unicode characters (range U+1F000 to U+1F02B)
        var index = 0
        while (index < originalText.length) {
            val codePoint = originalText.codePointAt(index)
            val charCount = Character.charCount(codePoint)

            // Check if it's a Mahjong tile character (U+1F000 - U+1F02B)
            if (codePoint in 0x1F000..0x1F02B) {
                spannable.setSpan(
                    RelativeSizeSpan(scaleFactor),
                    index,
                    index + charCount,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Apply custom font if available
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

    private fun handleDoubleClick() {
        onBackPressedDispatcher.onBackPressed()
    }

    private fun handleTripleClick() {
        when (currentState) {
            GameState.IDLE -> {
                // Start New Session
                lifecycleScope.launch {
                    viewModel.startNewSession()
                    clearGameData() // Clear data when starting a new game
                    updateGameState(GameState.GAMING)
                }
            }
            GameState.GAMING -> {
                // End Session
                viewModel.endCurrentSession()
                updateGameState(GameState.IDLE)
                clearGameData() // Clear data when ending a game
            }
            else -> {}
        }
    }

    private fun handleSwipeForward() {
        if (currentState == GameState.GAMING) {
            if (isRecordingAudio) {
                audioRecorder?.stop()
                isRecordingAudio = false
                showCustomToast("录音停止")
            } else {
                audioRecorder?.start()
                isRecordingAudio = true
                showCustomToast("录音开始")
            }
        } else if (currentState == GameState.PHOTO_REVIEW) {
            // Cancel Photo
            showCustomToast("取消拍照")
            updateGameState(GameState.GAMING)
        } else if (currentState == GameState.CAMERA_PREVIEW) {
            // Cancel Camera Preview
            updateGameState(GameState.GAMING)
        }
    }

    private fun handleSwipeBackward() {
        if (currentState == GameState.PHOTO_REVIEW) {
            // Send Photo
            currentPhotoFile?.let { file ->
                showCustomToast("正在分析手牌...")
                viewModel.uploadPhoto(file)
            }
            updateGameState(GameState.GAMING)
        }
    }

    private fun clearGameData() {
        mBindingPair.updateView {
            tvContentHand.text = ""
            tvContentSuggested.text = ""
            tvContentWaiting.text = ""
        }
    }

    // Camera2 Implementation
    private fun startCamera() {
        surfaceList.clear()
        mBindingPair.updateView {
            // 定义处理逻辑，避免重复代码
            fun onSurfaceReady(surface: SurfaceTexture, width: Int, height: Int) {
                configureTransform(width, height)
                val s = Surface(surface)
                // 避免重复添加同一个 Surface
                if (!surfaceList.contains(s)) {
                    surfaceList.add(s)
                }
                
                // 当左右眼两个 Surface 都准备好时启动相机
                if (surfaceList.size == 2) {
                    lifecycleScope.launch {
                        delay(100L)
                        setupCamera2()
                    }
                }
            }

            viewCameraPreview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    onSurfaceReady(surface, width, height)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    configureTransform(width, height)
                }
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }

            // [修复关键点]：如果 TextureView 已经可用（如从后台恢复时），
            // setSurfaceTextureListener 不会触发回调，必须手动检查并处理。
            if (viewCameraPreview.isAvailable) {
                viewCameraPreview.surfaceTexture?.let { 
                    onSurfaceReady(it, viewCameraPreview.width, viewCameraPreview.height)
                }
            }
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        mBindingPair.updateView {
            val textureView = viewCameraPreview
            val matrix = Matrix()
            
            // Use dynamically selected preview size or fallback to 900x1200 (3:4)
            val pWidth = previewSize?.width?.toFloat() ?: 900f
            val pHeight = previewSize?.height?.toFloat() ?: 1200f
            
            val centerX = viewWidth / 2f
            val centerY = viewHeight / 2f
            
            // Calculate scaling to FIT HEIGHT while maintaining aspect ratio
            // The texture is initially stretched to fit viewWidth x viewHeight
            // We want to un-stretch it and make it fill height
            
            val videoAspect = pWidth / pHeight
            val viewAspect = viewWidth.toFloat() / viewHeight
            
            // Target dimensions to fit height:
            // height = viewHeight
            // width = viewHeight * videoAspect
            
            // Calculate scale factors relative to the current stretched view
            // scaleY = targetHeight / viewHeight = 1.0f
            // scaleX = targetWidth / viewWidth = (viewHeight * videoAspect) / viewWidth
            
            val scaleX = (viewHeight * videoAspect) / viewWidth
            val scaleY = 1f
            
            matrix.setScale(scaleX, scaleY, centerX, centerY)
            textureView.setTransform(matrix)
        }
    }

    private fun setupCamera2() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.first() // Usually back camera
            
            // Calculate optimal preview size
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            // Get TextureView size (assumed from first surface in list if available, or screen size)
            // For simplicity, we use a target max width/height or current TextureView size if available
            var targetWidth = 900
            var targetHeight = 1200
            
            // We can try to access the current view size from binding if needed, but it might be 0 if not laid out.
            // However, configureTransform passes actual width/height.
            // Here we just want to choose a camera resolution that matches the ASPECT RATIO of the screen/view ideally,
            // or just the largest available one that fits.
            // Let's pick a standard 16:9 or 4:3 resolution closest to 1080p.
            
            if (map != null) {
                 val supportedSizes = map.getOutputSizes(SurfaceTexture::class.java).toList()
                 previewSize = chooseOptimalSize(supportedSizes.toTypedArray(), 900, 1200)
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
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = ArrayList<Size>()
        
        // We try to find a size that matches the aspect ratio of the texture view if possible, 
        // but since texture view might be stretched/full screen, we prioritize standard aspect ratios like 16:9 or 4:3
        // For AR glasses, usually 16:9 (1920x1080) is preferred.
        
        for (option in choices) {
            // Check for 16:9 aspect ratio roughly (1920/1080 = 1.777)
            // or just pick the one closest to textureViewWidth x textureViewHeight
            // Here we simply collect all
            if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the largest of those not big enough.
        if (bigEnough.size > 0) {
            return Collections.min(bigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        } else if (notBigEnough.size > 0) {
            return Collections.max(notBigEnough) { lhs, rhs ->
                java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
            }
        } else {
            Log.e("Camera2", "Couldn't find any suitable preview size")
            return choices[0]
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
        // Use dynamically selected preview size or fallback to 900x1200 (3:4)
        val pWidth = previewSize?.width ?: 900
        val pHeight = previewSize?.height ?: 1200
        
        // OPTIMIZATION: Use JPEG format for hardware encoding and direct saving
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

                    // Decode bitmap
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    // Rotate bitmap (90 degrees as per capture request)
                    val matrix = Matrix()
                    matrix.postRotate(90f)
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    
                    // Crop bottom 50% height
                    // Top: 50%, Height: 50%
                    val cropY = (rotatedBitmap.height * 0.50).toInt()
                    val cropHeight = (rotatedBitmap.height * 0.50).toInt()
                    
                    // Ensure bounds are valid
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
                    
                    // Recycle bitmaps to save memory
                    bitmap.recycle()
                    rotatedBitmap.recycle()
                    // croppedBitmap will be recycled by GC
                    
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
                        try { image.close() } catch (e: Exception) {}
                    }
                }
            }
        }, backHandler)

        try {
            // OPTIMIZATION: Do NOT add imageReader surface to preview request
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                for (surface in surfaceList) {
                    addTarget(surface)
                }
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(15, 30))
            }

            val outputConfigs = mutableListOf<OutputConfiguration>()
            imageReader?.let { outputConfigs.add(OutputConfiguration(it.surface)) }
            for (surface in surfaceList) {
                outputConfigs.add(OutputConfiguration(surface))
            }

            // Using SessionConfiguration for Android P+ (API 28+)
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

            // Create a still capture request
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            
            // Also add preview surface to prevent flicker (optional but recommended)
            for (surface in surfaceList) {
                captureBuilder.addTarget(surface)
            }

            // Use the same AE/AF modes as preview
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            
            // Hardware rotation
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
            previewSize = null // 重置 previewSize 以确保下次启动时的行为一致性
            
            for (surface in surfaceList) {
                surface.release()
            }
            surfaceList.clear()
        } catch (e: Exception) {
            Log.e("Camera2", "Close camera failed", e)
        }
    }
    
    private fun showPhotoReview(uri: Uri) {
        updateGameState(GameState.PHOTO_REVIEW)
        mBindingPair.updateView {
            layoutPhotoReviewContainer.visibility = View.VISIBLE
            imagePhotoReview.setImageURI(uri)
        }
    }

    private fun showCustomToast(message: String) {
        mBindingPair.updateView {
            tvCustomToast.text = message
            tvCustomToast.visibility = View.VISIBLE
            tvCustomToast.postDelayed({
                tvCustomToast.visibility = View.GONE
            }, 2000)
        }
    }
}