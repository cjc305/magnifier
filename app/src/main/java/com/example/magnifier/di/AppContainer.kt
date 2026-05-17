package com.example.magnifier.di

import android.content.Context
import com.example.magnifier.data.camera.CameraController
import com.example.magnifier.data.camera.CameraXController
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.data.media.MediaStoreMediaRepository
import com.example.magnifier.data.permission.AndroidPermissionGate
import com.example.magnifier.data.permission.PermissionGate
import com.example.magnifier.data.settings.ThemePreferences

/**
 * Manual DI container scoped to the Application instance.
 *
 * Created once when MagnifierApplication.container is first accessed; survives
 * Activity recreation (rotation) so ViewModels keep referencing the same
 * stateful dependencies (esp. CameraXController's CameraProvider binding).
 *
 * Will be replaced by Hilt or Koin in MAG-D-003 if app scope grows.
 */
class AppContainer(applicationContext: Context) {
    val mediaRepository: MediaRepository by lazy { MediaStoreMediaRepository(applicationContext) }
    val cameraController: CameraController by lazy { CameraXController(applicationContext) }
    val permissionGate: PermissionGate by lazy { AndroidPermissionGate(applicationContext) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(applicationContext) }
}
