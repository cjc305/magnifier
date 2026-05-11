---
doc: magnifier/audit/2026-05-11/MAG-A-001
title: 抽 MagnifierViewModel — state holder 從 Composable 拉出
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T19:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-A-001: 抽 MagnifierViewModel — state holder 從 Composable 拉出

```yaml
id: MAG-A-001
severity: p1
status: partial
owner: claude
eta_days: 1
blocker_for: [MAG-A-002]
discovered_via: grep 'mutableStateOf\|remember' MainActivity.kt → state 全在 Composable 內
fixed_in: 0cb3058+1d81ede+a44279c
related: [MAG-API-001, MAG-API-002, MAG-API-003, MAG-D-003]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 657-665（MagnifierScreen 內的 state）

var hasCameraPermission by remember { mutableStateOf(false) }
var hasStoragePermission by remember { mutableStateOf(false) }
var zoomLevel by remember { mutableFloatStateOf(4f) }
var isFlashOn by remember { mutableStateOf(false) }
var camera: Camera? by remember { mutableStateOf(null) }
var lastSavedImageUri by remember { mutableStateOf<Uri?>(null) }
var showGallery by remember { mutableStateOf(false) }
var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
```

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 120-123（GalleryScreen 內的 state）

var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
var isSelectionMode by remember { mutableStateOf(false) }
var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
```

**後果**：
- 螢幕旋轉 → state 全失（`remember` 而非 `rememberSaveable`）
- 進背景 → app 被殺重啟後狀態歸零
- 無法測試 state 轉移邏輯（綁死在 Compose runtime）
- 業務邏輯（拍照後設 `lastSavedImageUri`）與 UI 渲染交織

### Fix

```kotlin
// FILE: app/src/main/java/com/example/magnifier/ui/magnifier/MagnifierViewModel.kt
// LINES: new file
// COMMIT: refactor(MAG-A-001): extract MagnifierViewModel

class MagnifierViewModel(
    private val mediaRepository: MediaRepository,           // 見 MAG-API-001
    private val cameraController: CameraController,         // 見 MAG-API-002
) : ViewModel() {

    private val _uiState = MutableStateFlow(MagnifierUiState())
    val uiState: StateFlow<MagnifierUiState> = _uiState.asStateFlow()

    fun setZoom(level: Float) {
        _uiState.update { it.copy(zoomLevel = level.coerceIn(1f, 10f)) }
        cameraController.setZoom(level)
    }

    fun toggleFlash() {
        val next = !_uiState.value.isFlashOn
        _uiState.update { it.copy(isFlashOn = next) }
        cameraController.setTorch(next)
    }

    fun capture() {
        viewModelScope.launch {
            cameraController.capture()
                .onSuccess { bitmap ->
                    mediaRepository.save(bitmap)
                        .onSuccess { uri ->
                            _uiState.update { it.copy(lastSavedImageUri = uri) }
                        }
                        .onFailure { /* emit UI event */ }
                }
        }
    }

    fun showGallery(show: Boolean) {
        _uiState.update { it.copy(showGallery = show) }
    }
}

data class MagnifierUiState(
    val zoomLevel: Float = 4f,
    val isFlashOn: Boolean = false,
    val lastSavedImageUri: Uri? = null,
    val showGallery: Boolean = false,
)
```

Composable 收斂為純 UI：
```kotlin
@Composable
fun MagnifierScreen(viewModel: MagnifierViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MagnifierContent(
        state = state,
        onZoomChange = viewModel::setZoom,
        onFlashToggle = viewModel::toggleFlash,
        onCapture = viewModel::capture,
        onGalleryOpen = { viewModel.showGallery(true) },
    )
}
```

GalleryScreen 同理抽 `GalleryViewModel`。

### Acceptance criteria

- [x] AC-1: `MagnifierScreen` Composable 內無 `mutableStateOf` 業務 state — `grep mutableStateOf MagnifierScreen.kt` exit 1
- [x] AC-2: `MagnifierViewModel` 100% pure Kotlin — `grep "import androidx.compose" MagnifierViewModel.kt` exit 1（僅 import androidx.lifecycle.* 與 kotlinx.coroutines.*）
- [ ] AC-3: 旋轉螢幕後 zoom level / flash 狀態保留 — **待用戶實機驗證**（架構面 AppContainer + ViewModelStoreOwner 已確保 process-scoped 依賴 + cross-rotation VM survival）
- [x] AC-4: 寫 `MagnifierViewModelTest` 至少 4 個 case — **7 cases 全綠**：setZoom clamp / toggleFlash / capture success / capture failure (controller) / capture-but-save-fails / onImagesDeleted match / onImagesDeleted no-match
- [x] AC-5: GalleryScreen 同樣抽出 GalleryViewModel — GalleryViewModel.kt 86 行 + UiState + Factory；selection / delete / viewer 全部 state 移入

實作偏離原 spec 的兩處：
1. PermanentlyDenied permission 偵測需要 Activity ref，本次未做（MAG-API-003 AC-3 deferred）
2. 事件流改用 `Channel<UiEvent>` 而非 `SharedFlow`，因為 SharedFlow(replay=0) 對 rotation/test 都有「subscriber 不在時 emit 丟失」問題

### Verification

```bash
# 1. Composable 內無業務 mutableStateOf
grep -n "mutableStateOf\|mutableFloatStateOf" \
  app/src/main/java/com/example/magnifier/ui/magnifier/MagnifierScreen.kt
# Expected: 無，或僅限 transient UI state（如 slider drag）

# 2. ViewModel 不引 Compose framework
grep "import androidx.compose" \
  app/src/main/java/com/example/magnifier/ui/magnifier/MagnifierViewModel.kt
# Expected: 無輸出

# 3. 測試通過
./gradlew :app:testDebugUnitTest --tests "*MagnifierViewModelTest"
# Expected: BUILD SUCCESSFUL，4 個 case 通過

# 4. 旋轉螢幕測試（手動）
# - 開 app → 滑桿到 7x → 開手電筒
# - 旋轉螢幕
# - 確認：zoom 仍 7x、手電筒仍亮
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | CameraX `Camera` / `ImageCapture` 物件非 Parcelable，無法直接放 ViewModel state | 把它們放 `CameraController`（MAG-API-002）的內部 field，不進 ViewModel state |
| R-2 | 沒 DI 時 ViewModel 建構需要手動傳 Repository / Controller | 用 `ViewModelProvider.Factory` 或等 MAG-D-003 引入 Hilt |
| R-3 | StateFlow `collectAsStateWithLifecycle` 需要 `lifecycle-runtime-compose` 依賴 | 加 `androidx.lifecycle:lifecycle-runtime-compose:2.10.0` 到 dependencies |

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：旋轉螢幕又會掉 state（退回到改前的 UX）
# 副作用：所有依賴 ViewModel 的後續 issue（MAG-A-002 + MAG-API-*）需重新評估
```
