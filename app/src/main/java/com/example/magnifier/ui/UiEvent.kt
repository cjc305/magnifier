package com.example.magnifier.ui

/**
 * One-shot events emitted by ViewModels to the UI layer.
 * Composables collect from a SharedFlow and translate to Toasts / Snackbars.
 */
sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}
