package com.ffalcon.vision.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ffalcon.mercury.android.sdk.touch.TempleAction
import com.ffalcon.mercury.android.sdk.touch.TempleActionViewModel
import com.ffalcon.mercury.android.sdk.ui.activity.BaseMirrorActivity
import com.ffalcon.mercury.android.sdk.util.FLogger
import com.ffalcon.vision.scanner.databinding.ActivityCameraBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


class CameraActivity:BaseMirrorActivity<ActivityCameraBinding>() {

    private val surfaceList = mutableListOf<Surface>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backHandlerThread.start()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                templeActionViewModel.state.collect {
                    when (it) {
                        is TempleAction.Click -> {
                            takePhoto.set(true)
                        }
                        is TempleAction.DoubleClick->{
                            finish()
                        }
                        else->{

                        }
                    }
                }
            }
        }

            mBindingPair.updateView {
            this.cameraPreview.surfaceTextureListener = object :SurfaceTextureListener{
                var mSurface :Surface?= null
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    val surface2 = Surface(cameraPreview.surfaceTexture)
                    surfaceList.add(surface2)
                    mSurface = surface2
                    if( surfaceList.size == 2) {
                        lifecycleScope.launch {
                            delay(100L)
                            setupCamera2()
                        }
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    mSurface?.release()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                }

            }
        }


    }

    override fun onStop() {
        super.onStop()
        closeCamera()
    }

    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraManager: CameraManager
    private val atomicBoolean = AtomicBoolean(false)
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var backHandler: Handler
    private var imageReader: ImageReader? = null
    private var cameraJob: Job? = null
    var takePhoto = AtomicBoolean(false)

    private val backHandlerThread = object : HandlerThread("background") {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            backHandler = Handler(this.looper)
        }
    }
    private val stateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(p0: CameraDevice) {
            cameraJob = lifecycleScope.launch {
                cameraDevice = p0
                delay(100L)
                if (cameraDevice == p0) {
                    setUpImageReader(p0)
                }
            }
        }

        override fun onDisconnected(p0: CameraDevice) {
            cameraDevice = null
            cameraJob?.cancel()
        }


        override fun onError(p0: CameraDevice, p1: Int) {
        }

    }

    @SuppressLint("MissingPermission")
    private fun setupCamera2() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first()

        cameraManager.openCamera(cameraId, stateCallback, null)

    }

    var openTime = -1L
    @RequiresApi(Build.VERSION_CODES.P)
    fun setUpImageReader(camera: CameraDevice) {
        imageReader?.close()
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 10)
        cameraDevice = camera
        openTime = -1L
        imageReader?.setOnImageAvailableListener({ reader ->

            if (openTime == -1L) {
                openTime = System.currentTimeMillis()
                return@setOnImageAvailableListener
            }
            if ((System.currentTimeMillis() - openTime) < 1000L) {
                return@setOnImageAvailableListener
            }
            val image = reader.acquireLatestImage() ?: run {
                return@setOnImageAvailableListener
            }

            if(takePhoto.get( ) ){
                takePhoto.set(false)
            val bitmap = imageToBitmap(image)
            bitmap?.let {
                runOnUiThread {
                    mBindingPair.updateView {
                        this.thumbnailView.setImageBitmap(it)
                    }
                }
                } ?: Log.e( "CameraActivity","Image convert to bitmap failed! ")
            }
            image.close()

        }, backHandler)

        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            .apply {
                addTarget(imageReader!!.surface)
                for( item in surfaceList ){
                    addTarget(item)
                }
                val fpsRange = Range(5, 10)
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)

            }
        val outputConfig = OutputConfiguration(imageReader!!.surface)
        val outputConfig2 = OutputConfiguration(surfaceList[0])
        val outputConfig3 = OutputConfiguration(surfaceList[1])
        val sessionConfig = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputConfig,outputConfig2,outputConfig3),
            Executors.newSingleThreadExecutor(),
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    cameraCaptureSession = session
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                }
            }
        )
        camera.createCaptureSession(sessionConfig)
    }

    private fun closeCamera() {
        try {
            if (null != cameraCaptureSession) {

                cameraCaptureSession!!.close()
                cameraCaptureSession = null
            }
            if (null != cameraDevice) {

                cameraDevice!!.close()
                cameraDevice = null
            }
            // 如果你使用了ImageReader，也应该在这里关闭它
            if (null != imageReader) {
                imageReader?.close()
                imageReader = null
            }
            atomicBoolean.set(false)
        } catch (e: Exception) {
        } finally {
            atomicBoolean.set(false)
        }
    }

    fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val ySize = buffer.remaining()

        val uBuffer: ByteBuffer = planes[1].buffer
        val uSize = uBuffer.remaining()

        val vBuffer: ByteBuffer = planes[2].buffer
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        buffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        out.close()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}