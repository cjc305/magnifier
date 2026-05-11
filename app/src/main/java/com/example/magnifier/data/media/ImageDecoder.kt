package com.example.magnifier.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

internal interface ImageDecoderStrategy {
    fun matches(format: Int, planeCount: Int): Boolean
    fun decode(image: ImageProxy): Bitmap
}

internal object YuvDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int): Boolean =
        format == ImageFormat.YUV_420_888 && planeCount >= 3

    override fun decode(image: ImageProxy): Bitmap {
        val planes = image.planes
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
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}

internal object JpegDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int): Boolean =
        format == ImageFormat.JPEG && planeCount >= 1

    override fun decode(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

internal object FirstPlaneFallbackDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int): Boolean = planeCount >= 1

    override fun decode(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("無法解碼圖像格式: ${image.format}")
    }
}

object ImageDecoder {
    // Order matters: specific (YUV, JPEG) before general (fallback).
    private val strategies: List<ImageDecoderStrategy> =
        listOf(YuvDecoder, JpegDecoder, FirstPlaneFallbackDecoder)

    fun decode(image: ImageProxy): Bitmap {
        val strategy = selectStrategy(image.format, image.planes.size)
        return strategy.decode(image)
    }

    internal fun selectStrategy(format: Int, planeCount: Int): ImageDecoderStrategy =
        strategies.firstOrNull { it.matches(format, planeCount) }
            ?: throw IllegalArgumentException("不支援的格式: $format, planes: $planeCount")
}
