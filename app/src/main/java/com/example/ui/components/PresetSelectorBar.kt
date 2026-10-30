package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.xmp.XmpPreset

@Composable
fun PresetSelectorBar(
    presets: List<XmpPreset>,
    selectedPreset: XmpPreset,
    intensity: Float,
    isCompareMode: Boolean,
    onSelectPreset: (XmpPreset) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onToggleCompare: () -> Unit,
    onImportXmp: () -> Unit,
    onInspectPreset: (XmpPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var showIntensitySlider by remember { mutableStateOf(false) }

    val categories = remember(presets) {
        listOf("All") + presets.map { it.category }.distinct().filter { it != "Original" }
    }

    val filteredPresets = remember(presets, selectedCategory) {
        if (selectedCategory == "All") presets
        else presets.filter { it.category == selectedCategory || it.id == XmpPreset.DEFAULT.id }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack.copy(alpha = 0.65f))
            .padding(vertical = 4.dp)
    ) {
        // Intensity Slider & Compare Bar (shown when non-default preset selected)
        if (selectedPreset.id != XmpPreset.DEFAULT.id) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Preset Name & Intensity %
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showIntensitySlider = !showIntensitySlider }
                ) {
                    Text(
                        text = selectedPreset.name,
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " ${(intensity * 100).toInt()}%",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Compare Original vs Graded button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCompareMode) GoldAccent else Color.White.copy(alpha = 0.12f))
                            .clickable { onToggleCompare() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("compare_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare",
                                tint = if (isCompareMode) AmoledBlack else TextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isCompareMode) "SPLIT" else "COMPARE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompareMode) AmoledBlack else TextPrimary,
                                modifier = Modifier.padding(start = 3.dp)
                            )
                        }
                    }

                    // Inspect XMP Details
                    IconButton(
                        onClick = { onInspectPreset(selectedPreset) },
                        modifier = Modifier.size(32.dp).testTag("inspect_preset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Inspect XMP Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Intensity slider control
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("intensity_slider")
            )
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Import XMP chip first
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldAccent.copy(alpha = 0.2f))
                        .border(1.dp, GoldAccent, RoundedCornerShape(12.dp))
                        .clickable { onImportXmp() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("import_xmp_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import XMP",
                            tint = GoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "+ Import XMP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextMuted
                    )
                }
            }
        }

        // Horizontal Presets Thumbnails List
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPresets) { preset ->
                val isSelected = preset.id == selectedPreset.id
                PresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { onSelectPreset(preset) },
                    onLongClick = { onInspectPreset(preset) }
                )
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: XmpPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (preset.id == XmpPreset.DEFAULT.id) DarkCard else getPresetMoodColor(preset))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (preset.id == XmpPreset.DEFAULT.id) {
                Text(
                    text = "RAW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            } else {
                Text(
                    text = preset.name.take(2).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Text(
            text = preset.name,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) GoldAccent else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun getPresetMoodColor(preset: XmpPreset): Color {
    return when (preset.category.lowercase()) {
        "cinematic" -> Color(0xFF1E3A4C)
        "film" -> Color(0xFF4A3B2C)
        "vintage" -> Color(0xFF4C3627)
        "moody" -> Color(0xFF1B232E)
        "portrait" -> Color(0xFF4C2F2D)
        "wedding" -> Color(0xFF3E3247)
        "street" -> Color(0xFF28282E)
        "nature" -> Color(0xFF1B3D2B)
        "black & white" -> Color(0xFF202020)
        "warm" -> Color(0xFF4E2C1B)
        "cool" -> Color(0xFF1E334E)
        else -> Color(0xFF2B2B38)
    }
}
