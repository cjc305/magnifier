package com.example.magnifier.ui.camera

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.magnifier.data.camera.CameraController

@Composable
fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { view ->
                controller.setSurfaceProvider(view.surfaceProvider)
            }
        },
    )

    LaunchedEffect(controller, lifecycleOwner) {
        controller.bind(lifecycleOwner)
            .onFailure { e -> Log.e("CameraPreview", "bind failed", e) }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
}
