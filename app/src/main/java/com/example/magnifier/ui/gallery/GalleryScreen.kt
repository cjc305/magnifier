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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.ui.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    mediaRepository: MediaRepository,
    onBack: () -> Unit,
    onImagesDeleted: (Set<Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel(
        factory = remember(mediaRepository) { GalleryViewModelFactory(mediaRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

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

    // 把刪除事件廣播給父層（讓 MagnifierViewModel 清掉縮圖）
    LaunchedEffect(viewModel) {
        viewModel.deletedImages.collect { uris -> onImagesDeleted(uris) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isSelectionMode) {
                            if (uiState.selectedUris.isEmpty()) "選擇照片"
                            else "已選擇 ${uiState.selectedUris.size} 張"
                        } else {
                            "相簿"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            uiState.isSelectionMode -> viewModel.exitSelectionMode()
                            uiState.viewerUri != null -> viewModel.closeViewer()
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode && uiState.selectedUris.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
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
        if (uiState.images.isEmpty()) {
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
                items(uiState.images) { uri ->
                    val isSelected = uri in uiState.selectedUris
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(uri, uiState.isSelectionMode) {
                                detectTapGestures(
                                    onTap = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(uri)
                                        } else {
                                            viewModel.openViewer(uri)
                                        }
                                    },
                                    onLongPress = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode(uri)
                                        } else {
                                            viewModel.toggleSelection(uri)
                                        }
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
                        if (uiState.isSelectionMode) {
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

        uiState.viewerUri?.let { uri ->
            ImageViewer(
                imageUri = uri,
                onClose = viewModel::closeViewer
            )
        }
    }
}
