package com.example.magnifier.data.camera

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

interface CameraController {
    /**
     * Max digital zoom ratio supported by the currently bound camera.
     * Defaults to a conservative 1f until bind() succeeds; UI code should
     * use this to set slider upper bound — sending setZoom() above this
     * value will be silently ignored by CameraX on the device side.
     */
    val maxZoomRatio: StateFlow<Float>

    fun setSurfaceProvider(provider: Preview.SurfaceProvider)
    suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit>
    fun setZoom(ratio: Float)
    fun setTorch(enabled: Boolean)
    suspend fun capture(): Result<Bitmap>
    fun release()
}
