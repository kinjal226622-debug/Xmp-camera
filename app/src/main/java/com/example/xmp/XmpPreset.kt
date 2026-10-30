package com.example.xmp

import java.util.UUID

/**
 * Data model representing Lightroom Develop Settings parsed from an XMP preset.
 */
data class XmpPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Custom XMP",
    val isBuiltIn: Boolean = false,
    val intensity: Float = 1.0f, // 0.0 to 1.0
    val rawXml: String = "",

    // Basic Tonal Adjustments
    val exposure: Float = 0.0f,        // -5.0 to +5.0 EV
    val contrast: Float = 0.0f,        // -100 to +100
    val highlights: Float = 0.0f,      // -100 to +100
    val shadows: Float = 0.0f,         // -100 to +100
    val whites: Float = 0.0f,          // -100 to +100
    val blacks: Float = 0.0f,          // -100 to +100

    // Presence & Color
    val temperature: Float = 0.0f,     // -100 to +100 (warm/cool shift)
    val tint: Float = 0.0f,            // -100 to +100 (green/magenta shift)
    val vibrance: Float = 0.0f,        // -100 to +100
    val saturation: Float = 0.0f,      // -100 to +100
    val clarity: Float = 0.0f,         // -100 to +100
    val dehaze: Float = 0.0f,          // -100 to +100

    // Effects
    val vignette: Float = 0.0f,        // -100 to +100 (dark/light edges)
    val grain: Float = 0.0f,           // 0 to 100

    // Color Grading / Split Toning
    val shadowHue: Float = 0.0f,       // 0 to 360
    val shadowSat: Float = 0.0f,       // 0 to 100
    val highlightHue: Float = 0.0f,    // 0 to 360
    val highlightSat: Float = 0.0f,    // 0 to 100
    val balance: Float = 0.0f,         // -100 to +100

    // Tone Curve Points (normalized 0..1 x, y)
    val toneCurvePoints: List<Pair<Float, Float>> = emptyList(),

    // Detected but unsupported XMP settings (to inform the user)
    val unsupportedTags: List<String> = emptyList()
) {
    companion object {
        val DEFAULT = XmpPreset(
            id = "default_original",
            name = "Original",
            category = "Original",
            isBuiltIn = true,
            intensity = 1.0f
        )
    }
}
