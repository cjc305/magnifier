---
doc: magnifier/audit/2026-05-11/MAG-API-002
title: CameraController 介面 — CameraX 生命週期 + zoom + torch
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T17:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-API-002: CameraController 介面 — CameraX 生命週期 + zoom + torch

```yaml
id: MAG-API-002
severity: p1
status: partial
owner: claude
eta_days: 1
blocker_for: [MAG-A-001]
discovered_via: CameraX 三大操作（綁定 / zoom / torch / capture）散在 CameraPreview L465 + MagnifierScreen L789 takePicture
fixed_in: 65840a3
related: [MAG-A-001, MAG-A-002]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt

L465-539  CameraPreview Composable
          內含 ProcessCameraProvider.getInstance + bindToLifecycle
          內含 LaunchedEffect 監聽 zoomLevel 改變
          內含 LaunchedEffect 監聽 isFlashOn 改變
          camera.cameraControl.setZoomRatio / enableTorch 直接呼叫

L789-836  IconButton(拍照儲存) onClick lambda
          imageCapture.takePicture(executor, ImageCapture.OnImageCapturedCallback {
              onCaptureSuccess { image ->
                  val bitmap = imageProxyToBitmap(image)
                  val savedUri = saveImageToGallery(context, bitmap)
                  ...
              }
          })
```

**問題**：
- CameraX 物件（`Camera`、`ImageCapture`、`ProcessCameraProvider`）以 `mutableStateOf` 存在 Composable 內，生命週期混亂
- `cameraControl.setZoomRatio` 直接在 Composable 內呼叫，UI 與硬體強耦合
- 拍照流程 = `takePicture` callback hell + `imageProxyToBitmap` + `saveImageToGallery`，混在 IconButton 內
- 無法 mock CameraX 進行單測

### Fix

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/camera/CameraController.kt
// LINES: new file

interface CameraController {
    val previewSurfaceProvider: Preview.SurfaceProvider?

    suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit>
    fun setZoom(ratio: Float)
    fun setTorch(enabled: Boolean)
    suspend fun capture(): Result<Bitmap>
    fun release()
}
```

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/camera/CameraXController.kt
// LINES: new file

class CameraXController(
    private val context: Context,
    private val imageDecoder: ImageDecoder = ImageDecoder,    // 見 MAG-A-003
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context),
) : CameraController {

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val preview = Preview.Builder().build()

    override val previewSurfaceProvider get() = preview.surfaceProvider

    override suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit> = runCatching {
        val provider = ProcessCameraProvider.getInstance(context).await()
        val capture = ImageCapture.Builder().build()
        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
        )
        imageCapture = capture
        cameraProvider = provider
    }

    override fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1f, 10f))
    }

    override fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    override suspend fun capture(): Result<Bitmap> = suspendCancellableCoroutine { cont ->
        val capture = imageCapture ?: return@suspendCancellableCoroutine cont.resume(
            Result.failure(IllegalStateException("Camera not bound"))
        )
        capture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                runCatching { imageDecoder.decode(image) }
                    .also { image.close() }
                    .let { cont.resume(it) }
            }
            override fun onError(exception: ImageCaptureException) {
                cont.resume(Result.failure(exception))
            }
        })
    }

    override fun release() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        cameraProvider = null
    }
}
```

UI 層收斂為：
```kotlin
// CameraPreview 簡化成
@Composable
fun CameraPreview(controller: CameraController, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { controller.bind(lifecycleOwner) }
    DisposableEffect(Unit) { onDispose { controller.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PreviewView(ctx).also { it.surfaceProvider = controller.previewSurfaceProvider } },
    )
}
```

API 設計遵循 `~/.claude/rules/api-design.md`：
- 命名動詞為主：`bind / setZoom / setTorch / capture / release`
- 錯誤統一用 `Result<T>` envelope
- 生命週期顯式：`bind` 對應 `release`（不靠 GC 隨便清）

### Acceptance criteria

- [x] AC-1: `CameraController` interface 與 `CameraXController` impl 分離 — `data/camera/CameraController.kt`(14) + `CameraXController.kt`(91) (65840a3)
- [x] AC-2: `CameraPreview` Composable ≤ 30 行 — 整檔 37 行（含 imports）；composable function body ~23 行
- [x] AC-3: 無 `cameraControl.` 直接呼叫在 UI 層 — `grep cameraControl\. app/src/main/.../ui/` exit 1 (0 命中)
- [x] AC-4: `bind` / `release` 配對，DisposableEffect 處理 — CameraPreview.kt 有 `LaunchedEffect(controller, lifecycleOwner) { bind(...) }` 配 `DisposableEffect(controller) { onDispose { release() } }`
- [ ] AC-5: 寫 `FakeCameraController` 供 Compose Preview 使用 — **deferred**：本次重構未做 Preview 範本，留作後續工作（介面已抽好，加 fake 是純機械作業）
- [x] AC-6: 實機跑 app，預覽 / zoom / 手電筒 / 拍照全部正常；切後台再回前台 preview 不掛 — 用戶 2026-05-11 smoke test 全綠（含 Home 鍵出入 + 進出相簿 + bind/release 配對驗證）
- [x] AC-7: capture 失敗 UI 不 crash — 實機 smoke 期間無 crash；onFailure 路徑寫 Toast 已在 code 確認

### Verification

```bash
# 1. interface 存在
test -f app/src/main/java/com/example/magnifier/data/camera/CameraController.kt && echo OK

# 2. UI 層無 cameraControl 直接呼叫
grep -rn "cameraControl\." app/src/main/java/com/example/magnifier/ui/
# Expected: 無輸出

# 3. lifecycle 配對（每個 bind 都有對應 release）
grep -rn "controller.bind\|controller.release" app/src/main/java/com/example/magnifier/ui/
# Expected: 各 1 處（在 LaunchedEffect / DisposableEffect 內）

# 4. 手動 smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 開 app → zoom 1→10 順暢 → 手電筒切換 → 拍 3 張存檔 → 切後台 → 回前台 → 預覽不黑屏
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | `ProcessCameraProvider.getInstance(context).await()` 需要 `androidx.concurrent:concurrent-futures-ktx` | 加 dep；或用 `.addListener` + suspendCancellableCoroutine 自行包裝 |
| R-2 | release 時 camera 還在 capture 中 → ImageProxy 漏 close | `bind/release` 內加 mutex；release 前先 `unbindAll()` 由 CameraX 處理 |
| R-3 | DisposableEffect 在 recomposition 時連帶 release → 預覽閃爍 | 用 `key = Unit` 確保只 release 在 leaving composition |

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：CameraX 邏輯回到 inline；ViewModel 抽取 (MAG-A-001) 需要另尋抽象
```
