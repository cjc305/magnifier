package com.example.magnifier.ui.magnifier

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.magnifier.MagnifierApplication
import com.example.magnifier.ui.UiEvent
import com.example.magnifier.ui.camera.CameraPreview
import com.example.magnifier.ui.gallery.GalleryScreen

@Composable
fun MagnifierScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MagnifierApplication
    val container = app.container

    val viewModel: MagnifierViewModel = viewModel(
        factory = remember(container) {
            MagnifierViewModelFactory(
                container.mediaRepository,
                container.cameraController,
                container.permissionGate,
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    // Toast 事件
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast ->
                    android.widget.Toast.makeText(
                        context, event.message, android.widget.Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> viewModel.onMediaPermissionResult(results) }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        mediaPermissionLauncher.launch(viewModel.mediaPermissionList().toTypedArray())
    }

    if (uiState.showGallery) {
        GalleryScreen(
            mediaRepository = container.mediaRepository,
            onBack = {
                viewModel.showGallery(false)
                viewModel.onGalleryReturn()
            },
            onImagesDeleted = { uris -> viewModel.onImagesDeleted(uris) },
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (permissions.cameraGranted) {
                    CameraPreview(
                        controller = container.cameraController,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "放大倍率: ${String.format("%.1f", uiState.zoomLevel)}x",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = uiState.zoomLevel,
                                onValueChange = viewModel::setZoom,
                                valueRange = 1f..10f,
                                steps = 89,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 手電筒
                            IconButton(onClick = viewModel::toggleFlash) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = if (uiState.isFlashOn) "關閉手電筒" else "開啟手電筒",
                                    tint = if (uiState.isFlashOn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // 拍照
                            IconButton(
                                onClick = viewModel::capture,
                                enabled = permissions.cameraGranted,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照儲存",
                                    tint = if (permissions.cameraGranted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 相簿入口
                            if (uiState.lastSavedImageUri != null) {
                                IconButton(onClick = { viewModel.showGallery(true) }) {
                                    AsyncImage(
                                        model = uiState.lastSavedImageUri,
                                        contentDescription = "查看最後儲存的圖片",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.showGallery(true) }) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "查看相簿",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "需要相機權限才能使用",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
