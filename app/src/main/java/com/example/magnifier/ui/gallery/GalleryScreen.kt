package com.example.magnifier.ui.gallery

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.magnifier.data.media.queryMagnifierImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onImageDeleted: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 查詢所有儲存的圖片
    LaunchedEffect(Unit) {
        imageUris = queryMagnifierImages(context)
    }

    // 刪除選中的圖片
    fun deleteSelectedImages() {
        val count = selectedUris.size
        var deletedCount = 0
        selectedUris.forEach { uri ->
            try {
                var deleted = false

                // 嘗試使用 DocumentsContract 刪除（適用於 document URI）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    DocumentsContract.isDocumentUri(context, uri)) {
                    try {
                        deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                        if (deleted) {
                            Log.d("Magnifier", "使用 DocumentsContract 刪除圖片: $uri")
                        }
                    } catch (e: Exception) {
                        Log.d("Magnifier", "DocumentsContract 刪除失敗，嘗試其他方法: $uri", e)
                    }
                }

                // 如果 DocumentsContract 失敗，嘗試使用 MediaStore 刪除
                if (!deleted) {
                    val result = context.contentResolver.delete(uri, null, null)
                    if (result > 0) {
                        deleted = true
                        Log.d("Magnifier", "使用 MediaStore 刪除圖片: $uri")
                    } else {
                        Log.w("Magnifier", "刪除返回 0，可能圖片不存在或無權限: $uri")
                    }
                }

                if (deleted) {
                    deletedCount++
                    Log.d("Magnifier", "已刪除圖片: $uri")
                    // 通知父組件圖片已被刪除
                    onImageDeleted?.invoke(uri)
                } else {
                    Log.e("Magnifier", "無法刪除圖片: $uri")
                }
            } catch (e: SecurityException) {
                Log.e("Magnifier", "刪除圖片權限不足: $uri", e)
            } catch (e: Exception) {
                Log.e("Magnifier", "刪除圖片失敗: $uri", e)
            }
        }
        // 重新查詢圖片列表
        imageUris = queryMagnifierImages(context)
        selectedUris = emptySet()
        isSelectionMode = false
        android.widget.Toast.makeText(
            context,
            if (deletedCount > 0) "已刪除 $deletedCount 張圖片" else "刪除失敗，請檢查權限",
            android.widget.Toast.LENGTH_SHORT
        ).show()
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
