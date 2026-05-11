package com.example.magnifier.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream

fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap {
    // 檢查圖像格式
    val format = image.format
    val planes = image.planes

    return when {
        // YUV 格式（通常有 3 個 planes）
        format == ImageFormat.YUV_420_888 && planes.size >= 3 -> {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height),
                100,
                out
            )
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        // JPEG 格式（通常只有 1 個 plane）
        format == ImageFormat.JPEG && planes.size >= 1 -> {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        // 其他格式或未知格式，嘗試使用第一個 plane
        planes.size >= 1 -> {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("無法解碼圖像格式: $format")
        }
        else -> {
            throw IllegalArgumentException("不支持的圖像格式: $format, planes: ${planes.size}")
        }
    }
}
