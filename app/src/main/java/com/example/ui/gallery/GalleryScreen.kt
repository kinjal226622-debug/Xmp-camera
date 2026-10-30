package com.example.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.camera.MediaItem
import com.example.camera.MediaStorageManager
import com.example.gl.XmpImageProcessor
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.xmp.XmpPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    storageManager: MediaStorageManager,
    presets: List<XmpPreset>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler {
        if (selectedItem != null) {
            selectedItem = null
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        mediaItems = storageManager.getCapturedMedia()
        isLoading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedItem != null) selectedItem!!.displayName else "Gallery (${mediaItems.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedItem != null) selectedItem = null else onNavigateBack()
                        },
                        modifier = Modifier.testTag("gallery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GoldAccent
                )
            } else if (mediaItems.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No photos or videos yet", color = TextSecondary, fontSize = 16.sp)
                    Text("Capture your first cinematic shot!", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                // Media Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(mediaItems, key = { it.id }) { item ->
                        GalleryGridTile(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }

            // Fullscreen viewer overlay
            selectedItem?.let { item ->
                FullscreenMediaViewer(
                    item = item,
                    presets = presets,
                    onDismiss = { selectedItem = null },
                    onDelete = {
                        coroutineScope.launch {
                            storageManager.deleteMedia(item.uri)
                            mediaItems = storageManager.getCapturedMedia()
                            selectedItem = null
                        }
                    },
                    onShare = {
                        storageManager.shareMedia(item.uri, item.isVideo)
                    },
                    onReExport = { gradedUri ->
                        coroutineScope.launch {
                            mediaItems = storageManager.getCapturedMedia()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GalleryGridTile(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .background(DarkCard)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Preset Badge at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AmoledBlack.copy(alpha = 0.65f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.presetName,
                fontSize = 9.sp,
                color = GoldAccent,
                maxLines = 1
            )
        }

        // Video Duration badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RecordingRed.copy(alpha = 0.85f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "VIDEO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun FullscreenMediaViewer(
    item: MediaItem,
    presets: List<XmpPreset>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onReExport: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    var isReEditing by remember { mutableStateOf(false) }
    var reEditPreset by remember { mutableStateOf<XmpPreset?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Media Content
        if (item.isVideo) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(item.uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmoledBlack.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }

            Row {
                if (!item.isVideo) {
                    IconButton(
                        onClick = { isReEditing = !isReEditing },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AmoledBlack.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Re-Grade", tint = GoldAccent)
                    }
                }

                IconButton(
                    onClick = { showInfo = !showInfo },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmoledBlack.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = TextPrimary)
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmoledBlack.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmoledBlack.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RecordingRed)
                }
            }
        }

        // Re-Edit Preset Strip
        if (isReEditing && !item.isVideo) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(AmoledBlack.copy(alpha = 0.85f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Apply Different Preset:",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCard)
                                .clickable {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        val uri = reGradePhoto(context, item.uri, p)
                                        isProcessing = false
                                        if (uri != null) onReExport(uri)
                                        isReEditing = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = p.name, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // EXIF & Metadata Sheet
        if (showInfo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(DarkSurface)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "File Information",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${item.displayName}", color = TextPrimary, fontSize = 12.sp)
                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(item.dateTaken))
                    Text("Date: $dateStr", color = TextSecondary, fontSize = 12.sp)
                    Text("Preset: ${item.presetName}", color = TextSecondary, fontSize = 12.sp)
                    Text("Size: ${String.format("%.2f", item.sizeBytes / (1024f * 1024f))} MB", color = TextSecondary, fontSize = 12.sp)
                    if (item.isVideo) {
                        Text("Duration: ${item.durationMs / 1000}s", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GoldAccent
            )
        }
    }
}

private suspend fun reGradePhoto(context: Context, srcUri: Uri, preset: XmpPreset): Uri? = withContext(Dispatchers.IO) {
    try {
        val stream: InputStream? = context.contentResolver.openInputStream(srcUri)
        val bitmap = BitmapFactory.decodeStream(stream)
        stream?.close()
        if (bitmap != null) {
            val graded = XmpImageProcessor.applyPreset(bitmap, preset)
            MediaStorageManager(context).savePhoto(graded, preset.name, isOriginal = false)
        } else null
    } catch (e: Exception) {
        null
    }
}
