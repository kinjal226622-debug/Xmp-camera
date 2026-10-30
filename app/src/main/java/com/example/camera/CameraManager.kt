package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.gl.XmpImageProcessor
import com.example.xmp.XmpPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val storageManager: MediaStorageManager
) {
    private val TAG = "CameraManager"
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingTimerJob: Job? = null

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _histogram = MutableStateFlow(HistogramData())
    val histogram: StateFlow<HistogramData> = _histogram.asStateFlow()

    var activePreset: XmpPreset = XmpPreset.DEFAULT
    var presetIntensity: Float = 1.0f

    fun startCamera(previewView: PreviewView, onStarted: () -> Unit = {}) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(previewView)
                onStarted()
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(previewView: PreviewView) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val lensFacing = if (_uiState.value.isFrontCamera) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(
                when (_uiState.value.flashMode) {
                    1 -> ImageCapture.FLASH_MODE_ON
                    2 -> ImageCapture.FLASH_MODE_OFF
                    else -> ImageCapture.FLASH_MODE_AUTO
                }
            )
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, HistogramAnalyzer { data ->
                    _histogram.value = data
                })
            }

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture,
                imageAnalysis
            )

            // Setup camera zoom & exposure bounds
            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                _uiState.value = _uiState.value.copy(
                    zoomRatio = zoomState.zoomRatio,
                    minZoom = zoomState.minZoomRatio,
                    maxZoom = zoomState.maxZoomRatio
                )
            }

            camera?.cameraInfo?.exposureState?.let { exposureState ->
                _uiState.value = _uiState.value.copy(
                    exposureCompensation = exposureState.exposureCompensationIndex,
                    minExposure = exposureState.exposureCompensationRange.lower,
                    maxExposure = exposureState.exposureCompensationRange.upper,
                    exposureStep = exposureState.exposureCompensationStep.toFloat()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Binding camera use cases failed", e)
        }
    }

    fun toggleCamera(previewView: PreviewView) {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
        bindCameraUseCases(previewView)
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setLinearZoom(fraction: Float) {
        camera?.cameraControl?.setLinearZoom(fraction.coerceIn(0f, 1f))
    }

    fun selectLens(lens: LensType) {
        _uiState.value = _uiState.value.copy(currentLens = lens)
        setZoom(lens.zoomFactor)
    }

    fun setExposure(index: Int) {
        val clamped = index.coerceIn(_uiState.value.minExposure, _uiState.value.maxExposure)
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
        _uiState.value = _uiState.value.copy(exposureCompensation = clamped)
    }

    fun cycleFlashMode() {
        val nextMode = (_uiState.value.flashMode + 1) % 3
        _uiState.value = _uiState.value.copy(flashMode = nextMode)
        imageCapture?.flashMode = when (nextMode) {
            1 -> ImageCapture.FLASH_MODE_ON
            2 -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    fun toggleTorch() {
        val nextTorch = !_uiState.value.isTorchOn
        camera?.cameraControl?.enableTorch(nextTorch)
        _uiState.value = _uiState.value.copy(isTorchOn = nextTorch)
    }

    fun tapToFocus(xNorm: Float, yNorm: Float, viewWidth: Int, viewHeight: Int) {
        val factory = SurfaceOrientedMeteringPointFactory(viewWidth.toFloat(), viewHeight.toFloat())
        val point = factory.createPoint(xNorm * viewWidth, yNorm * viewHeight)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)

        _uiState.value = _uiState.value.copy(focusPoint = Pair(xNorm, yNorm))
        triggerHaptic()

        // Hide focus ring after 3 seconds
        mainScope.launch {
            delay(3000)
            if (_uiState.value.focusPoint?.first == xNorm && _uiState.value.focusPoint?.second == yNorm) {
                _uiState.value = _uiState.value.copy(focusPoint = null)
            }
        }
    }

    fun capturePhoto(
        settings: CameraSettings,
        onSuccess: (Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (settings.timerSeconds > 0) {
            startCountdown(settings.timerSeconds) {
                executePhotoCapture(settings, onSuccess, onError)
            }
        } else {
            executePhotoCapture(settings, onSuccess, onError)
        }
    }

    private fun startCountdown(seconds: Int, onFinished: () -> Unit) {
        _uiState.value = _uiState.value.copy(isCountingDown = true, countdownRemaining = seconds)
        mainScope.launch {
            for (s in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(countdownRemaining = s)
                triggerHaptic()
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(isCountingDown = false, countdownRemaining = 0)
            onFinished()
        }
    }

    private fun executePhotoCapture(
        settings: CameraSettings,
        onSuccess: (Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: return onError("Camera capture not ready")
        triggerHaptic()

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    val rawBitmap = imageProxyToBitmap(image, rotation)
                    image.close()

                    if (rawBitmap != null) {
                        mainScope.launch {
                            // Save original if option selected
                            if (settings.saveOriginalAlso) {
                                storageManager.savePhoto(rawBitmap, "Original", isOriginal = true)
                            }

                            // Apply XMP Develop Preset Color Grading
                            val gradedBitmap = XmpImageProcessor.applyPreset(
                                srcBitmap = rawBitmap,
                                preset = activePreset,
                                intensity = presetIntensity,
                                addWatermark = settings.addWatermark
                            )

                            // Save graded photo to MediaStore
                            val savedUri = storageManager.savePhoto(
                                bitmap = gradedBitmap,
                                presetName = activePreset.name,
                                isOriginal = false
                            )
                            onSuccess(savedUri)
                        }
                    } else {
                        onError("Failed to decode camera frame")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Capture failed")
                }
            }
        )
    }

    fun startBurstCapture(
        settings: CameraSettings,
        onComplete: (Int) -> Unit
    ) {
        val total = settings.burstCount
        mainScope.launch {
            var captured = 0
            for (i in 1..total) {
                executePhotoCapture(settings, onSuccess = { captured++ }, onError = {})
                triggerHaptic()
                delay(300)
            }
            onComplete(captured)
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleVideoRecording(
        settings: CameraSettings,
        onSaved: (Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        val video = videoCapture ?: return onError("Video recorder not ready")

        if (activeRecording != null) {
            // Stop recording
            activeRecording?.stop()
            activeRecording = null
            recordingTimerJob?.cancel()
            _uiState.value = _uiState.value.copy(isRecordingVideo = false, recordingDurationSec = 0L)
            triggerHaptic()
            return
        }

        // Start new video recording
        val (outputUri, contentValues) = storageManager.createVideoContentValues(activePreset.name)
        if (outputUri == null) {
            onError("Unable to create MediaStore video entry")
            return
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            outputUri
        ).setContentValues(contentValues).build()

        val pending = video.output.prepareRecording(context, mediaStoreOutput)

        // Try enabling audio
        try {
            pending.withAudioEnabled()
        } catch (e: SecurityException) {
            Log.w(TAG, "Audio recording permission missing; recording without audio")
        }

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _uiState.value = _uiState.value.copy(isRecordingVideo = true, recordingDurationSec = 0L)
                    triggerHaptic()
                    recordingTimerJob = mainScope.launch {
                        while (_uiState.value.isRecordingVideo) {
                            delay(1000)
                            _uiState.value = _uiState.value.copy(
                                recordingDurationSec = _uiState.value.recordingDurationSec + 1
                            )
                        }
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    recordingTimerJob?.cancel()
                    _uiState.value = _uiState.value.copy(isRecordingVideo = false, recordingDurationSec = 0L)
                    if (event.hasError()) {
                        onError("Video error: ${event.cause?.message}")
                    } else {
                        onSaved(event.outputResults.outputUri)
                    }
                }
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap? {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun triggerHaptic() {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        activeRecording?.stop()
        recordingTimerJob?.cancel()
    }
}
