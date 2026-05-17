package com.example.magnifier.ui.magnifier

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.magnifier.MagnifierApplication
import com.example.magnifier.ui.UiEvent
import com.example.magnifier.ui.camera.CameraPreview
import com.example.magnifier.ui.gallery.GalleryScreen
import com.example.magnifier.ui.theme.LocalSpacing
import com.example.magnifier.ui.theme.NoirPalette

@Composable
fun MagnifierScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MagnifierApplication
    val container = app.container

    val viewModel: MagnifierViewModel = viewModel(
        factory = remember(container) {
            MagnifierViewModelFactory(
                container.mediaRepository,
                container.cameraController,
                container.permissionGate,
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast ->
                    android.widget.Toast.makeText(
                        context, event.message, android.widget.Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> viewModel.onMediaPermissionResult(results) }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        mediaPermissionLauncher.launch(viewModel.mediaPermissionList().toTypedArray())
    }

    // Soft crossfade between camera surface and gallery — no hard cut.
    Crossfade(
        targetState = uiState.showGallery,
        animationSpec = tween(durationMillis = 280),
        label = "magnifier-gallery-crossfade",
    ) { showGallery ->
        if (showGallery) {
            GalleryScreen(
                mediaRepository = container.mediaRepository,
                onBack = {
                    viewModel.showGallery(false)
                    viewModel.onGalleryReturn()
                },
                onImagesDeleted = { uris -> viewModel.onImagesDeleted(uris) },
            )
        } else {
            CameraView(
                cameraGranted = permissions.cameraGranted,
                uiState = uiState,
                onZoomChange = viewModel::setZoom,
                onToggleFlash = viewModel::toggleFlash,
                onCapture = viewModel::capture,
                onOpenGallery = { viewModel.showGallery(true) },
                container = container,
            )
        }
    }
}

@Composable
private fun CameraView(
    cameraGranted: Boolean,
    uiState: MagnifierUiState,
    onZoomChange: (Float) -> Unit,
    onToggleFlash: () -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    container: com.example.magnifier.di.AppContainer,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cameraGranted) {
                CameraPreview(
                    controller = container.cameraController,
                    modifier = Modifier.fillMaxSize(),
                )

                FloatingControlCapsule(
                    zoom = uiState.zoomLevel,
                    isFlashOn = uiState.isFlashOn,
                    lastSavedImageUri = uiState.lastSavedImageUri,
                    onZoomChange = onZoomChange,
                    onToggleFlash = onToggleFlash,
                    onCapture = onCapture,
                    onOpenGallery = onOpenGallery,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
                PermissionEmptyState()
            }
        }
    }
}

// Floating glass capsule — replaces the edge-to-edge gradient overlay.
// Liquid-glass approximation via layered translucent fills + subtle border,
// since true backdrop blur (RenderEffect) only exists on API 31+ and we
// need to support API 24+. The amber-tinted inner gradient gives a sense
// of warm ambient light without expensive GPU blur.
@Composable
private fun FloatingControlCapsule(
    zoom: Float,
    isFlashOn: Boolean,
    lastSavedImageUri: android.net.Uri?,
    onZoomChange: (Float) -> Unit,
    onToggleFlash: () -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val capsuleShape = RoundedCornerShape(spacing.xxxl)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xxl)
            .shadow(elevation = 16.dp, shape = capsuleShape, clip = false)
            .clip(capsuleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NoirPalette.SurfaceContainerHigh.copy(alpha = 0.88f),
                        NoirPalette.SurfaceContainer.copy(alpha = 0.92f),
                    )
                ),
                shape = capsuleShape,
            )
            .border(
                width = 0.5.dp,
                color = NoirPalette.Outline.copy(alpha = 0.6f),
                shape = capsuleShape,
            )
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ZoomReadout(zoom = zoom, onZoomChange = onZoomChange)
        ControlRow(
            isFlashOn = isFlashOn,
            lastSavedImageUri = lastSavedImageUri,
            onToggleFlash = onToggleFlash,
            onCapture = onCapture,
            onOpenGallery = onOpenGallery,
        )
    }
}

@Composable
private fun ZoomReadout(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
) {
    val spacing = LocalSpacing.current
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format("%.1f", zoom),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(spacing.xs))
            Text(
                text = "x",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = spacing.xs),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "ZOOM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = spacing.sm),
            )
        }
        Slider(
            value = zoom,
            onValueChange = onZoomChange,
            valueRange = 1f..10f,
            steps = 89,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "放大倍率,目前 ${"%.1f".format(zoom)} 倍,範圍 1 到 10 倍"
                },
        )
    }
}

@Composable
private fun ControlRow(
    isFlashOn: Boolean,
    lastSavedImageUri: android.net.Uri?,
    onToggleFlash: () -> Unit,
    onCapture: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleFlash,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = if (isFlashOn) "關閉手電筒" else "開啟手電筒",
                tint = if (isFlashOn) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp),
            )
        }

        CaptureFab(onCapture = onCapture)

        if (lastSavedImageUri != null) {
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier.size(56.dp),
            ) {
                AsyncImage(
                    model = lastSavedImageUri,
                    contentDescription = "查看相簿,顯示最後一張儲存的圖片",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
            }
        } else {
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "查看相簿",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

// Primary action — spring press feedback, amber-tinted shadow glow.
// NoBouncy damping keeps it premium-utility, not toy-bouncy.
@Composable
private fun CaptureFab(onCapture: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "capture-press-scale",
    )

    FilledIconButton(
        onClick = onCapture,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = NoirPalette.Shadow,
                spotColor = NoirPalette.Shadow,
            ),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "拍照儲存",
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun PermissionEmptyState() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xxxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "需要相機權限",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = "本 App 用相機畫面做即時放大,沒有相機權限就無法運作。請到系統設定打開「相機」權限後重新進入。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
