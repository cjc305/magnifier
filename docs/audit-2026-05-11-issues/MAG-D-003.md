---
doc: magnifier/audit/2026-05-11/MAG-D-003
title: DI 框架評估 — Hilt vs manual constructor injection
created: 2026-05-11T00:00:00+08:00
updated: 2026-05-11T19:00:00+08:00
author: claude-opus-4.7
schema_version: 1
---

## MAG-D-003: DI 框架評估 — Hilt vs manual constructor injection

```yaml
id: MAG-D-003
severity: p2
status: partial
owner: claude
eta_days: 0.5
blocker_for: []
discovered_via: MAG-A-001 / MAG-API-* 完成後 ViewModel 需要依賴 MediaRepository / CameraController / PermissionGate → 必須有 DI 策略
fixed_in: 0cb3058
related: [MAG-A-001, MAG-API-001, MAG-API-002, MAG-API-003]
```

> **2026-05-11 update**：本 issue 原本排在 MAG-A-001 之後，但實作 MAG-A-001 時發現「ViewModel 跨 rotation 存活、deps 卻被 `remember` 重建」會造成孤兒 controller bug → Option B (manual constructor injection) 必須提前落地。Phase A of MAG-A-001 (commit 0cb3058) 已完成 Option B 的骨架：`MagnifierApplication` + `AppContainer` (by-lazy MediaRepository / CameraController / PermissionGate) + AndroidManifest 註冊。本 issue 因此標 partial — 決策已執行 Option B，剩餘是「跨季度重新評估」的決策時間戳記與升 Hilt 的觸發條件 documentation。

### Evidence

當前**完全沒有 DI**：
```
FILE: app/src/main/java/com/example/magnifier/MainActivity.kt
LINES: whole file

無 Application class 客製化
無 Hilt @HiltAndroidApp / @AndroidEntryPoint
無 Koin / Anvil / Dagger
所有依賴用 remember { ... } 在 Composable 內 new
```

`build.gradle.kts` 無 DI plugin。

完成 MAG-A-001 / MAG-API-* 後會出現需求：
- `MagnifierViewModel(mediaRepo, cameraController)` 需要從 Activity 拿到 instance
- `GalleryViewModel(mediaRepo, deletionUseCase)` 同上
- `MediaStoreMediaRepository(context)` 需要 Application Context
- `CameraXController(context)` 同上

不解決 → 退回 `viewModel { ... }` factory 手動 new，OR Singleton object，OR pass Activity 一路傳到深處（醜）。

### Fix

**三條路線評估**：

#### Option A：Hilt（Google 官方推薦）

```kotlin
// 加 dependencies
plugins { kotlin("kapt"); id("com.google.dagger.hilt.android") }
implementation("com.google.dagger:hilt-android:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
kapt("com.google.dagger:hilt-android-compiler:2.51.1")

// MagnifierApplication.kt
@HiltAndroidApp class MagnifierApplication : Application()

// di/MediaModule.kt
@Module @InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds @Singleton
    abstract fun bindMediaRepo(impl: MediaStoreMediaRepository): MediaRepository
}

// MainActivity.kt
@AndroidEntryPoint class MainActivity : ComponentActivity()

// MagnifierViewModel.kt
@HiltViewModel
class MagnifierViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val cameraController: CameraController,
) : ViewModel()

// MagnifierScreen.kt
val viewModel: MagnifierViewModel = hiltViewModel()
```

**Trade-off**：
- ✅ Compile-time safety、官方 Compose 支援好、未來擴張無痛
- ❌ kapt 編譯時間 +20-40%、增加 build 複雜度、APK +~200KB
- ❌ 學習曲線（Module / Component / Scope）

#### Option B：Manual constructor injection + ViewModelProvider.Factory

```kotlin
// MagnifierApplication.kt
class MagnifierApplication : Application() {
    val mediaRepository: MediaRepository by lazy { MediaStoreMediaRepository(this) }
    val cameraController: CameraController by lazy { CameraXController(this) }
}

// MainActivity.kt
class MainActivity : ComponentActivity() {
    private val viewModel: MagnifierViewModel by viewModels {
        val app = application as MagnifierApplication
        viewModelFactory {
            initializer { MagnifierViewModel(app.mediaRepository, app.cameraController) }
        }
    }
}
```

