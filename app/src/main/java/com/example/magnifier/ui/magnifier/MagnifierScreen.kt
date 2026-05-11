package com.example.magnifier.ui.magnifier

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.magnifier.data.camera.CameraXController
import com.example.magnifier.data.media.MediaStoreMediaRepository
import com.example.magnifier.ui.camera.CameraPreview
import com.example.magnifier.ui.gallery.GalleryScreen
import kotlinx.coroutines.launch

@Composable
fun MagnifierScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaRepository = remember(context) { MediaStoreMediaRepository(context) }
    val cameraController = remember(context) { CameraXController(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(4f) }
    var isFlashOn by remember { mutableStateOf(false) }
    var lastSavedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showGallery by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                        permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true)
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // 把 UI state 推送到 controller（bind 完成後立即生效；bind 前先 set 也安全 — controller 內部 null-safe）
    LaunchedEffect(zoomLevel) { cameraController.setZoom(zoomLevel) }
    LaunchedEffect(isFlashOn) { cameraController.setTorch(isFlashOn) }

    // 根據狀態顯示相機或相簿
    if (showGallery) {
        GalleryScreen(
            mediaRepository = mediaRepository,
            onBack = {
                showGallery = false
                // 從相簿返回時，檢查 lastSavedImageUri 是否還存在
                val uri = lastSavedImageUri
                if (uri != null) {
                    coroutineScope.launch {
                        val allImages = mediaRepository.queryMagnifierImages()
                        if (!allImages.contains(uri)) {
                            lastSavedImageUri = null
                        }
                    }
                }
            },
            onImageDeleted = { deletedUri ->
                // 如果刪除的圖片是當前顯示的縮圖，清除它
                if (lastSavedImageUri == deletedUri) {
                    lastSavedImageUri = null
                }
            }
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
                if (hasCameraPermission) {
                    CameraPreview(
                        controller = cameraController,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 放大倍率顯示和滑桿
                        Column {
                            Text(
                                text = "放大倍率: ${String.format("%.1f", zoomLevel)}x",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = zoomLevel,
                                onValueChange = { zoomLevel = it },
                                valueRange = 1f..10f,
                                steps = 89,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 功能按鈕行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 手電筒開關
                            IconButton(
                                onClick = { isFlashOn = !isFlashOn }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = if (isFlashOn) "關閉手電筒" else "開啟手電筒",
                                    tint = if (isFlashOn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // 拍照儲存
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        cameraController.capture()
                                            .onSuccess { bitmap ->
                                                mediaRepository.save(bitmap)
                                                    .onSuccess { savedUri ->
                                                        lastSavedImageUri = savedUri
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "圖片已儲存到相簿",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .onFailure { e ->
                                                        Log.e("Magnifier", "儲存失敗", e)
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "儲存失敗，請檢查權限",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                            .onFailure { e ->
                                                Log.e("Magnifier", "拍照失敗", e)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "拍照失敗: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                },
                                enabled = hasCameraPermission,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照儲存",
                                    tint = if (hasCameraPermission) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 顯示最後儲存的圖片縮圖
                            if (lastSavedImageUri != null) {
                                IconButton(onClick = { showGallery = true }) {
                                    AsyncImage(
                                        model = lastSavedImageUri,
                                        contentDescription = "查看最後儲存的圖片",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            } else {
                                IconButton(onClick = { showGallery = true }) {
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
