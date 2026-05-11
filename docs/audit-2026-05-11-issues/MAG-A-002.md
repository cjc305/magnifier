---
doc: magnifier/audit/2026-05-11/MAG-A-002
title: 拆 MagnifierScreen 為 atoms / molecules / organisms
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T00:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-A-002: 拆 MagnifierScreen 為 atoms / molecules / organisms

```yaml
id: MAG-A-002
severity: p1
status: not_started
owner: claude
eta_days: 2
blocker_for: []
discovered_via: MagnifierScreen 單一 Composable 從 L652 到 L891 共 240 行，內含 5 個 IconButton + 1 Slider + 1 Scaffold + 巢狀 lambda
fixed_in: null
related: [MAG-A-001, MAG-A-004]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 652-891

@Composable
fun MagnifierScreen() {
    ... state ×9 ...
    ... permission launcher ×2 ...
    ... if (showGallery) GalleryScreen(...) ...
    ... else Scaffold {
        Box {
            CameraPreview(...)
            Column {  // 控制面板
                Column {  // 放大倍率顯示
                    Text(...)
                    Slider(...)
                }
                Row {     // 功能按鈕行
                    IconButton(手電筒)
                    IconButton(拍照儲存) {
                        imageCapture.takePicture(executor, callback {  // ← 4 層巢狀
                            onCaptureSuccess { image ->
                                val bitmap = imageProxyToBitmap(image)
                                val savedUri = saveImageToGallery(...)
                                ...Toast.makeText(...).show()
                            }
                        })
                    }
                    if (lastSavedImageUri != null) IconButton(縮圖預覽)
                    else IconButton(空相簿圖示)
                }
            }
        }
    }
}
```

違反 `atomicity.md`：「一個 function 一件事；超過 50 行或 3 層巢狀 → 拆」。本 function 240 行 + 4 層巢狀 lambda。

### Fix

依 `module-boundaries.md` 的 atomic design 分層：

```text
ui/magnifier/
├── MagnifierScreen.kt          // organism — Scaffold + 編排 children
└── components/
    ├── ZoomSlider.kt           // molecule — Text + Slider 顯示倍率
    ├── FlashToggleButton.kt    // atom — IconButton + Icons.FlashOn
    ├── CaptureButton.kt        // atom — IconButton + Icons.CameraAlt
    └── GalleryThumbnail.kt     // molecule — lastSavedImageUri 縮圖 / 預設 icon
```

```kotlin
// FILE: ui/magnifier/components/ZoomSlider.kt
@Composable
fun ZoomSlider(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("放大倍率: ${"%.1f".format(zoomLevel)}x", ...)
        Slider(value = zoomLevel, onValueChange = onZoomChange, valueRange = 1f..10f, steps = 89)
    }
}

// FILE: ui/magnifier/components/FlashToggleButton.kt
@Composable
fun FlashToggleButton(isOn: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(Icons.Default.FlashOn, contentDescription = if (isOn) "關閉手電筒" else "開啟手電筒",
             tint = if (isOn) MaterialTheme.colorScheme.primary else ...)
    }
}

// FILE: ui/magnifier/MagnifierScreen.kt
@Composable
fun MagnifierScreen(viewModel: MagnifierViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            CameraPreviewArea(...)
            ControlPanel(
                state = state,
                onZoomChange = viewModel::setZoom,
                onFlashToggle = viewModel::toggleFlash,
                onCapture = viewModel::capture,
                onGalleryOpen = { viewModel.showGallery(true) },
            )
        }
    }
}
```

依賴方向（單向）：
```
MagnifierScreen (organism)
   ↓
ControlPanel (molecule)
   ↓
ZoomSlider / FlashToggleButton / CaptureButton / GalleryThumbnail (atoms / molecules)
```

**禁反向**：atoms 不准 import organisms。

### Acceptance criteria

- [ ] AC-1: `MagnifierScreen.kt` ≤ 80 行
- [ ] AC-2: 每個 atom / molecule .kt ≤ 60 行
- [ ] AC-3: 無 lambda 巢狀 > 2 層
- [ ] AC-4: 每個 atom / molecule 有 `@Preview` Composable
- [ ] AC-5: `androidx.compose.ui:ui-tooling` debug 依賴可看到所有 preview
- [ ] AC-6: 跑 app，功能與重構前一致

### Verification

```bash
# 1. 行數檢查
find app/src/main/java/com/example/magnifier/ui/magnifier -name "*.kt" -exec wc -l {} \;
# Expected: MagnifierScreen ≤ 80; 其他 ≤ 60

# 2. Preview annotation 數量
grep -rln "@Preview" app/src/main/java/com/example/magnifier/ui/magnifier/components/
# Expected: 至少 4 個檔（每個 atom / molecule 一個）

# 3. atomic boundary（atoms 不引 organism）
grep -l "import.*MagnifierScreen" app/src/main/java/com/example/magnifier/ui/magnifier/components/*.kt
# Expected: 無輸出（atoms 不 import organism）

# 4. 編譯與功能
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL
# 手動 smoke：開 app → 預覽 / zoom / flash / 拍照 / 縮圖 全部正常
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | 拆過細 → 每個 atom 只用一次，反而增加閱讀成本 | 規則「同 organism 內只用一次的 component 可保留 inline」；只有跨檔重用或 > 30 行才拆 |
| R-2 | Preview 在沒 ViewModel 的 atom 顯示正常，但 organism 預覽需要 mock ViewModel | organism 改吃 `MagnifierUiState` 而非 `MagnifierViewModel`，preview 傳 fake state |
| R-3 | callback 一路傳到深層 atom，prop drilling 變嚴重 | 控制深度 ≤ 3 層；超過考慮 `CompositionLocal` |

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：UI 行為不變；Preview 數量減少
```
