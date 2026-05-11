---
doc: magnifier/audit/2026-05-11/MAG-API-003
title: PermissionGate 介面 — 權限申請流程抽象
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T00:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-API-003: PermissionGate 介面 — 權限申請流程抽象

```yaml
id: MAG-API-003
severity: p2
status: not_started
owner: claude
eta_days: 0.5
blocker_for: []
discovered_via: rememberLauncherForActivityResult + Build.VERSION_CODES.TIRAMISU 判斷 inline 在 MagnifierScreen
fixed_in: null
related: [MAG-A-001, MAG-D-001]
```

### Evidence

```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: 669-695

val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted -> hasCameraPermission = isGranted }

val storagePermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    hasStoragePermission = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true)
}

LaunchedEffect(Unit) {
    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
    } else {
        storagePermissionLauncher.launch(arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))
    }
}
```

**問題**：
- 兩個 launcher 在 Composable 內宣告，邏輯與 UI 綁定
- API_LEVEL 判斷散在兩處（grant check + launch）— 修改需同步兩處
- 沒有「永久拒絕」處理（用戶點 Don't ask again 後永遠看不到提示）
- 沒有 rationale UI 提示
- `hasStoragePermission` 名字模糊（其實是「READ_MEDIA_IMAGES 或舊版 WRITE_EXTERNAL_STORAGE」）

### Fix

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/permission/PermissionGate.kt
// LINES: new file

sealed interface PermissionState {
    object Granted : PermissionState
    object Denied : PermissionState
    object PermanentlyDenied : PermissionState     // shouldShowRequestPermissionRationale=false 且未授權
    object NotRequested : PermissionState
}

data class AppPermissions(
    val camera: PermissionState = PermissionState.NotRequested,
    val mediaRead: PermissionState = PermissionState.NotRequested,
) {
    val allGranted: Boolean
        get() = camera is PermissionState.Granted && mediaRead is PermissionState.Granted
}

interface PermissionGate {
    val state: StateFlow<AppPermissions>
    suspend fun requestAll(activity: ComponentActivity)
    fun refresh()                                    // 從背景回前台時呼叫
}
```

```kotlin
// FILE: app/src/main/java/com/example/magnifier/data/permission/AndroidPermissionGate.kt

class AndroidPermissionGate(
    private val context: Context,
) : PermissionGate {

    private val _state = MutableStateFlow(AppPermissions())
    override val state: StateFlow<AppPermissions> = _state.asStateFlow()

    override suspend fun requestAll(activity: ComponentActivity) {
        val needed = buildRequestList()                          // 按 API_LEVEL 集中決定
        val results = activity.requestPermissions(needed)         // suspend wrapper
        _state.value = AppPermissions(
            camera = results.classifyCamera(activity),
            mediaRead = results.classifyMediaRead(activity),
        )
    }

    override fun refresh() {
        _state.value = AppPermissions(
            camera = readCurrentState(Manifest.permission.CAMERA),
            mediaRead = readCurrentMediaState(),
        )
    }

    private fun buildRequestList(): List<String> =
        listOf(Manifest.permission.CAMERA) + mediaPermissions()

    private fun mediaPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        else
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                   Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // private classify / readCurrent / requestPermissions wrapper
}
```

UI 層只用 state：
```kotlin
@Composable
fun PermissionAwareContent(
    gate: PermissionGate,
    onPermanentlyDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val perms by gate.state.collectAsStateWithLifecycle()
    when {
        perms.allGranted -> content()
        perms.camera is PermissionState.PermanentlyDenied -> SettingsLink(onPermanentlyDenied)
        else -> PermissionRationale(onRequest = { /* gate.requestAll(...) */ })
    }
}
```

### Acceptance criteria

- [ ] AC-1: `PermissionGate` interface 與 `AndroidPermissionGate` impl 分離
- [ ] AC-2: API_LEVEL 判斷集中在 `buildRequestList()` / `mediaPermissions()`，UI 層 0 個 `Build.VERSION` 判斷
- [ ] AC-3: 有「永久拒絕」狀態，UI 顯示「請到設定開啟權限」連結
- [ ] AC-4: Composable 內無 `rememberLauncherForActivityResult` 直接宣告（移到 ViewModel / Gate）
- [ ] AC-5: 手動測試三 case：首次授權 / 拒絕一次 / 永久拒絕

### Verification

```bash
# 1. interface 存在
test -f app/src/main/java/com/example/magnifier/data/permission/PermissionGate.kt && echo OK

# 2. UI 層無 Build.VERSION 散落
grep -rn "Build.VERSION" app/src/main/java/com/example/magnifier/ui/
# Expected: 無輸出（或僅限視覺判斷如 dynamic color）

# 3. UI 層無 rememberLauncherForActivityResult
grep -rn "rememberLauncherForActivityResult" app/src/main/java/com/example/magnifier/ui/
# Expected: 無輸出（或僅限非權限用途）

# 4. 手動三 case
# Case A：首次安裝 → 開 app → 系統彈權限 → 全允許 → 預覽顯示
# Case B：開 app → 拒絕相機 → 看到 rationale → 點「再試」→ 彈權限 → 允許 → 預覽
# Case C：開 app → 拒絕並勾「不再詢問」→ 看到「請到設定開啟」連結
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | `shouldShowRequestPermissionRationale` 在「首次」與「永久拒絕」回 false，難分辨 | 用 SharedPreferences 記錄「是否曾經請求過」，配合判斷 |
| R-2 | suspend wrapper 包裝 ActivityResultLauncher 需要 callback bridge | 用 `suspendCancellableCoroutine` + Activity 的 `registerForActivityResult` registry |
| R-3 | UI 層 `gate.requestAll(activity)` 需要 Activity reference | 用 `LocalActivity.current`（需要 androidx.activity 1.10+，已具備）|

### Rollback

```bash
git revert <merge-commit-sha>
# 副作用：權限邏輯回到散落狀態；UI 層又出現 Build.VERSION 判斷
```
