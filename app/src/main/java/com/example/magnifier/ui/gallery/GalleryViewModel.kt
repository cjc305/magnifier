package com.example.magnifier.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private val _deletedImages = MutableSharedFlow<Set<Uri>>(extraBufferCapacity = 4)
    val deletedImages: SharedFlow<Set<Uri>> = _deletedImages.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(images = mediaRepository.queryMagnifierImages()) }
        }
    }

    fun toggleSelection(uri: Uri) {
        _uiState.update {
            val next = if (uri in it.selectedUris) it.selectedUris - uri else it.selectedUris + uri
            it.copy(selectedUris = next)
        }
    }

    fun enterSelectionMode(initial: Uri) {
        _uiState.update {
            it.copy(isSelectionMode = true, selectedUris = it.selectedUris + initial)
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedUris = emptySet()) }
    }

    fun openViewer(uri: Uri) {
        _uiState.update { it.copy(viewerUri = uri) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(viewerUri = null) }
    }

    fun deleteSelected() {
        val toDelete = _uiState.value.selectedUris
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            val result = mediaRepository.delete(toDelete)
            if (result.deleted.isNotEmpty()) {
                _deletedImages.emit(result.deleted)
            }
            _uiState.update {
                it.copy(
                    images = mediaRepository.queryMagnifierImages(),
                    isSelectionMode = false,
                    selectedUris = emptySet(),
                )
            }
            val msg = if (result.deletedCount > 0) {
                "已刪除 ${result.deletedCount} 張圖片"
            } else {
                "刪除失敗，請檢查權限"
            }
            _events.trySend(UiEvent.ShowToast(msg))
        }
    }
}
