package com.example.magnifier.ui.magnifier

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.example.magnifier.data.camera.CameraController
import com.example.magnifier.data.media.DeletionResult
import com.example.magnifier.data.media.MediaRepository
import com.example.magnifier.data.permission.AppPermissions
import com.example.magnifier.data.permission.PermissionGate
import com.example.magnifier.ui.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.Mockito.mock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MagnifierViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setZoom updates state and forwards to controller clamping to 1 through 10`() = runTest(testDispatcher.scheduler) {
        val controller = RecordingCameraController()
        val vm = newViewModel(camera = controller)

        vm.setZoom(7f)
        assertEquals(7f, vm.uiState.value.zoomLevel, 0.0001f)
        assertEquals(7f, controller.lastZoom!!, 0.0001f)

        vm.setZoom(11f)
        assertEquals(10f, vm.uiState.value.zoomLevel, 0.0001f)
        assertEquals(10f, controller.lastZoom!!, 0.0001f)

        vm.setZoom(0.3f)
        assertEquals(1f, vm.uiState.value.zoomLevel, 0.0001f)
    }

    @Test
    fun `toggleFlash flips state and forwards torch to controller`() = runTest(testDispatcher.scheduler) {
        val controller = RecordingCameraController()
        val vm = newViewModel(camera = controller)

        assertEquals(false, vm.uiState.value.isFlashOn)
        vm.toggleFlash()
        assertEquals(true, vm.uiState.value.isFlashOn)
        assertEquals(true, controller.lastTorch)

        vm.toggleFlash()
        assertEquals(false, vm.uiState.value.isFlashOn)
        assertEquals(false, controller.lastTorch)
    }

    @Test
    fun `capture happy path updates lastSavedImageUri and emits success toast`() = runTest(testDispatcher.scheduler) {
        val savedUri = mock(Uri::class.java)
        val controller = RecordingCameraController(captureResult = Result.success(FAKE_BITMAP))
        val repo = RecordingMediaRepository(saveResult = Result.success(savedUri))
        val vm = newViewModel(camera = controller, media = repo)

        vm.capture()
        advanceUntilIdle()

        assertEquals(savedUri, vm.uiState.value.lastSavedImageUri)
        assertEquals(1, repo.saveCalls)
        val event = vm.events.first()
        assertTrue(event is UiEvent.ShowToast)
        assertEquals("圖片已儲存到相簿", (event as UiEvent.ShowToast).message)
    }

    @Test
    fun `capture failure leaves state unchanged and emits failure toast`() = runTest(testDispatcher.scheduler) {
        val controller = RecordingCameraController(
            captureResult = Result.failure(RuntimeException("hardware busy"))
        )
        val repo = RecordingMediaRepository()
        val vm = newViewModel(camera = controller, media = repo)

        vm.capture()
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastSavedImageUri)
        assertEquals(0, repo.saveCalls)
        val event = vm.events.first()
        assertTrue(event is UiEvent.ShowToast)
        assertEquals(
            "拍照失敗: hardware busy",
            (event as UiEvent.ShowToast).message
        )
    }

    @Test
    fun `capture saves but repo fails — state stays empty, failure toast emitted`() = runTest(testDispatcher.scheduler) {
        val controller = RecordingCameraController(captureResult = Result.success(FAKE_BITMAP))
        val repo = RecordingMediaRepository(
            saveResult = Result.failure(SecurityException("denied"))
        )
        val vm = newViewModel(camera = controller, media = repo)

        vm.capture()
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastSavedImageUri)
        assertEquals(1, repo.saveCalls)
        val event = vm.events.first()
        assertEquals("儲存失敗，請檢查權限", (event as UiEvent.ShowToast).message)
    }

    @Test
    fun `onImagesDeleted clears lastSavedImageUri when current uri is in deleted set`() = runTest(testDispatcher.scheduler) {
        val savedUri = mock(Uri::class.java)
        val controller = RecordingCameraController(captureResult = Result.success(FAKE_BITMAP))
        val repo = RecordingMediaRepository(saveResult = Result.success(savedUri))
        val vm = newViewModel(camera = controller, media = repo)

        vm.capture()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.lastSavedImageUri)

        vm.onImagesDeleted(setOf(savedUri))
        assertNull(vm.uiState.value.lastSavedImageUri)
    }

    @Test
    fun `onImagesDeleted leaves lastSavedImageUri when uri is not in deleted set`() = runTest(testDispatcher.scheduler) {
        val savedUri = mock(Uri::class.java)
        val controller = RecordingCameraController(captureResult = Result.success(FAKE_BITMAP))
        val repo = RecordingMediaRepository(saveResult = Result.success(savedUri))
        val vm = newViewModel(camera = controller, media = repo)

        vm.capture()
        advanceUntilIdle()

        val otherUri = mock(Uri::class.java)
        vm.onImagesDeleted(setOf(otherUri))
        assertEquals(savedUri, vm.uiState.value.lastSavedImageUri)
    }

    // ---- Fixtures ----

    private fun newViewModel(
        media: MediaRepository = RecordingMediaRepository(),
        camera: CameraController = RecordingCameraController(),
        gate: PermissionGate = FakePermissionGate(),
    ) = MagnifierViewModel(media, camera, gate)

    private class RecordingMediaRepository(
        val saveResult: Result<Uri> = Result.success(mock(Uri::class.java)),
    ) : MediaRepository {
        var saveCalls = 0
        var queryCalls = 0
        override suspend fun queryMagnifierImages(): List<Uri> {
            queryCalls++
            return emptyList()
        }

        override suspend fun save(bitmap: Bitmap, displayName: String?): Result<Uri> {
            saveCalls++
            return saveResult
        }

        override suspend fun delete(uris: Set<Uri>): DeletionResult =
            DeletionResult(deleted = uris, failed = emptySet())
    }

    private class RecordingCameraController(
        val captureResult: Result<Bitmap> = Result.success(FAKE_BITMAP),
        initialMaxZoom: Float = 10f,   // mirrors the historical test range
    ) : CameraController {
        var lastZoom: Float? = null
        var lastTorch: Boolean? = null
        var bindCalls = 0
        var releaseCalls = 0
        private val _maxZoom = MutableStateFlow(initialMaxZoom)
        override val maxZoomRatio: StateFlow<Float> = _maxZoom.asStateFlow()
        fun emitMaxZoom(max: Float) { _maxZoom.value = max }
        override fun setSurfaceProvider(provider: Preview.SurfaceProvider) {}
        override suspend fun bind(lifecycleOwner: LifecycleOwner): Result<Unit> {
            bindCalls++
            return Result.success(Unit)
        }
        override fun setZoom(ratio: Float) { lastZoom = ratio }
        override fun setTorch(enabled: Boolean) { lastTorch = enabled }
        override suspend fun capture(): Result<Bitmap> = captureResult
        override fun release() { releaseCalls++ }
    }

    private class FakePermissionGate : PermissionGate {
        private val flow = MutableStateFlow(AppPermissions())
        override val state: StateFlow<AppPermissions> = flow.asStateFlow()
        override fun mediaPermissions(): List<String> = emptyList()
        override fun onCameraResult(granted: Boolean) {}
        override fun onMediaResult(results: Map<String, Boolean>) {}
        override fun refresh() {}
    }

    companion object {
        // android.graphics.Bitmap is final + JVM-stubbed; use Mockito's inline mock-maker
        // to create a Bitmap reference for plumbing. We never call methods on it; only
        // identity matters for the capture→save flow.
        private val FAKE_BITMAP: Bitmap = mock(Bitmap::class.java)
    }
}
