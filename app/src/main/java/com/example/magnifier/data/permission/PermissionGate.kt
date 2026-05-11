package com.example.magnifier.data.permission

import kotlinx.coroutines.flow.StateFlow

sealed interface PermissionState {
    object NotRequested : PermissionState
    object Granted : PermissionState
    object Denied : PermissionState
}

data class AppPermissions(
    val camera: PermissionState = PermissionState.NotRequested,
    val mediaRead: PermissionState = PermissionState.NotRequested,
) {
    val cameraGranted: Boolean get() = camera is PermissionState.Granted
    val mediaReadGranted: Boolean get() = mediaRead is PermissionState.Granted
}

interface PermissionGate {
    val state: StateFlow<AppPermissions>

    /**
     * 該平台 API_LEVEL 對應的「讀媒體圖片」權限清單。
     * Tiramisu (33)+ → READ_MEDIA_IMAGES；其他 → READ_EXTERNAL_STORAGE + WRITE_EXTERNAL_STORAGE。
     */
    fun mediaPermissions(): List<String>

    fun onCameraResult(granted: Boolean)
    fun onMediaResult(results: Map<String, Boolean>)

    /** 從 PackageManager 重新讀取當前狀態（onResume 從設定回來時呼叫）。 */
    fun refresh()
}
