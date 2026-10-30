package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.camera.GridType
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBlack,
        topBar = {
            TopAppBar(
                title = { Text("Camera Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader("PHOTO CAPTURE")
            SettingRow(
                title = "Photo Resolution",
                subtitle = settings.photoResolution,
                onClick = {
                    val next = when (settings.photoResolution) {
                        "48MP / High-Res" -> "24MP"
                        "24MP" -> "12MP Standard"
                        else -> "48MP / High-Res"
                    }
                    onSettingsChanged(settings.copy(photoResolution = next))
                }
            )
            SettingToggle(
                title = "Save Original Raw Copy",
                subtitle = "Save an unedited copy alongside the XMP filtered photo",
                checked = settings.saveOriginalAlso,
                onCheckedChange = { onSettingsChanged(settings.copy(saveOriginalAlso = it)) }
            )
            SettingToggle(
                title = "Add XMP Watermark",
                subtitle = "Include 'Shot on XMP Camera Pro • [Preset]' in exported photos",
                checked = settings.addWatermark,
                onCheckedChange = { onSettingsChanged(settings.copy(addWatermark = it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("CINEMATIC VIDEO")
            SettingRow(
                title = "Video Resolution",
                subtitle = settings.videoQuality,
                onClick = {
                    val next = when (settings.videoQuality) {
                        "4K UHD (2160p)" -> "Full HD (1080p)"
                        "Full HD (1080p)" -> "HD (720p)"
                        else -> "4K UHD (2160p)"
                    }
                    onSettingsChanged(settings.copy(videoQuality = next))
                }
            )
            SettingRow(
                title = "Frame Rate",
                subtitle = "${settings.videoFps} FPS",
                onClick = {
                    val next = when (settings.videoFps) {
                        24 -> 30
                        30 -> 60
                        else -> 24
                    }
                    onSettingsChanged(settings.copy(videoFps = next))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("ASSISTANCE & OVERLAYS")
            SettingRow(
                title = "Grid Lines",
                subtitle = when (settings.gridType) {
                    GridType.NONE -> "Off"
                    GridType.RULE_OF_THIRDS -> "Rule of Thirds"
                    GridType.GOLDEN_RATIO -> "Golden Ratio (Phi)"
                    GridType.SQUARE -> "Square 1:1"
                },
                onClick = {
                    val next = when (settings.gridType) {
                        GridType.NONE -> GridType.RULE_OF_THIRDS
                        GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
                        GridType.GOLDEN_RATIO -> GridType.SQUARE
                        GridType.SQUARE -> GridType.NONE
                    }
                    onSettingsChanged(settings.copy(gridType = next))
                }
            )
            SettingToggle(
                title = "Live Level Horizon Indicator",
                subtitle = "Display pitch and roll artificial horizon",
                checked = settings.showLevelIndicator,
                onCheckedChange = { onSettingsChanged(settings.copy(showLevelIndicator = it)) }
            )
            SettingToggle(
                title = "Live RGB Luminance Histogram",
                subtitle = "Monitor exposure clipping in real-time",
                checked = settings.showHistogram,
                onCheckedChange = { onSettingsChanged(settings.copy(showHistogram = it)) }
            )
            SettingToggle(
                title = "Haptic Shutter Feedback",
                subtitle = "Vibrate on shutter click and level lock",
                checked = settings.hapticFeedback,
                onCheckedChange = { onSettingsChanged(settings.copy(hapticFeedback = it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("ABOUT")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text("XMP Camera Pro", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Version 1.0 • Professional DSLR & Lightroom Color Engine", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Combines native CameraX 1.5 camera capture with an Adobe Lightroom develop settings parser and real-time GPU color grading pipeline.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = GoldAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Text("Change", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AmoledBlack,
                checkedTrackColor = GoldAccent,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkCard
            )
        )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}
