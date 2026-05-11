---
doc: magnifier/audit/2026-05-11/MAG-M-001
title: god-file split — MainActivity.kt 891 行拆 package-by-feature
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T13:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-M-001: god-file split — MainActivity.kt 891 行拆 package-by-feature

```yaml
id: MAG-M-001
severity: p1
status: partial
owner: claude
eta_days: 1
blocker_for: [MAG-A-001, MAG-A-002, MAG-A-003, MAG-A-004, MAG-API-001, MAG-API-002, MAG-API-003]
discovered_via: wc -l MainActivity.kt → 891 行
fixed_in: 104f2e3
related: [MAG-M-002]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 1-891

頂層宣告盤點（grep '^(class|fun|object) ' MainActivity.kt）：
- L96  class MainActivity                  ← Activity entry
- L113 fun GalleryScreen(...)              ← 相簿 (200+ 行)
- L322 fun ImageViewer(...)                ← 全屏檢視 (90+ 行)
- L416 fun queryMagnifierImages(...)       ← MediaStore query
- L465 fun CameraPreview(...)              ← CameraX wrapper
- L541 fun imageProxyToBitmap(...)         ← format conversion
- L594 fun saveImageToGallery(...)         ← MediaStore insert
- L652 fun MagnifierScreen()               ← root composable + state
```

單檔同時混雜：Activity 啟動、UI 渲染、CameraX、MediaStore、bitmap 轉換、權限申請。

`atomicity.md`：function ≤ 50 行 / ≤ 3 層巢狀；單檔 ≤ 1000 行。**891 行已超 atomicity warning 線**。

### Fix

```text
// 目標 package 結構
app/src/main/java/com/example/magnifier/
├── MainActivity.kt                          // ≤ 30 行，只放 onCreate + setContent
├── ui/
│   ├── theme/                               // 既有，不動
│   ├── magnifier/
│   │   ├── MagnifierScreen.kt              // root composable
│   │   └── MagnifierState.kt               // UI state data class
│   ├── camera/
│   │   ├── CameraPreview.kt                // CameraX composable wrapper
│   │   └── CameraControls.kt               // zoom slider + flash + capture
│   └── gallery/
│       ├── GalleryScreen.kt                // grid + selection
│       └── ImageViewer.kt                  // 全屏檢視
└── data/
    ├── media/
    │   ├── MediaStoreSource.kt             // queryMagnifierImages + save + delete
    │   └── ImageDecoder.kt                 // imageProxyToBitmap
    └── permission/
        └── PermissionLauncher.kt           // 權限申請邏輯
```

**Step-by-step**：
1. 建 package 資料夾（空檔即可，Kotlin 不需要 `__init__`）
2. 用 IDE「Move」refactor 把 function / composable 搬到對應檔（保留 import）
3. 每搬一個 function 即 `./gradlew assembleDebug` 確認編譯
4. 全部搬完後 MainActivity.kt 應只剩 ~30 行（Activity 殼）

### Acceptance criteria

- [x] AC-1: `MainActivity.kt` ≤ 50 行（只剩 Activity 殼）— **29 行** (commit 104f2e3)
- [ ] AC-2: 每個拆出的 .kt ≤ 250 行 — **partial**：MagnifierScreen 293 / GalleryScreen 252 仍超出（pure-move 無法拆內部，待 MAG-A-002 / MAG-A-004 atomize）；其他 4 檔皆 ≤ 125 行
- [x] AC-3: `./gradlew assembleDebug` 編譯通過 — **BUILD SUCCESSFUL in 6s** (2026-05-11)
- [x] AC-4: 手機跑 app，相機預覽 / zoom / 拍照 / 相簿 / 刪除 全部正常 — 用戶 2026-05-11 smoke test 全綠
- [ ] AC-5: `git diff --stat` 顯示新增檔案而非整檔重寫（保留 git blame 歷史）— **not used**：single-file → multi-file split 無法用 `git mv`，新檔 git blame 從零開始；用 commit message + audit 補償歷史

### Verification

```bash
# 1. 確認 MainActivity 已瘦身
wc -l app/src/main/java/com/example/magnifier/MainActivity.kt
# Expected: 30 ~ 50 行

# 2. 確認新 package 結構存在
find app/src/main/java/com/example/magnifier -type f -name "*.kt" | sort
# Expected: 至少 8 個 .kt（MainActivity + 7 拆出檔 + 3 theme）

# 3. 每檔行數檢查
find app/src/main/java/com/example/magnifier -name "*.kt" -exec wc -l {} \; | sort -rn
# Expected: 每檔 ≤ 250 行

# 4. 編譯通過
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL

# 5. 手動 smoke test（必跑，不可用「應該沒問題」帶過 — 見 ~/.claude/CLAUDE.md 事實驗證）
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.magnifier/.MainActivity
# Expected: app 開啟 → 授權相機 → 預覽顯示 → zoom 1→10 順暢 → 拍照存檔 → 相簿可見
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | IDE「Move」refactor 可能漏改 import | 每搬一檔即 `./gradlew compileDebugKotlin` 驗證 |
| R-2 | 拆檔過細導致 cyclic import | `MagnifierScreen` 依賴 `camera.*` 與 `gallery.*`，不可反向。建好 `module-boundaries.md` § Public API surface 即可避免 |
| R-3 | git blame 歷史斷裂 | 用 `git mv` 而非 delete + create；commit 訊息註明「Pure move, no behavior change」|

### Rollback

```bash
# 拆檔過程在 feature branch 進行；失敗直接捨棄分支
git checkout main
git branch -D refactor/MAG-M-001-god-file-split

# 已 merge 才出問題：
git revert <merge-commit-sha>
# 副作用：無（純結構改動，無 schema / API 變更）
```
