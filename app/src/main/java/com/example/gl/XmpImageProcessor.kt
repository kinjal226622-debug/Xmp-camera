package com.example.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.xmp.XmpPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object XmpImageProcessor {

    /**
     * Applies full XMP color grading to a Bitmap.
     * Uses multithreaded pixel processing for optimal performance and original quality retention.
     */
    suspend fun applyPreset(
        srcBitmap: Bitmap,
        preset: XmpPreset,
        intensity: Float = preset.intensity,
        addWatermark: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        if (preset.id == XmpPreset.DEFAULT.id && intensity >= 0.99f && !addWatermark) {
            return@withContext srcBitmap.copy(srcBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = srcBitmap.width
        val height = srcBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val expMult = 2.0.pow(preset.exposure.toDouble()).toFloat()
        val contrastFactor = 1.0f + (preset.contrast / 100.0f)
        val whitesShift = preset.whites * 0.2f / 100.0f
        val blacksShift = preset.blacks * 0.15f / 100.0f
        val highlightsShift = preset.highlights * 0.35f / 100.0f
        val shadowsShift = preset.shadows * 0.35f / 100.0f
        val tempR = preset.temperature * 0.15f / 100.0f
        val tempB = -preset.temperature * 0.15f / 100.0f
        val tintG = -preset.tint * 0.12f / 100.0f
        val tintRB = preset.tint * 0.06f / 100.0f
        val satAdj = preset.saturation / 100.0f
        val vibAdj = preset.vibrance / 100.0f
        val vignetteAmt = preset.vignette / 100.0f
        val grainAmt = preset.grain / 100.0f

        val shadowRgb = XmpColorShader.hueToRgbVec3(preset.shadowHue)
        val shadowSat = preset.shadowSat / 100.0f
        val hlRgb = XmpColorShader.hueToRgbVec3(preset.highlightHue)
        val hlSat = preset.highlightSat / 100.0f

        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = hypot(centerX, centerY)
        val random = Random(42)

        // Parallel chunk processing across CPU cores
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val rowsPerCore = height / cores

        val threads = (0 until cores).map { coreIndex ->
            val startY = coreIndex * rowsPerCore
            val endY = if (coreIndex == cores - 1) height else (coreIndex + 1) * rowsPerCore

            kotlin.concurrent.thread {
                for (y in startY until endY) {
                    val rowOffset = y * width
                    val distY = (y - centerY)

                    for (x in 0 until width) {
                        val index = rowOffset + x
                        val p = pixels[index]
                        val origR = (p shr 16 and 0xFF) / 255.0f
                        val origG = (p shr 8 and 0xFF) / 255.0f
                        val origB = (p and 0xFF) / 255.0f
                        val alpha = p ushr 24

                        var r = origR
                        var g = origG
                        var b = origB

                        // 1. Exposure
                        r *= expMult
                        g *= expMult
                        b *= expMult

                        // 2. Whites & Blacks
                        r = (r + whitesShift).coerceIn(0f, 1f)
                        g = (g + whitesShift).coerceIn(0f, 1f)
                        b = (b + whitesShift).coerceIn(0f, 1f)

                        r = (r + blacksShift * (1f - r)).coerceIn(0f, 1f)
                        g = (g + blacksShift * (1f - g)).coerceIn(0f, 1f)
                        b = (b + blacksShift * (1f - b)).coerceIn(0f, 1f)

                        // 3. Highlights & Shadows
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val hlWeight = ((luma - 0.5f) * 2f).coerceIn(0f, 1f)
                        val shWeight = (1.0f - (luma * 2f)).coerceIn(0f, 1f)

                        r += r * (highlightsShift * hlWeight) + r * (shadowsShift * shWeight)
                        g += g * (highlightsShift * hlWeight) + g * (shadowsShift * shWeight)
                        b += b * (highlightsShift * hlWeight) + b * (shadowsShift * shWeight)

                        // 4. Contrast
                        r = ((r - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                        g = ((g - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)
                        b = ((b - 0.5f) * contrastFactor + 0.5f).coerceIn(0f, 1f)

                        // 5. White Balance (Temperature & Tint)
                        r = (r + tempR + tintRB).coerceIn(0f, 1f)
                        g = (g + tintG).coerceIn(0f, 1f)
                        b = (b + tempB + tintRB).coerceIn(0f, 1f)

                        // 6. Split Toning
                        if (shadowSat > 0f) {
                            r = lerp(r, r * shadowRgb[0], shWeight * shadowSat)
                            g = lerp(g, g * shadowRgb[1], shWeight * shadowSat)
                            b = lerp(b, b * shadowRgb[2], shWeight * shadowSat)
                        }
                        if (hlSat > 0f) {
                            r = lerp(r, (r + hlRgb[0]) * 0.5f, hlWeight * hlSat)
                            g = lerp(g, (g + hlRgb[1]) * 0.5f, hlWeight * hlSat)
                            b = lerp(b, (b + hlRgb[2]) * 0.5f, hlWeight * hlSat)
                        }

                        // 7. Saturation & Vibrance
                        val currentLuma = 0.299f * r + 0.587f * g + 0.114f * b
                        val maxC = max(r, max(g, b))
                        val minC = min(r, min(g, b))
                        val satVal = (maxC - minC) / (maxC + 1.0e-5f)
                        val totalSat = 1.0f + satAdj + (vibAdj * (1.0f - satVal))

                        r = (currentLuma + (r - currentLuma) * totalSat).coerceIn(0f, 1f)
                        g = (currentLuma + (g - currentLuma) * totalSat).coerceIn(0f, 1f)
                        b = (currentLuma + (b - currentLuma) * totalSat).coerceIn(0f, 1f)

                        // 8. Vignette
                        if (vignetteAmt != 0f) {
                            val distX = (x - centerX)
                            val dist = hypot(distX, distY) / maxDist
                            val vig = (dist * dist).coerceIn(0f, 1f)
                            if (vignetteAmt < 0f) {
                                val factor = 1f + vignetteAmt * vig
                                r *= factor
                                g *= factor
                                b *= factor
                            } else {
                                r = (r + vignetteAmt * 0.3f * vig).coerceIn(0f, 1f)
                                g = (g + vignetteAmt * 0.3f * vig).coerceIn(0f, 1f)
                                b = (b + vignetteAmt * 0.3f * vig).coerceIn(0f, 1f)
                            }
                        }

                        // 9. Grain
                        if (grainAmt > 0f) {
                            val noise = (random.nextFloat() - 0.5f) * (grainAmt * 0.18f)
                            r = (r + noise).coerceIn(0f, 1f)
                            g = (g + noise).coerceIn(0f, 1f)
                            b = (b + noise).coerceIn(0f, 1f)
                        }

                        // Blend by intensity
                        val finalR = (lerp(origR, r, intensity) * 255.0f).toInt().coerceIn(0, 255)
                        val finalG = (lerp(origG, g, intensity) * 255.0f).toInt().coerceIn(0, 255)
                        val finalB = (lerp(origB, b, intensity) * 255.0f).toInt().coerceIn(0, 255)

                        pixels[index] = (alpha shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                    }
                }
            }
        }

        threads.forEach { it.join() }
        output.setPixels(pixels, 0, width, 0, 0, width, height)

        if (addWatermark) {
            drawWatermark(output, preset.name)
        }

        output
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    private fun drawWatermark(bitmap: Bitmap, presetName: String) {
        val canvas = Canvas(bitmap)
        val text = "SHOT ON XMP CAMERA PRO  •  $presetName"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (bitmap.height * 0.022f).coerceAtLeast(24f)
            setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
            letterSpacing = 0.08f
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val margin = bitmap.width * 0.04f
        val x = bitmap.width - bounds.width() - margin
        val y = bitmap.height - margin

        canvas.drawText(text, x, y, paint)
    }
}
