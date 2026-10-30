package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.camera.CameraSettings
import com.example.camera.MediaStorageManager
import com.example.data.PresetRepository
import com.example.ui.CameraScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    CAMERA,
    GALLERY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val presetRepository = PresetRepository(applicationContext)
        val storageManager = MediaStorageManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledBlack)
                ) {
                    AppNavigation(
                        presetRepository = presetRepository,
                        storageManager = storageManager
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    presetRepository: PresetRepository,
    storageManager: MediaStorageManager
) {
    var currentScreen by remember { mutableStateOf(Screen.CAMERA) }
    var cameraSettings by remember { mutableStateOf(CameraSettings()) }

    when (currentScreen) {
        Screen.CAMERA -> {
            CameraScreen(
                presetRepository = presetRepository,
                storageManager = storageManager,
                settings = cameraSettings,
                onSettingsChanged = { cameraSettings = it },
                onOpenGallery = { currentScreen = Screen.GALLERY },
                onOpenSettings = { currentScreen = Screen.SETTINGS }
            )
        }
        Screen.GALLERY -> {
            val presets = remember { com.example.data.BuiltInPresets.all }
            GalleryScreen(
                storageManager = storageManager,
                presets = presets,
                onNavigateBack = { currentScreen = Screen.CAMERA }
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                settings = cameraSettings,
                onSettingsChanged = { cameraSettings = it },
                onNavigateBack = { currentScreen = Screen.CAMERA }
            )
        }
    }
}
