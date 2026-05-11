---
doc: magnifier/audit/2026-05-11/MAG-D-001
title: 移除未使用的 accompanist-permissions:0.34.0
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T12:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-D-001: 移除未使用的 accompanist-permissions:0.34.0

```yaml
id: MAG-D-001
severity: polish
status: done
owner: claude
eta_days: 0.1
blocker_for: []
discovered_via: grep 'accompanist\|rememberPermissionState' app/src/main → 0 matches
fixed_in: 978278a
related: [MAG-API-003]
```

### Evidence

```
FILE: app/build.gradle.kts
LINES: 64-65

// Permission handling
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

驗證未使用：
```
$ grep -rn "accompanist\|rememberPermissionState\|rememberMultiplePermissionsState" app/src/main
（無輸出 — 0 matches）
```

實際使用的是 `ActivityResultContracts.RequestPermission` / `RequestMultiplePermissions`（AndroidX 內建），accompanist 純屬未消費的依賴。

**影響**：
- APK size 多帶 ~50KB（accompanist-permissions 編譯產物）
- 多一條 supply chain 攻擊面
- 新人看到依賴會誤以為「權限走 accompanist」→ 推測錯方向
- accompanist 已停止 active 開發（部分功能移入 Compose 1.x 官方）

### Fix

```diff
// FILE: app/build.gradle.kts
// LINES: 64-65
// COMMIT: chore(MAG-D-001): drop unused accompanist-permissions

-    // Permission handling
-    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

完整刪除兩行。不需要任何 code 改動（grep 已確認 0 使用）。

### Acceptance criteria

- [x] AC-1: `app/build.gradle.kts` 移除 accompanist-permissions 行 (commit 978278a)
- [x] AC-2: `./gradlew app:dependencies` 不再列出 accompanist-permissions（drawablepainter 是 Coil transitive，非本 issue scope）
- [x] AC-3: `./gradlew assembleDebug` 編譯通過（BUILD SUCCESSFUL in 1m 10s, 2026-05-11）
- [x] AC-4: 實機跑 app，權限申請仍正常 — 用戶 2026-05-11 smoke test 全綠

### Verification

```bash
# 1. 依賴已移除
grep "accompanist" app/build.gradle.kts
# Expected: 無輸出

# 2. Gradle resolve tree 不含
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep accompanist
# Expected: 無輸出

# 3. 編譯
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL

# 4. APK 內 dex 不含 accompanist class
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep accompanist
# Expected: 無輸出
# 或更精確：
./gradlew :app:assembleDebug && \
  $ANDROID_HOME/build-tools/*/aapt2 dump packagename app/build/outputs/apk/debug/app-debug.apk
# 然後手動 dexdump 確認

# 5. 手動 smoke
# 首次安裝 → 開 app → 系統權限彈窗 → 全允許 → 預覽正常
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | grep 漏判（如 reflective import） | accompanist-permissions 沒有 reflective API，純 Compose function；grep 已涵蓋。再 `grep -r "com.google.accompanist" .` 全域確認 |
| R-2 | 未來 MAG-API-003 (PermissionGate) 想用 accompanist 又要加回來 | MAG-API-003 計畫用 AndroidX 原生 + StateFlow，不依賴 accompanist。若改主意可隨時加回 |

### Rollback

```bash
git revert <commit-sha>
# 副作用：依賴重新出現，APK 多 50KB；功能不變
```
