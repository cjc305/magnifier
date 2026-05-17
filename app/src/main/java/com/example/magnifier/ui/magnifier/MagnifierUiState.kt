package com.example.magnifier.ui.magnifier

import android.net.Uri

data class MagnifierUiState(
    val zoomLevel: Float = 2f,
    val maxZoom: Float = 4f,           // refreshed from CameraController on bind
    val isFlashOn: Boolean = false,
    val lastSavedImageUri: Uri? = null,
    val showGallery: Boolean = false,
)
