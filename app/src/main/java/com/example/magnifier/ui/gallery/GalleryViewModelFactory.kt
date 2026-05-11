package com.example.magnifier.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.magnifier.data.media.MediaRepository

class GalleryViewModelFactory(
    private val mediaRepository: MediaRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return GalleryViewModel(mediaRepository) as T
    }
}
