---
doc: magnifier/audit/2026-05-11/architecture
title: Magnifier 架構 audit — 模組化 / 原子化 / API化 / 依賴化
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T12:00:00+08:00
author: claude-opus-4.7
follows: null
followed_by: null
trigger: 用戶從 Cursor 轉 Claude Code 工作流，動手重構前先寫 AI 友善 audit
language: zh-Hant primary, code/keywords in English
schema_version: 1
---

# Magnifier 架構 audit — 模組化 / 原子化 / API化 / 依賴化

## TL;DR

1. `MainActivity.kt` 是 891 行 god file — 1 Activity + 4 Composable + 3 工具 function 全擠一檔，**先拆 package**
2. 全部 state 都在 Composable 內 `remember` — 旋轉螢幕 / 進背景就掉狀態，**抽 ViewModel**
3. MediaStore / CameraX / Permission 邏輯直接寫死在 UI — 無法測試、無法替換，**定義介面**
4. `accompanist-permissions:0.34.0` 引入但 grep 0 命中 — **dead dependency，刪掉**
5. CameraX 1.3.4 / Coil 2.5.0 hardcoded 在 `app/build.gradle.kts` — **移入 `libs.versions.toml`**

> 本 audit 不動代碼，只列 gap 與 fix 計畫。每個 issue 都有獨立 sub-doc，含 Evidence / Fix / Acceptance criteria / Verification / Risks / Rollback 六段。

## Status table

| ID | severity | status | owner | eta | blocker_for | summary |
|---|---|---|---|---|---|---|
| [MAG-M-001](audit-2026-05-11-issues/MAG-M-001.md) | p1 | done | claude | 1d | MAG-A-*, MAG-API-* | god-file split：MainActivity 891→29（104f2e3）；AC-2 retroactively cleared by 65840a3 + 0b2dd78（所有檔 ≤ 250）|
| [MAG-M-002](audit-2026-05-11-issues/MAG-M-002.md) | p2 | not_started | claude | 1d | — | Gradle 子模組評估：`:app` vs `:core` + `:feature-*` |
| [MAG-A-001](audit-2026-05-11-issues/MAG-A-001.md) | p1 | not_started | claude | 1d | MAG-API-* | 抽 MagnifierViewModel：state holder 從 Composable 拉出 |
| [MAG-A-002](audit-2026-05-11-issues/MAG-A-002.md) | p1 | not_started | claude | 2d | — | 拆 MagnifierScreen 為 atoms / molecules / organisms |
| [MAG-A-003](audit-2026-05-11-issues/MAG-A-003.md) | p2 | done | claude | 0.5d | — | imageProxyToBitmap 拆 format-strategy（99fa6bb，6 unit tests pass，device smoke green）|
| [MAG-A-004](audit-2026-05-11-issues/MAG-A-004.md) | p2 | not_started | claude | 1d | — | GalleryScreen 內含 grid / selection / viewer 三 mode → 拆 |
| [MAG-API-001](audit-2026-05-11-issues/MAG-API-001.md) | p1 | partial | claude | 1d | MAG-A-001 | `MediaRepository` 介面：MediaStore CRUD 包裝（0b2dd78，device smoke green）；AC-3 instrumented test deferred |
| [MAG-API-002](audit-2026-05-11-issues/MAG-API-002.md) | p1 | partial | claude | 1d | MAG-A-001 | `CameraController` 介面：CameraX 生命週期 + zoom + torch（65840a3，device smoke green）；AC-5 FakeController deferred |
| [MAG-API-003](audit-2026-05-11-issues/MAG-API-003.md) | p2 | not_started | claude | 0.5d | — | `PermissionGate` 介面：權限申請流程抽象 |
| [MAG-D-001](audit-2026-05-11-issues/MAG-D-001.md) | polish | done | claude | 0.1d | — | 移除未使用的 accompanist-permissions:0.34.0（978278a）|
| [MAG-D-002](audit-2026-05-11-issues/MAG-D-002.md) | polish | done | claude | 0.2d | — | CameraX / Coil hardcoded version → libs.versions.toml catalog（2d58cac）|
| [MAG-D-003](audit-2026-05-11-issues/MAG-D-003.md) | p2 | not_started | claude | 0.5d | MAG-API-* | DI 框架評估：Hilt vs manual constructor injection |

```bash
# Copy-paste filter — 看當前 open issues
grep -hE "^\| \[?MAG-" docs/audit-2026-05-11-architecture.md \
  | grep -E "not_started|in_progress|partial|blocked|deferred"
```

## 推薦執行順序（依賴鏈）

