package com.example.magnifier.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.magnifier.data.media.ImageDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private const val TAG = "CameraXController"

class CameraXController(
    private val context: Context,
    private val decode: (ImageProxy) -> Bitmap = { ImageDecoder.decode(it) },
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context),
) : CameraController {

    private val preview: Preview = Preview.Builder().build()
    private val imageCapture: ImageCapture = ImageCapture.Builder().build()
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun setSurfaceProvider(provider: Preview.SurfaceProvider) {
        preview.setSurfaceProvider(provider)
    }

    override suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit> {
        return try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
            cameraProvider = provider
            Result.success(Unit)
        } catch (e: CancellationException) {
            // 結構化併發要求 cancellation 必須往外丟，不可吞掉
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "相機綁定失敗", e)
            Result.failure(e)
        }
    }

    override fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1f, 10f))
    }

    override fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    override suspend fun capture(): Result<Bitmap> = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val outcome = runCatching { decode(image) }
                    image.close()
                    cont.resume(outcome)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失敗", exception)
                    cont.resume(Result.failure(exception))
                }
            },
        )
    }

    override fun release() {
        cameraProvider?.unbindAll()
        camera = null
        cameraProvider = null
    }
}
