package com.example.magnifier.ui.gallery

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.magnifier.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    mediaRepository: MediaRepository,
    onBack: () -> Unit,
    onImagesDeleted: (Set<Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val viewModel: GalleryViewModel = viewModel(
        factory = remember(mediaRepository) { GalleryViewModelFactory(mediaRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(viewModel) {
        viewModel.deletedImages.collect { uris -> onImagesDeleted(uris) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState.isSelectionMode && uiState.selectedUris.isEmpty() -> "選擇照片"
                            uiState.isSelectionMode -> "已選擇 ${uiState.selectedUris.size} 張"
                            else -> "相簿"
                        },
                        style = MaterialTheme.typography.titleLarge,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode && uiState.selectedUris.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除已選擇的照片",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { paddingValues ->
        if (uiState.images.isEmpty()) {
            EmptyGalleryState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(uiState.images) { uri ->
                    val isSelected = uri in uiState.selectedUris
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(spacing.sm))
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
                            contentScale = ContentScale.Crop,
                        )
                        if (uiState.isSelectionMode) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isSelected) "已選中" else "未選中",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(spacing.sm)
                                    .size(24.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }

        uiState.viewerUri?.let { uri ->
            ImageViewer(imageUri = uri, onClose = viewModel::closeViewer)
        }
    }
}

@Composable
private fun EmptyGalleryState(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "還沒有儲存任何照片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "回到主畫面按拍照鈕,儲存的放大畫面會出現在這裡",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
