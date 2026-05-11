package com.example.magnifier.ui.magnifier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.magnifier.data.camera.CameraController
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.data.permission.PermissionGate

class MagnifierViewModelFactory(
    private val mediaRepository: MediaRepository,
    private val cameraController: CameraController,
    private val permissionGate: PermissionGate,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MagnifierViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MagnifierViewModel(mediaRepository, cameraController, permissionGate) as T
    }
}
