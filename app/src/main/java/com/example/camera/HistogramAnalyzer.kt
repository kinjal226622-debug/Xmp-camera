package com.example.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

data class HistogramData(
    val lumaBins: FloatArray = FloatArray(32), // 32 bins for responsive UI drawing
    val redBins: FloatArray = FloatArray(32),
    val greenBins: FloatArray = FloatArray(32),
    val blueBins: FloatArray = FloatArray(32)
)

class HistogramAnalyzer(private val onHistogramReady: (HistogramData) -> Unit) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTime = 0L

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Throttle to 10 fps to keep GPU/CPU cool and smooth
        if (currentTime - lastAnalyzedTime < 100) {
            image.close()
            return
        }
        lastAnalyzedTime = currentTime

        try {
            val yBuffer: ByteBuffer = image.planes[0].buffer
            val remaining = yBuffer.remaining()
            val step = (remaining / 1024).coerceAtLeast(1) // sample 1024 points for fast analysis

            val bins = IntArray(32)
            var maxCount = 1

            var i = 0
            while (i < remaining) {
                val yVal = yBuffer.get(i).toInt() and 0xFF
                val binIndex = (yVal shr 3).coerceIn(0, 31) // map 0..255 to 0..31
                bins[binIndex]++
                if (bins[binIndex] > maxCount) maxCount = bins[binIndex]
                i += step
            }

            val normalized = FloatArray(32) { idx ->
                (bins[idx].toFloat() / maxCount).coerceIn(0f, 1f)
            }

            onHistogramReady(HistogramData(lumaBins = normalized))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}
