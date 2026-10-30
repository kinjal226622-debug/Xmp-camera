package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraSettings
import com.example.camera.CameraUiState
import com.example.camera.GridType
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun TopHudBar(
    uiState: CameraUiState,
    settings: CameraSettings,
    onCycleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleGrid: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleProMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode
        IconButton(
            onClick = onCycleFlash,
            modifier = Modifier.size(44.dp).testTag("flash_button")
        ) {
            val icon = when (uiState.flashMode) {
                1 -> Icons.Default.FlashOn
                2 -> Icons.Default.FlashOff
                else -> Icons.Default.FlashAuto
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash Mode",
                tint = if (uiState.flashMode == 1) GoldAccent else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Torch
        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier.size(44.dp).testTag("torch_button")
        ) {
            Icon(
                imageVector = Icons.Default.Highlight,
                contentDescription = "Torch",
                tint = if (uiState.isTorchOn) GoldAccent else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        // Timer (Off, 3s, 5s, 10s)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCycleTimer() }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = if (settings.timerSeconds > 0) GoldAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                if (settings.timerSeconds > 0) {
                    Text(
                        text = "${settings.timerSeconds}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // Grid Lines
        IconButton(
            onClick = onCycleGrid,
            modifier = Modifier.size(44.dp).testTag("grid_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = "Grid",
                tint = if (settings.gridType != GridType.NONE) GoldAccent else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        // PRO Mode toggle pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (uiState.isManualProMode) GoldAccent else Color.White.copy(alpha = 0.12f))
                .clickable { onToggleProMode() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("pro_mode_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Pro Mode",
                    tint = if (uiState.isManualProMode) AmoledBlack else TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (uiState.isManualProMode) "PRO" else "AUTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isManualProMode) AmoledBlack else TextPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Settings Button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(44.dp).testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
