package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camera.LevelAngles
import com.example.ui.theme.FocusGold
import com.example.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelIndicatorOverlay(
    angles: LevelAngles,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        val isLevel = angles.isPerfectLevel
        val color = if (isLevel) SuccessGreen else FocusGold.copy(alpha = 0.7f)
        val stroke = if (isLevel) 2.5.dp.toPx() else 1.5.dp.toPx()

        // Center reticle
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = Offset(cx, cy)
        )

        // Artificial horizon line tilted by rollDegrees
        val rad = Math.toRadians(angles.rollDegrees.toDouble())
        val lineLen = 70.dp.toPx()
        val dx = (cos(rad) * lineLen).toFloat()
        val dy = (sin(rad) * lineLen).toFloat()

        // Left wing and right wing
        val gap = 14.dp.toPx()
        val startLeft = Offset(cx - dx, cy - dy)
        val endLeft = Offset(cx - (cos(rad) * gap).toFloat(), cy - (sin(rad) * gap).toFloat())

        val startRight = Offset(cx + (cos(rad) * gap).toFloat(), cy + (sin(rad) * gap).toFloat())
        val endRight = Offset(cx + dx, cy + dy)

        drawLine(color, startLeft, endLeft, stroke)
        drawLine(color, startRight, endRight, stroke)

        // Center fixed reference ticks
        val refColor = Color.White.copy(alpha = 0.35f)
        drawLine(refColor, Offset(cx - 30.dp.toPx(), cy), Offset(cx - 15.dp.toPx(), cy), 1.dp.toPx())
        drawLine(refColor, Offset(cx + 15.dp.toPx(), cy), Offset(cx + 30.dp.toPx(), cy), 1.dp.toPx())
    }
}
