package com.example.magnifier.ui.gallery

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.magnifier.data.media.MediaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    mediaRepository: MediaRepository,
    onBack: () -> Unit,
    onImageDeleted: ((Uri) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 查詢所有儲存的圖片
    LaunchedEffect(Unit) {
        imageUris = mediaRepository.queryMagnifierImages()
    }

    // 刪除選中的圖片（透過 MediaRepository，per-URI fallback 邏輯封裝於實作層）
    fun deleteSelectedImages() {
        val toDelete = selectedUris
        coroutineScope.launch {
            val result = mediaRepository.delete(toDelete)
            result.deleted.forEach { uri -> onImageDeleted?.invoke(uri) }
            imageUris = mediaRepository.queryMagnifierImages()
            selectedUris = emptySet()
            isSelectionMode = false
            android.widget.Toast.makeText(
                context,
                if (result.deletedCount > 0) "已刪除 ${result.deletedCount} 張圖片"
                else "刪除失敗，請檢查權限",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) {
                            if (selectedUris.isEmpty()) "選擇照片" else "已選擇 ${selectedUris.size} 張"
                        } else {
                            "相簿"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedUris = emptySet()
                        } else if (selectedImageUri != null) {
                            // 如果正在查看圖片，只關閉圖片查看器
                            selectedImageUri = null
                        } else {
                            // 否則返回首頁
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode && selectedUris.isNotEmpty()) {
                        IconButton(onClick = { deleteSelectedImages() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "還沒有儲存任何圖片",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(imageUris) { uri ->
                    val isSelected = selectedUris.contains(uri)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(uri, isSelectionMode) {
                                detectTapGestures(
                                    onTap = {
                                        // 點擊處理
                                        if (isSelectionMode) {
                                            // 選擇模式下點擊切換選中狀態
                                            selectedUris = if (isSelected) {
                                                selectedUris - uri
                                            } else {
                                                selectedUris + uri
                                            }
                                        } else {
                                            // 普通模式下點擊查看大圖
                                            selectedImageUri = uri
                                        }
                                    },
                                    onLongPress = {
                                        // 長按進入選擇模式並勾選該圖片
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                        }
                                        selectedUris = selectedUris + uri
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "儲存的圖片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelectionMode) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isSelected) "已選中" else "未選中",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 全屏圖片查看器
        selectedImageUri?.let { uri ->
            ImageViewer(
                imageUri = uri,
                onClose = { selectedImageUri = null }
            )
        }
    }
}
