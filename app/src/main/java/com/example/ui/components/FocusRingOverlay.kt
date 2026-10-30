package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FocusGold

@Composable
fun FocusRingOverlay(
    focusPointNorm: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    if (focusPointNorm == null) return

    val scale = remember(focusPointNorm) { Animatable(1.5f) }
    val alpha = remember(focusPointNorm) { Animatable(1.0f) }

    LaunchedEffect(focusPointNorm) {
        scale.snapTo(1.5f)
        alpha.snapTo(1.0f)
        scale.animateTo(1.0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = focusPointNorm.first * size.width
        val cy = focusPointNorm.second * size.height
        val baseRadius = 36.dp.toPx() * scale.value
        val ringColor = FocusGold.copy(alpha = alpha.value * 0.9f)
        val stroke = 1.5.dp.toPx()

        // Circular ring
        drawCircle(
            color = ringColor,
            radius = baseRadius,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )

        // Four cardinal indicator notches
        val notchLen = 6.dp.toPx()
        drawLine(ringColor, Offset(cx, cy - baseRadius - notchLen), Offset(cx, cy - baseRadius), stroke)
        drawLine(ringColor, Offset(cx, cy + baseRadius), Offset(cx, cy + baseRadius + notchLen), stroke)
        drawLine(ringColor, Offset(cx - baseRadius - notchLen, cy), Offset(cx - baseRadius, cy), stroke)
        drawLine(ringColor, Offset(cx + baseRadius, cy), Offset(cx + baseRadius + notchLen, cy), stroke)

        // Center dot
        drawCircle(
            color = ringColor,
            radius = 2.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
