---
doc: magnifier/audit/2026-05-11/MAG-M-002
title: Gradle 子模組評估 — `:app` 單模組 vs `:core` + `:feature-*`
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T00:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-M-002: Gradle 子模組評估 — `:app` 單模組 vs `:core` + `:feature-*`

```yaml
id: MAG-M-002
severity: p2
status: not_started
owner: claude
eta_days: 1
blocker_for: []
discovered_via: settings.gradle.kts 僅 include(":app")
fixed_in: null
related: [MAG-M-001]
```

### Evidence

```
FILE: settings.gradle.kts
LINES: 22
內容：include(":app")  ← 單模組

FILE: app/build.gradle.kts
LINES: 1-77
所有依賴（CameraX / Compose / Coil）混在一個 module
```

**目前單模組現況**：
- 編譯時間：full clean build ~ 估 60-90s（待實測）
- Gradle config 時間：小，因為只有 1 個 module
- 換 feature 改 1 行 → 整個 `:app` rebuild

**單模組 vs 多模組 trade-off**：

| 維度 | 單模組 `:app` | 多模組 `:core` + `:feature-*` |
|------|--------------|-----------------------------|
| Clean build | 60-90s | 90-150s（多 Gradle config 開銷）|
| Incremental | 改 1 檔 → 全 app 編譯 | 改 feature 只編該 feature + app |
| 測試隔離 | 必跑整 app testDebug | 可單獨 `./gradlew :feature-camera:test` |
| Boundary 強制 | 靠規矩 / lint | Gradle 物理隔絕（compile error）|
| 適合規模 | < 30 個 Kotlin 檔 | > 50 個 Kotlin 檔或多 feature 並行 |
| 入門門檻 | 0 | 中（依賴宣告、build types 同步）|

### Fix

**建議：暫不拆 Gradle 子模組（status: deferred）**，理由：
1. 全 app 預估拆完 MAG-M-001 後總共 ~15-20 個 Kotlin 檔，規模未到多模組門檻
2. MAG-M-001 的 package-by-feature 已能達到 80% 的好處（boundary 靠 ESLint-style lint 或 review）
3. 多模組會增加 IDE 載入時間 + Gradle config 時間，對單人專案是負債

**何時重新評估**：
- Kotlin 檔 > 50 個
- 引入第二個 Activity（如：設定頁、教學頁）
- 引入後台 Service（如：背景濾鏡處理）
- 多人並行開發（>= 2 人）

若決定動手拆，目標結構：

```text
magnifier/
├── settings.gradle.kts                     // include(":app", ":core", ":feature-camera", ":feature-gallery")
├── app/                                    // 只剩 Application + MainActivity + DI 組裝
├── core/
│   ├── data/                               // MediaRepository impl, CameraController impl
│   ├── ui/                                 // 共享 atoms (按鈕、icon、圖示)
│   └── domain/                             // pure Kotlin model / use case
├── feature-camera/                         // CameraPreview + Controls + ViewModel
└── feature-gallery/                        // GalleryScreen + ImageViewer + ViewModel
```

依賴方向（單向，禁循環）：
```
:app → :feature-camera → :core
     → :feature-gallery → :core
```

### Acceptance criteria

- [ ] AC-1：寫下「不拆」的決策時間戳記 + 重新評估觸發條件（記在本 doc Change log）
- [ ] AC-2：若決定拆，`settings.gradle.kts` include 全 module
- [ ] AC-3：若決定拆，`./gradlew :feature-camera:assembleDebug` 可獨立執行
- [ ] AC-4：若決定拆，`madge --circular` 或等價工具 0 循環

### Verification

```bash
# 1. 確認當前狀態（不拆的情況下）
grep -c '^include' settings.gradle.kts
# Expected: 1（只 include :app）

# 2. 計算 Kotlin 檔數量，看有沒有越過 50 個的門檻
find app/src/main/java -name "*.kt" | wc -l
# Expected: < 30（不需要拆），> 50（觸發重新評估）

# 3. 若決定拆，驗證 module 隔絕（無循環依賴）
./gradlew :feature-camera:dependencies --configuration debugCompileClasspath \
  | grep -E "project ':feature-gallery'"
# Expected: 無輸出（feature 不互相依賴）
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | 過早拆模組導致 IDE 慢、build 慢 | 用 AC-1 的觸發條件當決策閾值，不憑感覺拆 |
| R-2 | 拆完後跨模組共享 type 需提到 `:core` | 拆時把 model class 提到 `:core:domain` 即可 |
| R-3 | Compose preview 在子模組失效 | 每子模組單獨加 `androidx-compose-ui-tooling` debug 依賴 |

### Rollback

```bash
# 多模組改回單模組
# 1. settings.gradle.kts 移除所有 include 只留 :app
# 2. 把 :core / :feature-* 的程式碼搬回 :app
# 3. 刪空模組資料夾
# 4. ./gradlew clean assembleDebug 驗證
git revert <merge-commit-sha>
```

N/A — 本 issue 預設 deferred，若不動手則無需 rollback。
