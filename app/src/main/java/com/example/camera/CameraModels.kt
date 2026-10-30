package com.example.camera

enum class GridType {
    NONE,
    RULE_OF_THIRDS,
    GOLDEN_RATIO,
    SQUARE
}

enum class LensType(val displayName: String, val zoomFactor: Float) {
    ULTRA_WIDE("0.5x", 0.5f),
    WIDE("1x", 1.0f),
    TELEPHOTO("3x", 3.0f),
    FRONT("Front", 1.0f)
}

data class CameraSettings(
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    val showHistogram: Boolean = true,
    val showLevelIndicator: Boolean = true,
    val timerSeconds: Int = 0, // 0 (Off), 3, 5, 10
    val isBurstMode: Boolean = false,
    val burstCount: Int = 5,
    val photoResolution: String = "48MP / High-Res",
    val videoQuality: String = "4K UHD (2160p)",
    val videoFps: Int = 30, // 24, 30, 60
    val saveOriginalAlso: Boolean = false,
    val hapticFeedback: Boolean = true,
    val addWatermark: Boolean = false
)

data class CameraUiState(
    val isFrontCamera: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 8.0f,
    val exposureCompensation: Int = 0,
    val minExposure: Int = -4,
    val maxExposure: Int = 4,
    val exposureStep: Float = 0.5f,
    val flashMode: Int = 0, // 0 = Auto, 1 = On, 2 = Off
    val isTorchOn: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingDurationSec: Long = 0L,
    val isCountingDown: Boolean = false,
    val countdownRemaining: Int = 0,
    val isManualProMode: Boolean = false,
    val manualIso: Int = 100, // 50, 100, 200, 400, 800, 1600, 3200
    val manualShutterSpeed: String = "1/125s",
    val manualWhiteTemp: Int = 5500, // Kelvin 2500 - 8000
    val manualFocus: Float = 0.5f, // 0.0 (near) to 1.0 (infinity)
    val focusPoint: Pair<Float, Float>? = null, // Screen coords (0..1) for tap-to-focus ring
    val currentLens: LensType = LensType.WIDE,
    val compareMode: Boolean = false, // When true, preview shows original or split
    val compareSplitPosition: Float = 1.0f // 1.0 = all filtered, 0.5 = 50/50 split
)
