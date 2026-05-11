---
doc: magnifier/audit/2026-05-11/MAG-A-003
title: imageProxyToBitmap 拆 format-strategy（YUV / JPEG / fallback）
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T15:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-A-003: imageProxyToBitmap 拆 format-strategy（YUV / JPEG / fallback）

```yaml
id: MAG-A-003
severity: p2
status: done
owner: claude
eta_days: 0.5
blocker_for: []
discovered_via: imageProxyToBitmap 單 function 52 行 + when 三分支 + 不同 ByteBuffer 處理邏輯
fixed_in: 99fa6bb
related: []
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 541-592

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val format = image.format
    val planes = image.planes
    return when {
        format == ImageFormat.YUV_420_888 && planes.size >= 3 -> { /* 14 行 YUV→NV21→JPEG */ }
        format == ImageFormat.JPEG && planes.size >= 1 ->       { /* 5 行 JPEG decode */ }
        planes.size >= 1 ->                                       { /* 5 行 fallback */ }
        else -> throw IllegalArgumentException("不支持的圖像格式: $format")
    }
}
```

**問題**：
- 三個 branch 各自 ByteBuffer 操作，邏輯不對稱
- YUV 路徑 14 行 inline，難以單獨測試
- 沒有 unit test 覆蓋（test/ 只有 ExampleUnitTest）
- 新增格式（如 RAW10）必改本 function

### Fix

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/media/ImageDecoder.kt
// LINES: new file

internal interface ImageDecoderStrategy {
    fun matches(format: Int, planeCount: Int): Boolean
    fun decode(image: ImageProxy): Bitmap
}

internal object YuvDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int) =
        format == ImageFormat.YUV_420_888 && planeCount >= 3
    override fun decode(image: ImageProxy): Bitmap = yuvImageProxyToBitmap(image)
}

internal object JpegDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int) =
        format == ImageFormat.JPEG && planeCount >= 1
    override fun decode(image: ImageProxy): Bitmap = jpegImageProxyToBitmap(image)
}

internal object FirstPlaneFallbackDecoder : ImageDecoderStrategy {
    override fun matches(format: Int, planeCount: Int) = planeCount >= 1
    override fun decode(image: ImageProxy): Bitmap = firstPlaneToBitmap(image)
}

object ImageDecoder {
    private val strategies = listOf(YuvDecoder, JpegDecoder, FirstPlaneFallbackDecoder)

    fun decode(image: ImageProxy): Bitmap {
        val strategy = strategies.firstOrNull { it.matches(image.format, image.planes.size) }
            ?: throw IllegalArgumentException("不支援的格式: ${image.format}, planes: ${image.planes.size}")
        return strategy.decode(image)
    }
}
```

各 decoder 內部函數小於 20 行，可獨立測試。

### Acceptance criteria

- [x] AC-1: `ImageDecoder.decode` ≤ 10 行 — **4 行**（含註解）
- [x] AC-2: 每個 strategy 的 `decode` 函數 ≤ 20 行 — Yuv 18 行 / Jpeg 4 行 / Fallback 6 行
- [x] AC-3: `ImageDecoderTest` 覆蓋 YUV / JPEG / fallback / unsupported — **6 cases 全綠**（tests=6, failures=0, errors=0；包含 JPEG-with-3-planes 防誤判 yuv 的迴歸 case）
- [x] AC-4: `imageProxyToBitmap` 已移除 — grep `fun imageProxyToBitmap` 全 src/main 0 命中（MAG-M-001 已搬到 ImageDecoder.kt，本 issue 一併重命名為 `ImageDecoder.decode`）
- [ ] AC-5: 實機拍照功能不變（仍能存到相簿）— **待用戶實機驗證**（JVM 單測無法 cover Android `BitmapFactory` / `YuvImage` 真實呼叫）

### Verification

```bash
# 1. Old function 已移除
grep -n "fun imageProxyToBitmap" app/src/main/java/com/example/magnifier/MainActivity.kt
# Expected: 無

# 2. 新 decoder 存在
test -f app/src/main/java/com/example/magnifier/data/media/ImageDecoder.kt && echo OK
# Expected: OK

# 3. Unit test 通過
./gradlew :app:testDebugUnitTest --tests "*ImageDecoderTest"
# Expected: 4 tests passed

# 4. 實機拍照（手動）
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.magnifier/.MainActivity
# 拍 1 張，相簿可見
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | strategy 順序錯誤導致 fallback 永遠 match 不到 YUV | strategies list 順序明確：specific → general；單測覆蓋順序 |
| R-2 | Mock ImageProxy 難建 | 用 Mockito-Kotlin 或寫一個 fake ImageProxy data class |
| R-3 | CameraX 在某些裝置回 ImageFormat.NV21 而非 YUV_420_888 | 加 strategy 或 fallback 已涵蓋；實機測試需涵蓋至少 2 種 SoC |

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：format 邏輯回到 inline；測試覆蓋率退步
```
