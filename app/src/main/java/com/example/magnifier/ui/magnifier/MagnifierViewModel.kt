package com.example.magnifier.ui.magnifier

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.magnifier.data.camera.CameraController
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.data.permission.AppPermissions
import com.example.magnifier.data.permission.PermissionGate
import com.example.magnifier.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MagnifierViewModel(
    private val mediaRepository: MediaRepository,
    private val cameraController: CameraController,
    private val permissionGate: PermissionGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MagnifierUiState())
    val uiState: StateFlow<MagnifierUiState> = _uiState.asStateFlow()

    val permissions: StateFlow<AppPermissions> = permissionGate.state

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun setZoom(level: Float) {
        val clamped = level.coerceIn(1f, 10f)
        _uiState.update { it.copy(zoomLevel = clamped) }
        cameraController.setZoom(clamped)
    }

    fun toggleFlash() {
        val next = !_uiState.value.isFlashOn
        _uiState.update { it.copy(isFlashOn = next) }
        cameraController.setTorch(next)
    }

    fun capture() {
        viewModelScope.launch {
            cameraController.capture()
                .onSuccess { bitmap ->
                    mediaRepository.save(bitmap)
                        .onSuccess { uri ->
                            _uiState.update { it.copy(lastSavedImageUri = uri) }
                            _events.trySend(UiEvent.ShowToast("圖片已儲存到相簿"))
                        }
                        .onFailure {
                            _events.trySend(UiEvent.ShowToast("儲存失敗，請檢查權限"))
                        }
                }
                .onFailure { e ->
                    _events.trySend(UiEvent.ShowToast("拍照失敗: ${e.message}"))
                }
        }
    }

    fun showGallery(show: Boolean) {
        _uiState.update { it.copy(showGallery = show) }
    }

    /** Called when GalleryScreen returns; verifies the last-saved thumbnail still exists. */
    fun onGalleryReturn() {
        val uri = _uiState.value.lastSavedImageUri ?: return
        viewModelScope.launch {
            val allImages = mediaRepository.queryMagnifierImages()
            if (uri !in allImages) {
                _uiState.update { it.copy(lastSavedImageUri = null) }
            }
        }
    }

    /** Called by GalleryViewModel (via Composable wiring) after images are deleted. */
    fun onImagesDeleted(deleted: Set<Uri>) {
        val current = _uiState.value.lastSavedImageUri
        if (current != null && current in deleted) {
            _uiState.update { it.copy(lastSavedImageUri = null) }
        }
    }

    // Permission result delegation — launchers stay in Composable (Compose API),
    // results funnel through here to keep the gate the single source of truth.
    fun onCameraPermissionResult(granted: Boolean) = permissionGate.onCameraResult(granted)
    fun onMediaPermissionResult(results: Map<String, Boolean>) =
        permissionGate.onMediaResult(results)

    fun mediaPermissionList(): List<String> = permissionGate.mediaPermissions()
}
