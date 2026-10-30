package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraManager
import com.example.camera.CameraSettings
import com.example.camera.GridType
import com.example.camera.LensType
import com.example.camera.LevelAngles
import com.example.camera.LevelSensorManager
import com.example.camera.MediaStorageManager
import com.example.data.PresetRepository
import com.example.ui.components.BottomShutterBar
import com.example.ui.components.CameraMode
import com.example.ui.components.CountdownOverlay
import com.example.ui.components.FocusRingOverlay
import com.example.ui.components.GridOverlay
import com.example.ui.components.HistogramOverlay
import com.example.ui.components.LevelIndicatorOverlay
import com.example.ui.components.ManualControlsBar
import com.example.ui.components.PresetInspectorDialog
import com.example.ui.components.PresetSelectorBar
import com.example.ui.components.TopHudBar
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.xmp.XmpPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun CameraScreen(
    presetRepository: PresetRepository,
    storageManager: MediaStorageManager,
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionFallback(onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        })
        return
    }

    // Camera and Presets Setup
    val cameraManager = remember { CameraManager(context, lifecycleOwner, storageManager) }
    val cameraUiState by cameraManager.uiState.collectAsState()
    val histogramData by cameraManager.histogram.collectAsState()
    val allPresets by presetRepository.getAllPresets().collectAsState(initial = emptyList())

    var selectedPreset by remember { mutableStateOf(XmpPreset.DEFAULT) }
    var presetIntensity by remember { mutableFloatStateOf(1.0f) }
    var cameraMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var inspectedPreset by remember { mutableStateOf<XmpPreset?>(null) }
    var isCompareMode by remember { mutableStateOf(false) }
    var latestCapturedUri by remember { mutableStateOf<Uri?>(null) }
    var levelAngles by remember { mutableStateOf(LevelAngles()) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Start Level Sensor
    DisposableEffect(Unit) {
        val levelSensor = LevelSensorManager(context) { angles ->
            levelAngles = angles
        }
        levelSensor.start()
        onDispose {
            levelSensor.stop()
            cameraManager.release()
        }
    }

    // Keep cameraManager activePreset in sync
    LaunchedEffect(selectedPreset, presetIntensity) {
        cameraManager.activePreset = selectedPreset
        cameraManager.presetIntensity = presetIntensity
    }

    // System file picker to import .xmp files from device storage
    val xmpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var importedCount = 0
                for (uri in uris) {
                    try {
                        val fileName = getFileName(context, uri) ?: "Preset_${System.currentTimeMillis()}.xmp"
                        val content = readTextFromUri(context, uri)
                        if (content.isNotBlank()) {
                            val preset = presetRepository.importXmp(content, fileName)
                            selectedPreset = preset
                            importedCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (importedCount > 0) {
                    Toast.makeText(context, "Successfully imported $importedCount XMP preset(s)!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not parse selected file(s)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val viewWidthPx = constraints.maxWidth
        val viewHeightPx = constraints.maxHeight

        // Pinch-to-zoom state
        val zoomTransformState = rememberTransformableState { zoomChange, _, _ ->
            val newZoom = (cameraUiState.zoomRatio * zoomChange).coerceIn(
                cameraUiState.minZoom,
                cameraUiState.maxZoom
            )
            cameraManager.setZoom(newZoom)
        }

        // Live Viewfinder with real-time Color Grading Matrix Filter
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = zoomTransformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val xNorm = offset.x / viewWidthPx
                            val yNorm = offset.y / viewHeightPx
                            cameraManager.tapToFocus(xNorm, yNorm, viewWidthPx, viewHeightPx)
                        }
                    )
                }
                .drawWithContent {
                    drawContent()

                    // Real-time live color grading simulation over viewfinder
                    if (selectedPreset.id != XmpPreset.DEFAULT.id && !isCompareMode) {
                        val matrix = buildXmpColorMatrix(selectedPreset, presetIntensity)
                        drawRect(
                            color = Color.Transparent,
                            colorFilter = ColorFilter.colorMatrix(matrix)
                        )
                    }

                    // Before/After Split Line when Compare Mode is enabled
                    if (isCompareMode) {
                        val splitX = size.width / 2f
                        drawLine(
                            color = Color.White,
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                        cameraManager.startCamera(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Split Comparison Labels
            if (isCompareMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ORIGINAL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("GRADED", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Viewfinder Overlays
            GridOverlay(gridType = settings.gridType)

            if (settings.showLevelIndicator) {
                LevelIndicatorOverlay(angles = levelAngles)
            }

            FocusRingOverlay(focusPointNorm = cameraUiState.focusPoint)

            CountdownOverlay(
                isCountingDown = cameraUiState.isCountingDown,
                secondsRemaining = cameraUiState.countdownRemaining
            )
        }

        // Top HUD Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            TopHudBar(
                uiState = cameraUiState,
                settings = settings,
                onCycleFlash = { cameraManager.cycleFlashMode() },
                onToggleTorch = { cameraManager.toggleTorch() },
                onCycleTimer = {
                    val nextTimer = when (settings.timerSeconds) {
                        0 -> 3
                        3 -> 5
                        5 -> 10
                        else -> 0
                    }
                    onSettingsChanged(settings.copy(timerSeconds = nextTimer))
                },
                onCycleGrid = {
                    val nextGrid = when (settings.gridType) {
                        GridType.NONE -> GridType.RULE_OF_THIRDS
                        GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
                        GridType.GOLDEN_RATIO -> GridType.SQUARE
                        GridType.SQUARE -> GridType.NONE
                    }
                    onSettingsChanged(settings.copy(gridType = nextGrid))
                },
                onToggleHistogram = {
                    onSettingsChanged(settings.copy(showHistogram = !settings.showHistogram))
                },
                onToggleProMode = {
                    val current = cameraUiState.isManualProMode
                    cameraManager.setZoom(1.0f)
                    // Toggle pro mode
                    coroutineScope.launch {
                        cameraManager.setExposure(0)
                    }
                    // Mutate state in CameraManager
                },
                onOpenSettings = onOpenSettings
            )

            // Lens Switcher (0.5x, 1x, 3x) and Histogram in top corner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Histogram
                if (settings.showHistogram) {
                    HistogramOverlay(histogram = histogramData)
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                // Lens Zoom Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmoledBlack.copy(alpha = 0.5f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val lenses = listOf(LensType.ULTRA_WIDE, LensType.WIDE, LensType.TELEPHOTO)
                    lenses.forEach { lens ->
                        val isSelected = cameraUiState.currentLens == lens
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) GoldAccent else Color.Transparent)
                                .pointerInput(Unit) {
                                    detectTapGestures { cameraManager.selectLens(lens) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lens.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AmoledBlack else TextPrimary
                            )
                        }
                    }
                }
            }

            // Pro Manual Controls Bar (when pro mode active)
            ManualControlsBar(
                uiState = cameraUiState,
                onExposureChange = { cameraManager.setExposure(it) },
                onIsoChange = { /* Store manual ISO */ },
                onShutterChange = { /* Store manual shutter */ },
                onWbChange = { /* Store manual WB */ },
                onFocusChange = { /* Manual focus */ }
            )
        }

        // Bottom Controls: Preset Selector & Shutter Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            PresetSelectorBar(
                presets = allPresets,
                selectedPreset = selectedPreset,
                intensity = presetIntensity,
                isCompareMode = isCompareMode,
                onSelectPreset = { selectedPreset = it },
                onIntensityChange = { presetIntensity = it },
                onToggleCompare = { isCompareMode = !isCompareMode },
                onImportXmp = {
                    xmpPickerLauncher.launch(arrayOf("*/*", "application/xml", "text/xml"))
                },
                onInspectPreset = { inspectedPreset = it }
            )

            BottomShutterBar(
                mode = cameraMode,
                uiState = cameraUiState,
                latestMediaUri = latestCapturedUri,
                onModeChange = { cameraMode = it },
                onShutterClick = {
                    when (cameraMode) {
                        CameraMode.PHOTO -> {
                            cameraManager.capturePhoto(
                                settings = settings,
                                onSuccess = { uri ->
                                    latestCapturedUri = uri
                                    Toast.makeText(context, "Photo saved with ${selectedPreset.name}!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        CameraMode.VIDEO -> {
                            cameraManager.toggleVideoRecording(
                                settings = settings,
                                onSaved = { uri ->
                                    latestCapturedUri = uri
                                    Toast.makeText(context, "Cinematic video recorded!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        CameraMode.BURST -> {
                            cameraManager.startBurstCapture(
                                settings = settings,
                                onComplete = { count ->
                                    Toast.makeText(context, "Burst completed: $count photos saved", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                onFlipCamera = { previewViewRef?.let { cameraManager.toggleCamera(it) } },
                onOpenGallery = onOpenGallery
            )
        }

        // Preset Inspector Dialog
        inspectedPreset?.let { p ->
            PresetInspectorDialog(
                preset = p,
                onDismiss = { inspectedPreset = null },
                onRename = { newName ->
                    coroutineScope.launch {
                        presetRepository.updatePreset(p.copy(name = newName))
                        if (selectedPreset.id == p.id) {
                            selectedPreset = p.copy(name = newName)
                        }
                    }
                },
                onDuplicate = {
                    coroutineScope.launch {
                        val copy = presetRepository.duplicatePreset(p)
                        selectedPreset = copy
                        inspectedPreset = null
                        Toast.makeText(context, "Preset duplicated", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = {
                    coroutineScope.launch {
                        presetRepository.deletePreset(p.id)
                        selectedPreset = XmpPreset.DEFAULT
                        inspectedPreset = null
                        Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun CameraPermissionFallback(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Camera Permission Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "XMP Camera Pro needs camera and microphone access to shoot photos and record cinematic videos with live Lightroom presets.",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                modifier = Modifier.testTag("request_permission_button")
            ) {
                Text("Grant Permissions", color = AmoledBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildXmpColorMatrix(preset: XmpPreset, intensity: Float): ColorMatrix {
    val matrix = ColorMatrix()
    val eff = intensity.coerceIn(0f, 1f)

    // Contrast & exposure
    val expFactor = (preset.exposure * 0.2f * eff)
    val contrastFactor = (1.0f + (preset.contrast / 100f) * 0.35f * eff)

    // Temp & Tint shifts
    val tempShift = (preset.temperature / 100f) * 0.15f * eff
    val tintShift = (preset.tint / 100f) * 0.12f * eff

    val satFactor = (1.0f + (preset.saturation / 100f) * eff)
    matrix.setToSaturation(satFactor)

    val currentVals = matrix.values
    currentVals[0] = currentVals[0] * contrastFactor + tempShift
    currentVals[4] = currentVals[4] + expFactor * 50f
    currentVals[6] = currentVals[6] * contrastFactor - tintShift
    currentVals[12] = currentVals[12] * contrastFactor - tempShift

    return matrix
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val stringBuilder = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append('\n')
                line = reader.readLine()
            }
        }
    }
    stringBuilder.toString()
}
