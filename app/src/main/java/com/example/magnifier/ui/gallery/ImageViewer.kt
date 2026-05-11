package com.example.magnifier.ui.gallery

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImageViewer(
    imageUri: Uri,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 處理系統返回按鈕，只關閉圖片查看器，不返回到首頁
    BackHandler(onBack = onClose)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // 雙擊重置縮放和位置
                        scale = 1f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        // 背景
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black
        ) {}

        // 圖片（支持縮放和拖動）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "查看圖片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        // 頂部操作欄（放在最後以確保在最上層，並阻止圖片層攔截觸摸事件）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // 阻止觸摸事件傳遞到圖片層
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // 返回按鈕（左上角）
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
