package com.example.magnifier.ui.camera

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    zoomLevel: Float,
    isFlashOn: Boolean,
    camera: Camera?,
    onZoomChange: (Float) -> Unit,
    onCameraReady: (Camera, ImageCapture, ProcessCameraProvider) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var currentCamera: Camera? by remember { mutableStateOf(null) }

    // 初始化相機
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView?.surfaceProvider)
        }

        val imageCapture = ImageCapture.Builder()
            .setFlashMode(
                if (isFlashOn) ImageCapture.FLASH_MODE_ON
                else ImageCapture.FLASH_MODE_OFF
            )
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            currentCamera = camera
            // 相機初始化完成後立即應用初始縮放值
            camera.cameraControl.setZoomRatio(zoomLevel)
            onCameraReady(camera, imageCapture, cameraProvider)
        } catch (e: Exception) {
            Log.e("CameraPreview", "相機初始化失敗", e)
        }
    }

    // 更新縮放 - 使用 remember 來避免不必要的更新
    var lastZoomLevel by remember { mutableFloatStateOf(zoomLevel) }
    LaunchedEffect(zoomLevel, currentCamera) {
        if (currentCamera != null && kotlin.math.abs(zoomLevel - lastZoomLevel) > 0.01f) {
            currentCamera?.cameraControl?.setZoomRatio(zoomLevel)
            lastZoomLevel = zoomLevel
        }
    }

    // 更新手電筒
    LaunchedEffect(isFlashOn) {
        currentCamera?.cameraControl?.enableTorch(isFlashOn)
    }

    // 更新相機引用
    LaunchedEffect(camera) {
        currentCamera = camera
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView = it }
        },
        modifier = modifier
    )
}
