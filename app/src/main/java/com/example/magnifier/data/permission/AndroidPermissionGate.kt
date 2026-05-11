package com.example.magnifier.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidPermissionGate(private val context: Context) : PermissionGate {

    private val _state = MutableStateFlow(AppPermissions())
    override val state: StateFlow<AppPermissions> = _state.asStateFlow()

    init {
        refresh()
    }

    override fun mediaPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

    override fun onCameraResult(granted: Boolean) {
        _state.update {
            it.copy(camera = if (granted) PermissionState.Granted else PermissionState.Denied)
        }
    }

    override fun onMediaResult(results: Map<String, Boolean>) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                results[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        }
        _state.update {
            it.copy(mediaRead = if (granted) PermissionState.Granted else PermissionState.Denied)
        }
    }

    override fun refresh() {
        _state.value = AppPermissions(
            camera = readSinglePermissionState(Manifest.permission.CAMERA),
            mediaRead = readMediaPermissionState(),
        )
    }

    private fun readSinglePermissionState(permission: String): PermissionState =
        if (ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            PermissionState.Granted
        } else {
            PermissionState.NotRequested
        }

    private fun readMediaPermissionState(): PermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readSinglePermissionState(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            val read = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (read || write) PermissionState.Granted else PermissionState.NotRequested
        }
}
