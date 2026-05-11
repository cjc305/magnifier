---
doc: magnifier/audit/2026-05-11/MAG-D-002
title: CameraX / Coil hardcoded version → libs.versions.toml catalog
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T14:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-D-002: CameraX / Coil hardcoded version → libs.versions.toml catalog

```yaml
id: MAG-D-002
severity: polish
status: done
owner: claude
eta_days: 0.2
blocker_for: []
discovered_via: app/build.gradle.kts L55-68 含 hardcoded "1.3.4" / "2.5.0" / "0.34.0" string literal
fixed_in: 2d58cac
related: [MAG-D-001]
```

### Evidence

```
FILE: app/build.gradle.kts
LINES: 54-68

// CameraX
val cameraxVersion = "1.3.4"
implementation("androidx.camera:camera-core:${cameraxVersion}")
implementation("androidx.camera:camera-camera2:${cameraxVersion}")
implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
implementation("androidx.camera:camera-view:${cameraxVersion}")

// Material Icons Extended (版本由 Compose BOM 管理)
implementation("androidx.compose.material:material-icons-extended")

// Permission handling
implementation("com.google.accompanist:accompanist-permissions:0.34.0")    ← 將被 MAG-D-001 移除

// Coil for image loading
implementation("io.coil-kt:coil-compose:2.5.0")
```

```
FILE: gradle/libs.versions.toml
LINES: 1-29

已有 catalog 結構（agp / kotlin / coreKtx / lifecycleRuntimeKtx / activityCompose / composeBom）
缺：camerax / coil / material-icons-extended
```

**問題**：
- 升級 CameraX 要動 `app/build.gradle.kts` 而非 `libs.versions.toml`（與既有依賴 inconsistent）
- Renovate / Dependabot 偵測 hardcoded 版本麻煩
- 多 module 時版本可能 drift（雖然本專案目前單 module）
- 違反 `implicit-deps.md` 「集中宣告於一份 schema」

### Fix

```diff
// FILE: gradle/libs.versions.toml
// COMMIT: chore(MAG-D-002): move camerax/coil to version catalog

 [versions]
 agp = "8.13.2"
 kotlin = "2.0.21"
 coreKtx = "1.17.0"
 ...
 composeBom = "2024.09.00"
+camerax = "1.3.4"
+coil = "2.5.0"

 [libraries]
 ...
+androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
+androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
+androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
+androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
+androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
+coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

```diff
// FILE: app/build.gradle.kts
// LINES: 54-68

-    // CameraX
-    val cameraxVersion = "1.3.4"
-    implementation("androidx.camera:camera-core:${cameraxVersion}")
-    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
-    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
-    implementation("androidx.camera:camera-view:${cameraxVersion}")
-
-    // Material Icons Extended (版本由 Compose BOM 管理)
-    implementation("androidx.compose.material:material-icons-extended")
-
-    // Coil for image loading
-    implementation("io.coil-kt:coil-compose:2.5.0")
+    // CameraX (version managed in libs.versions.toml)
+    implementation(libs.androidx.camera.core)
+    implementation(libs.androidx.camera.camera2)
+    implementation(libs.androidx.camera.lifecycle)
+    implementation(libs.androidx.camera.view)
+
+    // Material Icons Extended (version managed by Compose BOM)
+    implementation(libs.androidx.compose.material.icons.extended)
+
+    // Coil for image loading
+    implementation(libs.coil.compose)
```

### Acceptance criteria

- [x] AC-1: `app/build.gradle.kts` 無任何 `implementation("group:name:version")` 字串字面量 — grep exit 1 (commit 2d58cac)
- [x] AC-2: `gradle/libs.versions.toml` 含 camerax / coil / material-icons-extended 條目 — 8 個 catalog 項
- [x] AC-3: `./gradlew assembleDebug` 編譯通過，依賴 resolve 結果與原版一致 — BUILD SUCCESSFUL in 1m 2s (2026-05-11)；dep tree 確認 CameraX 1.3.4 / Coil 2.5.0 / material-icons-extended 1.7.0 (BOM)
- [ ] AC-4: 實機跑 app，相機 / Coil 載入縮圖 / Material icons 顯示全部正常 — **待用戶實機驗證**

### Verification

```bash
# 1. build.gradle.kts 內無 hardcoded 版本字串
grep -E 'implementation\("[^"]+:[^"]+:[^"]+"\)' app/build.gradle.kts
# Expected: 無輸出（全部走 libs.xxx）

# 2. catalog 含新條目
grep -E "camerax|coil" gradle/libs.versions.toml
# Expected: [versions] camerax = "1.3.4" / coil = "2.5.0"
# Expected: [libraries] androidx-camera-* / coil-compose 條目

# 3. 依賴 resolve 一致
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > /tmp/deps-after.txt
# 與重構前的 deps-before.txt 對 diff，只應差「accompanist-permissions」(if MAG-D-001 也做了)

# 4. 編譯 + 實機
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
# Expected: 預覽 / 縮圖 / icon 正常
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | catalog 條目命名 convention 與既有不一致 | 沿用 `androidx-camera-*` / `coil-compose` 連字號風格（與既有 `androidx-core-ktx` 一致） |
| R-2 | material-icons-extended 沒有 version.ref（BOM 管），catalog 條目語法易錯 | 條目省略 `version.ref`，純掛 group / name；測試確認 BOM 仍能 resolve |
| R-3 | Renovate 升級 PR 同時動 2 個檔（catalog + build.gradle）破壞 | 純動 catalog 即可升級，build.gradle 不需動 |

### Rollback

```bash
git revert <commit-sha>
# 副作用：版本回到 hardcoded；功能不變
```
