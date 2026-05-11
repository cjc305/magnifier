---
doc: magnifier/audit/2026-05-11/MAG-API-001
title: MediaRepository 介面 — MediaStore CRUD 包裝
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T00:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-API-001: MediaRepository 介面 — MediaStore CRUD 包裝

```yaml
id: MAG-API-001
severity: p1
status: not_started
owner: claude
eta_days: 1
blocker_for: [MAG-A-001, MAG-A-004]
discovered_via: MediaStore 三組操作（query / insert / delete）散在 MainActivity.kt 三處不同 function
fixed_in: null
related: [MAG-A-001, MAG-A-004]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt

L416-463  queryMagnifierImages(context): List<Uri>
          ContentResolver.query + selection RELATIVE_PATH LIKE %/Magnifier/%
          手動處理 Build.VERSION (Q+ vs older)

L594-650  saveImageToGallery(context, bitmap): Uri?
          ContentValues + insert + openOutputStream + bitmap.compress
          try/catch 三層；錯誤路徑 delete 已建 entry

L131-185  deleteSelectedImages (inside GalleryScreen)
          DocumentsContract.deleteDocument → fallback MediaStore.delete
          直接呼叫 context.contentResolver
```

**問題**：
- 三個操作直接呼叫 `context.contentResolver`，UI 層綁死 Android framework
- 無法 mock 測試（必須跑 instrumented test）
- 邏輯散落三處，新增「重新命名」「依日期 group」等操作要再開一個全域 function
- API_LEVEL 判斷 inline 在 query / save 兩處，重複碼

### Fix

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/media/MediaRepository.kt
// LINES: new file

interface MediaRepository {
    suspend fun queryMagnifierImages(): List<Uri>
    suspend fun save(bitmap: Bitmap, displayName: String? = null): Result<Uri>
    suspend fun delete(uris: Set<Uri>): DeletionResult
}

data class DeletionResult(val deleted: Int, val failed: Int)
```

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/media/MediaStoreMediaRepository.kt
// LINES: new file

class MediaStoreMediaRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaRepository {

    private val resolver get() = context.contentResolver

    override suspend fun queryMagnifierImages(): List<Uri> = withContext(dispatcher) {
        // L416-463 邏輯搬入；private fun buildSelection() 處理 API_LEVEL
    }

    override suspend fun save(bitmap: Bitmap, displayName: String?): Result<Uri> =
        withContext(dispatcher) {
            runCatching {
                val name = displayName ?: defaultDisplayName()
                val values = buildContentValues(name)        // private — API_LEVEL 處理集中於此
                val uri = resolver.insert(IMAGE_TABLE, values)
                    ?: error("無法建立 MediaStore entry")
                writeBitmap(uri, bitmap)                     // private — 失敗即 delete entry
                uri
            }
        }

    override suspend fun delete(uris: Set<Uri>): DeletionResult = withContext(dispatcher) {
        var ok = 0
        var fail = 0
        for (uri in uris) {
            if (tryDocumentsDelete(uri) || tryMediaStoreDelete(uri)) ok++ else fail++
        }
        DeletionResult(ok, fail)
    }

    // private helpers ≤ 20 行 each
    private fun buildContentValues(displayName: String): ContentValues { ... }
    private fun writeBitmap(uri: Uri, bitmap: Bitmap) { ... }
    private fun tryDocumentsDelete(uri: Uri): Boolean { ... }
    private fun tryMediaStoreDelete(uri: Uri): Boolean { ... }

    private companion object {
        val IMAGE_TABLE = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
}
```

API 設計遵循 `~/.claude/rules/api-design.md`：
- 統一 envelope：用 Kotlin `Result<T>` / data class `DeletionResult`
- 命名：query / save / delete，動詞為主
- 錯誤分類：`Result.failure` 帶 `Throwable`，UI 層 `onFailure { ... }` 顯示 Toast
- Idempotency：save 用 `displayName` (含時間戳) 自然去重

### Acceptance criteria

- [ ] AC-1: `MediaRepository` interface 定義在 `data/media/` 下
- [ ] AC-2: `MediaStoreMediaRepository` 實作所有方法，每個 method 不超過 40 行
- [ ] AC-3: `MediaRepositoryTest` (instrumented，需 `androidTest`) 至少覆蓋：query 空清單 / save 一張圖回傳 URI / delete 不存在 URI 回 failed=1
- [ ] AC-4: `MainActivity.kt` / `GalleryScreen.kt` 內無 `contentResolver.` 直接呼叫
- [ ] AC-5: 實機跑 app，拍照 / 相簿 / 刪除全部正常

### Verification

```bash
# 1. interface 存在
test -f app/src/main/java/com/example/magnifier/data/media/MediaRepository.kt && echo OK

# 2. UI 層無直接 contentResolver 呼叫
grep -rn "contentResolver" \
  app/src/main/java/com/example/magnifier/ui/ \
  app/src/main/java/com/example/magnifier/MainActivity.kt
# Expected: 無輸出

# 3. instrumented test 通過（需接 emulator / 手機）
./gradlew :app:connectedDebugAndroidTest --tests "*MediaRepositoryTest"
# Expected: 至少 3 個 case 通過

# 4. 手動 smoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 拍 3 張 → 進相簿看到 3 張 → 刪除 1 張 → 看到 2 張
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | suspend fun 在 IO dispatcher，但 CameraX callback 在 main thread → 需要 bridge | ViewModel 用 `viewModelScope.launch { repo.save(...) }`，coroutine context switch 自動處理 |
| R-2 | API_LEVEL 判斷集中後若漏判一個分支可能 crash 在舊機 | 寫 `MediaStoreMediaRepositoryAndroidTest` 在 minSdk 24 emulator 跑 |
| R-3 | `Result<Uri>` Kotlin 1.5+ 才穩定，本專案 Kotlin 2.0.21 → 無風險 | 已驗證 |

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：MediaStore 邏輯回到散落狀態；後續 MAG-A-001 ViewModel 需要另尋資料源
```
