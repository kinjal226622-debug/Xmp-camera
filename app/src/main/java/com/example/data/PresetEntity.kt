package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.xmp.XmpPreset

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val isBuiltIn: Boolean,
    val intensity: Float,
    val rawXml: String,

    val exposure: Float,
    val contrast: Float,
    val highlights: Float,
    val shadows: Float,
    val whites: Float,
    val blacks: Float,

    val temperature: Float,
    val tint: Float,
    val vibrance: Float,
    val saturation: Float,
    val clarity: Float,
    val dehaze: Float,

    val vignette: Float,
    val grain: Float,

    val shadowHue: Float,
    val shadowSat: Float,
    val highlightHue: Float,
    val highlightSat: Float,
    val balance: Float,

    val toneCurveData: String = "", // serialized points "x,y;x,y"
    val unsupportedTags: String = "", // comma-separated
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): XmpPreset {
        val points = if (toneCurveData.isNotBlank()) {
            toneCurveData.split(";").mapNotNull { pairStr ->
                val p = pairStr.split(",")
                if (p.size == 2) {
                    val x = p[0].toFloatOrNull() ?: 0f
                    val y = p[1].toFloatOrNull() ?: 0f
                    Pair(x, y)
                } else null
            }
        } else emptyList()

        val unsupported = if (unsupportedTags.isNotBlank()) {
            unsupportedTags.split(",").filter { it.isNotBlank() }
        } else emptyList()

        return XmpPreset(
            id = id,
            name = name,
            category = category,
            isBuiltIn = isBuiltIn,
            intensity = intensity,
            rawXml = rawXml,
            exposure = exposure,
            contrast = contrast,
            highlights = highlights,
            shadows = shadows,
            whites = whites,
            blacks = blacks,
            temperature = temperature,
            tint = tint,
            vibrance = vibrance,
            saturation = saturation,
            clarity = clarity,
            dehaze = dehaze,
            vignette = vignette,
            grain = grain,
            shadowHue = shadowHue,
            shadowSat = shadowSat,
            highlightHue = highlightHue,
            highlightSat = highlightSat,
            balance = balance,
            toneCurvePoints = points,
            unsupportedTags = unsupported
        )
    }

    companion object {
        fun fromDomain(preset: XmpPreset): PresetEntity {
            val curveData = preset.toneCurvePoints.joinToString(";") { "${it.first},${it.second}" }
            val unsupported = preset.unsupportedTags.joinToString(",")

            return PresetEntity(
                id = preset.id,
                name = preset.name,
                category = preset.category,
                isBuiltIn = preset.isBuiltIn,
                intensity = preset.intensity,
                rawXml = preset.rawXml,
                exposure = preset.exposure,
                contrast = preset.contrast,
                highlights = preset.highlights,
                shadows = preset.shadows,
                whites = preset.whites,
                blacks = preset.blacks,
                temperature = preset.temperature,
                tint = preset.tint,
                vibrance = preset.vibrance,
                saturation = preset.saturation,
                clarity = preset.clarity,
                dehaze = preset.dehaze,
                vignette = preset.vignette,
                grain = preset.grain,
                shadowHue = preset.shadowHue,
                shadowSat = preset.shadowSat,
                highlightHue = preset.highlightHue,
                highlightSat = preset.highlightSat,
                balance = preset.balance,
                toneCurveData = curveData,
                unsupportedTags = unsupported
            )
        }
    }
}
