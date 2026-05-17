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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private const val TAG = "CameraXController"

// Fallback used until bind() reads the real maxZoomRatio from cameraInfo.
// Most devices' main back camera reports 1.0–4.0; ultra-wide phones go
// higher. Sending setZoomRatio() above the device max is silently dropped
// by CameraX — hence we MUST clamp to this dynamic value in the UI.
private const val FALLBACK_MAX_ZOOM = 4f

class CameraXController(
    private val context: Context,
    private val decode: (ImageProxy) -> Bitmap = { ImageDecoder.decode(it) },
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context),
) : CameraController {

    private val preview: Preview = Preview.Builder().build()
    private val imageCapture: ImageCapture = ImageCapture.Builder().build()
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val _maxZoomRatio = MutableStateFlow(FALLBACK_MAX_ZOOM)
    override val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()

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

            // Read the real device max zoom. zoomState is LiveData populated
            // synchronously by CameraX during bind on supported devices.
            val state = camera?.cameraInfo?.zoomState?.value
            val deviceMax = state?.maxZoomRatio
            if (deviceMax != null && deviceMax > 1f) {
                _maxZoomRatio.value = deviceMax
                Log.i(TAG, "Camera bound, maxZoomRatio=$deviceMax")
            } else {
                Log.w(TAG, "zoomState not ready post-bind, keeping fallback $FALLBACK_MAX_ZOOM")
            }
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
        val clamped = ratio.coerceIn(1f, _maxZoomRatio.value)
        camera?.cameraControl?.setZoomRatio(clamped)
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
