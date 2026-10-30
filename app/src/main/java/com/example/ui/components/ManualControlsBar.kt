package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraUiState
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class ProControlTab {
    EV, ISO, SHUTTER, WB, FOCUS
}

@Composable
fun ManualControlsBar(
    uiState: CameraUiState,
    onExposureChange: (Int) -> Unit,
    onIsoChange: (Int) -> Unit,
    onShutterChange: (String) -> Unit,
    onWbChange: (Int) -> Unit,
    onFocusChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProControlTab.EV) }

    AnimatedVisibility(
        visible = uiState.isManualProMode,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmoledBlack.copy(alpha = 0.75f))
                .padding(vertical = 8.dp)
        ) {
            // Slider / Value Selector Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    ProControlTab.EV -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val evVal = uiState.exposureCompensation * uiState.exposureStep
                            Text(
                                text = "EV ${if (evVal >= 0) "+" else ""}${String.format("%.1f", evVal)}",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = uiState.exposureCompensation.toFloat(),
                                onValueChange = { onExposureChange(it.toInt()) },
                                valueRange = uiState.minExposure.toFloat()..uiState.maxExposure.toFloat(),
                                steps = (uiState.maxExposure - uiState.minExposure) - 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    ProControlTab.ISO -> {
                        val isos = listOf(50, 100, 200, 400, 800, 1600, 3200)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(isos) { iso ->
                                val isSelected = uiState.manualIso == iso
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldAccent else Color.White.copy(alpha = 0.12f))
                                        .clickable { onIsoChange(iso) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "$iso",
                                        color = if (isSelected) AmoledBlack else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    ProControlTab.SHUTTER -> {
                        val shutters = listOf("1/2000s", "1/1000s", "1/500s", "1/250s", "1/125s", "1/60s", "1/30s", "1/15s")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(shutters) { speed ->
                                val isSelected = uiState.manualShutterSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldAccent else Color.White.copy(alpha = 0.12f))
                                        .clickable { onShutterChange(speed) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = speed,
                                        color = if (isSelected) AmoledBlack else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    ProControlTab.WB -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.manualWhiteTemp}K",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = uiState.manualWhiteTemp.toFloat(),
                                onValueChange = { onWbChange(it.toInt()) },
                                valueRange = 2500f..8000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    ProControlTab.FOCUS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (uiState.manualFocus <= 0.05f) "Macro" else if (uiState.manualFocus >= 0.95f) "Infinity" else "${(uiState.manualFocus * 100).toInt()}%",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = uiState.manualFocus,
                                onValueChange = onFocusChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }

            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProControlTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tab.name,
                            color = if (isSelected) GoldAccent else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