**Trade-off**：
- ✅ 0 新增依賴、編譯時間不變、Kotlin 純粹
- ✅ 適合小專案（< 10 個 ViewModel）
- ❌ 加 ViewModel / Repository 要動 Application + Activity 兩個檔
- ❌ Scope 全靠 by lazy + Application 生命週期

#### Option C：Koin（runtime DI）

```kotlin
implementation("io.insert-koin:koin-androidx-compose:3.5.6")

// di/AppModule.kt
val appModule = module {
    single<MediaRepository> { MediaStoreMediaRepository(androidContext()) }
    single<CameraController> { CameraXController(androidContext()) }
    viewModel { MagnifierViewModel(get(), get()) }
}

// MagnifierApplication.kt
class MagnifierApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@MagnifierApplication); modules(appModule) }
    }
}

// MagnifierScreen.kt
val viewModel: MagnifierViewModel = koinViewModel()
```

**Trade-off**：
- ✅ 無 kapt、編譯快、語法精簡
- ❌ Runtime resolve（錯誤要跑到才知）
- ❌ 社群比 Hilt 小，未來人接手熟悉度低

#### 推薦

**目前規模（< 5 ViewModel、單人開發）→ Option B（manual factory）**

理由：
- 專案規模小，DI 框架的好處（自動 wiring）未顯著
- 編譯快比 DI elegance 更重要（手機 debug iterate）
- 若日後超過 10 ViewModel 或加第二個 Activity，再升級 Hilt

**升級觸發條件**：
- ViewModel 數 >= 10
- 加第二個 Activity / Service
- 引入 Worker / Notification Listener 等 framework component
- 多 module 分割（MAG-M-002 拆完）

### Acceptance criteria

- [x] AC-1: 決策時間戳記與選項記在本 doc Change log — 2026-05-11 採 Option B（見上方更新說明 + Change log）
- [x] AC-2: Option B：建 `MagnifierApplication.kt`，AndroidManifest 註冊 `android:name=".MagnifierApplication"` — commit 0cb3058
- [x] AC-3: Option B：Composable 用 `viewModel(factory = ...)` 取得 ViewModel — `MagnifierScreen` / `GalleryScreen` 用 `androidx.lifecycle.viewmodel.compose.viewModel` (commit 1d81ede)
- [ ] AC-4: 新增 ViewModel 流程文檔化 — **deferred**：等加第 3 個 ViewModel 時再寫 SOP（目前 2 個 VM，pattern 還在演進）
- [ ] AC-5: clean build 時間 ±20% — **待量測**：MAG-A-001 完整 chain 跑完後一次 clean build 計時對照

### Verification

```bash
# 1. Option B 驗證
grep -E "@HiltAndroidApp|startKoin" app/src/main/java/com/example/magnifier/MagnifierApplication.kt
# Expected: 無輸出（純 manual）

grep "ViewModelProvider.Factory\|viewModelFactory" app/src/main/java/com/example/magnifier/MainActivity.kt
# Expected: 至少 1 處

# 2. AndroidManifest 註冊
grep "MagnifierApplication" app/src/main/AndroidManifest.xml
# Expected: android:name=".MagnifierApplication"

# 3. 編譯時間
./gradlew clean && time ./gradlew assembleDebug
# Expected: clean build < 120s（無 kapt 開銷）

# 4. 旋轉螢幕 state 保留（ViewModelStore 生效）
# 手動：滑桿到 7x → 旋轉螢幕 → 仍 7x
```

### Risks

| risk_id | description | mitigation |
|---|---|---|
| R-1 | Option B 隨時間漸長變散亂 | AC-4 文檔化新增流程；季度 review 是否該升 Hilt |
| R-2 | Application class 內 `by lazy` 過多 → 啟動慢 | 改用顯式 init only-when-needed；或拆 `AppContainer` data class |
| R-3 | 選 Hilt 後反悔回 Option B → kapt cleanup 麻煩 | 決策前先在 spike branch 驗 1 個 ViewModel 流程，再決定全面採用 |

### Rollback

```bash
# 不論選哪個 option，回到「無 DI」即 git revert
git revert <merge-commit-sha>
# 副作用：MAG-A-001 ViewModel 需要重新處理依賴注入（退回 remember { new ... }）
```
