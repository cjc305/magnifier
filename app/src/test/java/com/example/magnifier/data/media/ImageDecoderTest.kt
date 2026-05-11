package com.example.magnifier.data.media

import android.graphics.ImageFormat
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * JVM unit tests for strategy selection only.
 *
 * Cannot test actual decode() in JVM tests because BitmapFactory / YuvImage
 * are Android framework calls that throw "Stub!" at runtime here. Decode
 * correctness is covered by user device smoke test (MAG-M-001 AC-4 passed
 * 2026-05-11). Future instrumented test → app/src/androidTest/.
 */
class ImageDecoderTest {

    @Test
    fun yuv_format_with_3_planes_selects_yuv() {
        assertSame(
            YuvDecoder,
            ImageDecoder.selectStrategy(ImageFormat.YUV_420_888, planeCount = 3)
        )
    }

    @Test
    fun jpeg_format_with_1_plane_selects_jpeg() {
        assertSame(
            JpegDecoder,
            ImageDecoder.selectStrategy(ImageFormat.JPEG, planeCount = 1)
        )
    }

    @Test
    fun yuv_format_but_only_1_plane_falls_back_to_first_plane() {
        // YuvDecoder requires planeCount >= 3; with 1 plane the fallback wins.
        assertSame(
            FirstPlaneFallbackDecoder,
            ImageDecoder.selectStrategy(ImageFormat.YUV_420_888, planeCount = 1)
        )
    }

    @Test
    fun unknown_format_with_planes_selects_fallback() {
        assertSame(
            FirstPlaneFallbackDecoder,
            ImageDecoder.selectStrategy(format = 9999, planeCount = 1)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun zero_planes_throws() {
        ImageDecoder.selectStrategy(format = ImageFormat.JPEG, planeCount = 0)
    }

    @Test
    fun jpeg_format_with_3_planes_still_uses_jpeg_not_yuv() {
        // Strategy order: YUV → JPEG → fallback. JPEG format ≠ YUV_420_888, so YUV's matches()
        // returns false. JPEG matches because format==JPEG && planeCount>=1.
        assertSame(
            JpegDecoder,
            ImageDecoder.selectStrategy(ImageFormat.JPEG, planeCount = 3)
        )
    }
}
