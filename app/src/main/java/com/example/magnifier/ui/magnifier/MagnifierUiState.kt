package com.example.magnifier.ui.magnifier

import android.net.Uri

data class MagnifierUiState(
    val zoomLevel: Float = 4f,
    val isFlashOn: Boolean = false,
    val lastSavedImageUri: Uri? = null,
    val showGallery: Boolean = false,
)
