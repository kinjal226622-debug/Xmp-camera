package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camera.GridType

@Composable
fun GridOverlay(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.28f)
        val strokeWidth = 1.dp.toPx()

        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                // Vertical lines at 1/3 and 2/3
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)
                // Horizontal lines at 1/3 and 2/3
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
            }
            GridType.GOLDEN_RATIO -> {
                // Phi ratio = 0.618 and 0.382
                val phi1 = 0.382f
                val phi2 = 0.618f
                drawLine(gridColor, Offset(w * phi1, 0f), Offset(w * phi1, h), strokeWidth)
                drawLine(gridColor, Offset(w * phi2, 0f), Offset(w * phi2, h), strokeWidth)
                drawLine(gridColor, Offset(0f, h * phi1), Offset(w, h * phi1), strokeWidth)
                drawLine(gridColor, Offset(0f, h * phi2), Offset(w, h * phi2), strokeWidth)
            }
            GridType.SQUARE -> {
                // 1:1 square crop guide centered
                val squareSize = kotlin.math.min(w, h)
                val left = (w - squareSize) / 2f
                val top = (h - squareSize) / 2f
                val right = left + squareSize
                val bottom = top + squareSize

                drawLine(gridColor, Offset(left, top), Offset(right, top), strokeWidth)
                drawLine(gridColor, Offset(right, top), Offset(right, bottom), strokeWidth)
                drawLine(gridColor, Offset(right, bottom), Offset(left, bottom), strokeWidth)
                drawLine(gridColor, Offset(left, bottom), Offset(left, top), strokeWidth)
            }
            GridType.NONE -> {}
        }
    }
}
