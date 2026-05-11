package com.example.magnifier.ui.gallery

import android.net.Uri

data class GalleryUiState(
    val images: List<Uri> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedUris: Set<Uri> = emptySet(),
    val viewerUri: Uri? = null,
)
