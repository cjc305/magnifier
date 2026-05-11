---
doc: magnifier/audit/2026-05-11/MAG-A-004
title: GalleryScreen 內含 grid / selection / viewer 三 mode → 拆
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T00:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-A-004: GalleryScreen 內含 grid / selection / viewer 三 mode → 拆

```yaml
id: MAG-A-004
severity: p2
status: not_started
owner: claude
eta_days: 1
blocker_for: []
discovered_via: GalleryScreen 從 L113 到 L320 共 208 行，內含三 mode（瀏覽 / 選擇 / 全屏檢視）的 state 與 UI 邏輯
fixed_in: null
related: [MAG-A-001, MAG-A-002]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 113-320

GalleryScreen 內 state：
- L120  imageUris: List<Uri>                ← data
- L121  isSelectionMode: Boolean             ← UI mode 1
- L122  selectedUris: Set<Uri>               ← UI mode 1 derived
- L123  selectedImageUri: Uri?               ← UI mode 2

GalleryScreen 內 function：
- L131  deleteSelectedImages()               ← 50 行刪除邏輯（DocumentsContract + MediaStore fallback）
- L313  selectedImageUri?.let { ImageViewer(...) }  ← 嵌入全屏檢視

GalleryScreen 內 IconButton navigation logic：
- L200-216  返回邏輯分三層：選擇模式 / 檢視模式 / 一般模式
```

**問題**：
- 三個 mode 混在一個 Composable，每個 click 都要判斷當前 mode
- 刪除邏輯（DocumentsContract → MediaStore fallback）50 行 inline
- ImageViewer 嵌套在 GalleryScreen 內，難以獨立用於其他畫面（如：拍照後直接預覽）

### Fix

```text
ui/gallery/
├── GalleryScreen.kt                     // organism — top-level navigation between modes
├── GalleryViewModel.kt                  // state（imageUris / mode / selection）
├── components/
│   ├── GalleryGrid.kt                   // molecule — LazyVerticalGrid + thumbnail
│   ├── GalleryThumbnail.kt              // atom — single image + select indicator
│   ├── SelectionTopBar.kt               // molecule — TopAppBar with selection count + delete
│   └── BrowseTopBar.kt                  // molecule — plain TopAppBar
data/media/
└── MediaDeletionUseCase.kt              // 50 行刪除邏輯抽出（DocumentsContract → MediaStore fallback）
```

```kotlin
// FILE: ui/gallery/GalleryViewModel.kt
class GalleryViewModel(
    private val mediaRepository: MediaRepository,        // 見 MAG-API-001
    private val deletionUseCase: MediaDeletionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(images = mediaRepository.queryMagnifierImages()) }
    }

    fun enterSelectionMode(initial: Uri) {
        _uiState.update { it.copy(mode = GalleryMode.Selection, selected = setOf(initial)) }
    }

    fun toggleSelection(uri: Uri) { ... }
    fun deleteSelected() { ... }
    fun openViewer(uri: Uri) { _uiState.update { it.copy(viewerUri = uri) } }
    fun closeViewer() { _uiState.update { it.copy(viewerUri = null) } }
}

sealed interface GalleryMode {
    object Browse : GalleryMode
    object Selection : GalleryMode
}

data class GalleryUiState(
    val images: List<Uri> = emptyList(),
    val mode: GalleryMode = GalleryMode.Browse,
    val selected: Set<Uri> = emptySet(),
    val viewerUri: Uri? = null,
)
```

```kotlin
// FILE: ui/gallery/GalleryScreen.kt
@Composable
fun GalleryScreen(onBack: () -> Unit, viewModel: GalleryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    BackHandler {
        when {
            state.viewerUri != null -> viewModel.closeViewer()
            state.mode is GalleryMode.Selection -> viewModel.exitSelectionMode()
            else -> onBack()
        }
    }

    Scaffold(
        topBar = {
            if (state.mode is GalleryMode.Selection) SelectionTopBar(...)
            else BrowseTopBar(...)
        }
    ) { padding ->
        GalleryGrid(state.images, state.mode, state.selected, ...)
    }
    state.viewerUri?.let { ImageViewer(it, onClose = viewModel::closeViewer) }
}
```

刪除邏輯抽 use case：
```kotlin
// FILE: data/media/MediaDeletionUseCase.kt
class MediaDeletionUseCase(
    private val resolver: ContentResolver,
    private val context: Context,
) {
    suspend fun delete(uris: Set<Uri>): DeletionResult {
        // L131-185 的 50 行邏輯，但分成兩個 private fun:
        // - tryDeleteViaDocumentsContract(uri): Boolean
        // - tryDeleteViaMediaStore(uri): Boolean
    }
}

data class DeletionResult(val deleted: Int, val failed: Int)
```

### Acceptance criteria

- [ ] AC-1: `GalleryScreen.kt` ≤ 100 行
- [ ] AC-2: 刪除邏輯獨立在 `MediaDeletionUseCase.kt`，可單測
- [ ] AC-3: `GalleryViewModel` 有 `mode: GalleryMode` 而非散落 boolean
- [ ] AC-4: `ImageViewer.kt` 不依賴 `GalleryViewModel`，可獨立用（傳 `Uri` + `onClose`）
- [ ] AC-5: 手動測試三個 mode 切換正常（瀏覽 → 長按進入選擇 → 多選 → 刪除；瀏覽 → 點圖進入全屏 → 返回）

### Verification

```bash
# 1. 行數
wc -l app/src/main/java/com/example/magnifier/ui/gallery/GalleryScreen.kt
# Expected: ≤ 100

# 2. mode 用 sealed interface 而非 boolean
grep "isSelectionMode" app/src/main/java/com/example/magnifier/ui/gallery/
# Expected: 無輸出

# 3. MediaDeletionUseCase 可單測
./gradlew :app:testDebugUnitTest --tests "*MediaDeletionUseCaseTest"
# Expected: 至少 3 個 case（DocumentsContract 成功 / DocumentsContract 失敗 fallback / 兩者皆失敗）

# 4. 手動 smoke
# 進相簿 → 長按某圖 → 進選擇模式 → 多選 → 刪除（Toast 顯示「已刪除 N 張」）
# 進相簿 → 點某圖 → 進全屏 → pinch zoom → 雙擊重置 → 返回鍵關閉檢視（不直接退出相簿）
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | BackHandler 邏輯三層複雜，容易遺漏 mode 切換 | ViewModel 統一 navigation 動作；BackHandler 只 dispatch 到 ViewModel |
| R-2 | MediaStore query 在背景 thread vs Compose recomposition 競爭 | `viewModelScope.launch` + `StateFlow` 統一；不在 Composable 內 query |
| R-3 | DocumentsContract.deleteDocument 在某些裝置 SecurityException | use case 內 try/catch，fallback 到 MediaStore（既有邏輯保留）|

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：GalleryScreen 又變大；刪除邏輯回到 inline
```
