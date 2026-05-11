package com.example.magnifier.data.camera

import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

interface CameraController {
    fun setSurfaceProvider(provider: Preview.SurfaceProvider)
    suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit>
    fun setZoom(ratio: Float)
    fun setTorch(enabled: Boolean)
    suspend fun capture(): Result<Bitmap>
    fun release()
}
