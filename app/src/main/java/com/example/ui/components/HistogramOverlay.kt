package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camera.HistogramData
import com.example.ui.theme.DarkSurface

@Composable
fun HistogramOverlay(
    histogram: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(86.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val bins = histogram.lumaBins
            val count = bins.size
            if (count == 0) return@Canvas

            val barWidth = size.width / count
            val h = size.height

            for (i in 0 until count) {
                val binVal = bins[i].coerceIn(0.04f, 1f)
                val barHeight = binVal * h
                drawRect(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(i * barWidth, h - barHeight),
                    size = Size(barWidth - 0.5f, barHeight)
                )
            }
        }
    }
}