```
MAG-D-001 (dead dep)               ← 0.1d，先清乾淨
   ↓
MAG-D-002 (version catalog)        ← 0.2d，clean 版本來源
   ↓
MAG-M-001 (package split)          ← 1d，建立 package 結構，後續所有 issue 都依賴它
   ↓
┌──────────────────────┬──────────────────────┐
MAG-API-001 (Media)   MAG-API-002 (Camera)  MAG-API-003 (Permission)
   ↓                     ↓                     ↓
   └──────── MAG-A-001 (ViewModel) ──────────┘
                        ↓
            MAG-A-002 (Compose split)
                        ↓
   ┌─────────────────────┴──────────────────────┐
MAG-A-003 (bitmap)       MAG-A-004 (gallery mode)
   ↓
MAG-D-003 (DI evaluation — 看到結構成形後再決定要不要 Hilt)
   ↓
MAG-M-002 (Gradle submodule — 等 package 穩定 6 個月後再考慮)
```

**最短關鍵路徑**（不含 polish）：`MAG-M-001 → MAG-API-* → MAG-A-001 → MAG-A-002`，預估 6 個工作日。

## 為何這四個面向

| 面向 | 當前 pain | 目標 |
|------|---------|------|
| **模組化** | 891 行單檔，新增功能要看完整檔才知道在改什麼 | 1 個 package = 1 個 feature，每檔 ≤ 200 行 |
| **原子化** | state / side-effect / UI 全混在 Composable 內 | state holder（ViewModel）/ 副作用（Repository）/ UI（Composable）三分 |
| **API 化** | MediaStore / CameraX 直接呼叫 | interface 包裝，可 mock 可測試可換實作 |
| **依賴化** | 第三方 lib 散在 build.gradle，DI 全靠 `remember` | 版本集中、生命週期由 DI 託管 |

## Cross-references

- llms.txt index：[llms.txt](llms.txt)
- 全域 schema_version=1 規範：`~/.claude/rules/ai-friendly-docs.md`
- 原子化規範：`~/.claude/rules/atomicity.md`
- 模組邊界規範：`~/.claude/rules/module-boundaries.md`
- API 設計規範：`~/.claude/rules/api-design.md`
- 隱性依賴規範：`~/.claude/rules/implicit-deps.md`

## Change log

| date | author | change |
|---|---|---|
| 2026-05-11 | claude-opus-4.7 | initial — 12 issues across M / A / API / D categories |
| 2026-05-11 | claude-opus-4.7 | self-check pass — Step 1 ✅ (all files < 250 lines, frontmatter OK) / Step 2 ✅ (6-section matrix 12/12 issues all 1s) / Step 3 ⚠️ skipped (this machine has no python; spec-doctor.py needs to run on Mac Studio / MacBook Air via Chronicle sync) |
| 2026-05-11 | claude-opus-4.7 | MAG-D-001 done in commit 978278a — verified build (1m 10s) + dep tree clean; AC-4 awaits manual device test |
| 2026-05-11 | claude-opus-4.7 | MAG-M-001 partial in commit 104f2e3 — MainActivity 891→29 lines, 6 new files; AC-2 awaits MAG-A-002/004 (MagnifierScreen 293 / GalleryScreen 252 exceed 250 target); AC-4 awaits manual device test |
| 2026-05-11 | claude-opus-4.7 | MAG-D-002 done in commit 2d58cac — catalog migration; AC-1/2/3 ✅ (build 1m 2s, dep tree unchanged); AC-4 awaits manual device test |
| 2026-05-11 | claude-opus-4.7 | user device smoke test all green — AC-4 marked done for MAG-D-001 / MAG-M-001 / MAG-D-002 |
| 2026-05-11 | claude-opus-4.7 | MAG-A-003 done in commit 99fa6bb — strategy pattern + 6 unit tests (tests=6 failures=0); AC-5 (capture flow) awaits next device smoke |
| 2026-05-11 | claude-opus-4.7 | MAG-API-001 partial in commit 0b2dd78 — MediaRepository interface + MediaStoreMediaRepository impl (157 lines); MediaStoreSource.kt deleted; UI layer has 0 ContentResolver refs; AC-3 instrumented deferred, AC-5 awaits device smoke (coroutine wrapping + Result envelope + DeletionResult are new behavior points) |
| 2026-05-11 | claude-opus-4.7 | Device smoke green for capture flow — AC-5 done for MAG-A-003 + MAG-API-001 (ab5dcf8) |
| 2026-05-11 | claude-opus-4.7 | MAG-API-002 partial in commit 65840a3 — CameraController + CameraXController; CameraPreview 96→37 lines; MagnifierScreen 304→250 lines; added concurrent-futures-ktx for await(); CancellationException explicitly rethrown. AC-5 FakeController deferred / AC-6/7 await device |
| 2026-05-11 | claude-opus-4.7 | MAG-M-001 upgraded partial→done — AC-2 retroactively cleared as 65840a3 + 0b2dd78 shrunk MagnifierScreen and GalleryScreen below the 250-line target |
