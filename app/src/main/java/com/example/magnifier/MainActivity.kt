package com.example.magnifier

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.magnifier.ui.theme.MagnifierTheme
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagnifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MagnifierScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onImageDeleted: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 查詢所有儲存的圖片
    LaunchedEffect(Unit) {
        imageUris = queryMagnifierImages(context)
    }
    
    // 刪除選中的圖片
    fun deleteSelectedImages() {
        val count = selectedUris.size
        var deletedCount = 0
        selectedUris.forEach { uri ->
            try {
                var deleted = false
                
                // 嘗試使用 DocumentsContract 刪除（適用於 document URI）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && 
                    DocumentsContract.isDocumentUri(context, uri)) {
                    try {
                        deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                        if (deleted) {
                            Log.d("Magnifier", "使用 DocumentsContract 刪除圖片: $uri")
                        }
                    } catch (e: Exception) {
                        Log.d("Magnifier", "DocumentsContract 刪除失敗，嘗試其他方法: $uri", e)
                    }
                }
                
                // 如果 DocumentsContract 失敗，嘗試使用 MediaStore 刪除
                if (!deleted) {
                    val result = context.contentResolver.delete(uri, null, null)
                    if (result > 0) {
                        deleted = true
                        Log.d("Magnifier", "使用 MediaStore 刪除圖片: $uri")
                    } else {
                        Log.w("Magnifier", "刪除返回 0，可能圖片不存在或無權限: $uri")
                    }
                }
                
                if (deleted) {
                    deletedCount++
                    Log.d("Magnifier", "已刪除圖片: $uri")
                    // 通知父組件圖片已被刪除
                    onImageDeleted?.invoke(uri)
                } else {
                    Log.e("Magnifier", "無法刪除圖片: $uri")
                }
            } catch (e: SecurityException) {
                Log.e("Magnifier", "刪除圖片權限不足: $uri", e)
            } catch (e: Exception) {
                Log.e("Magnifier", "刪除圖片失敗: $uri", e)
            }
        }
        // 重新查詢圖片列表
        imageUris = queryMagnifierImages(context)
        selectedUris = emptySet()
        isSelectionMode = false
        android.widget.Toast.makeText(
            context,
            if (deletedCount > 0) "已刪除 $deletedCount 張圖片" else "刪除失敗，請檢查權限",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isSelectionMode) {
                            if (selectedUris.isEmpty()) "選擇照片" else "已選擇 ${selectedUris.size} 張"
                        } else {
                            "相簿"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedUris = emptySet()
                        } else if (selectedImageUri != null) {
                            // 如果正在查看圖片，只關閉圖片查看器
                            selectedImageUri = null
                        } else {
                            // 否則返回首頁
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode && selectedUris.isNotEmpty()) {
                        IconButton(onClick = { deleteSelectedImages() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "還沒有儲存任何圖片",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(imageUris) { uri ->
                    val isSelected = selectedUris.contains(uri)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(uri, isSelectionMode) {
                                detectTapGestures(
                                    onTap = {
                                        // 點擊處理
                                        if (isSelectionMode) {
                                            // 選擇模式下點擊切換選中狀態
                                            selectedUris = if (isSelected) {
                                                selectedUris - uri
                                            } else {
                                                selectedUris + uri
                                            }
                                        } else {
                                            // 普通模式下點擊查看大圖
                                            selectedImageUri = uri
                                        }
                                    },
                                    onLongPress = {
                                        // 長按進入選擇模式並勾選該圖片
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                        }
                                        selectedUris = selectedUris + uri
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "儲存的圖片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelectionMode) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isSelected) "已選中" else "未選中",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 全屏圖片查看器
        selectedImageUri?.let { uri ->
            ImageViewer(
                imageUri = uri,
                onClose = { selectedImageUri = null }
            )
        }
    }
}

@Composable
fun ImageViewer(
    imageUri: Uri,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // 處理系統返回按鈕，只關閉圖片查看器，不返回到首頁
    BackHandler(onBack = onClose)
    
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // 雙擊重置縮放和位置
                        scale = 1f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        // 背景
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black
        ) {}
        
        // 圖片（支持縮放和拖動）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "查看圖片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        // 頂部操作欄（放在最後以確保在最上層，並阻止圖片層攔截觸摸事件）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // 阻止觸摸事件傳遞到圖片層
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // 返回按鈕（左上角）
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

fun queryMagnifierImages(context: android.content.Context): List<Uri> {
    val imageUris = mutableListOf<Uri>()
    
    try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        } else {
            null
        }
        
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%/Magnifier/%")
        } else {
            null
        }
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                imageUris.add(contentUri)
            }
        }
    } catch (e: Exception) {
        Log.e("Magnifier", "查詢圖片失敗", e)
    }
    
    return imageUris
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    zoomLevel: Float,
    isFlashOn: Boolean,
    camera: Camera?,
    onZoomChange: (Float) -> Unit,
    onCameraReady: (Camera, ImageCapture, ProcessCameraProvider) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var currentCamera: Camera? by remember { mutableStateOf(null) }
    
    // 初始化相機
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView?.surfaceProvider)
        }
        
        val imageCapture = ImageCapture.Builder()
            .setFlashMode(
                if (isFlashOn) ImageCapture.FLASH_MODE_ON
                else ImageCapture.FLASH_MODE_OFF
            )
            .build()
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            currentCamera = camera
            // 相機初始化完成後立即應用初始縮放值
            camera.cameraControl.setZoomRatio(zoomLevel)
            onCameraReady(camera, imageCapture, cameraProvider)
        } catch (e: Exception) {
            Log.e("CameraPreview", "相機初始化失敗", e)
        }
    }
    
    // 更新縮放 - 使用 remember 來避免不必要的更新
    var lastZoomLevel by remember { mutableFloatStateOf(zoomLevel) }
    LaunchedEffect(zoomLevel, currentCamera) {
        if (currentCamera != null && kotlin.math.abs(zoomLevel - lastZoomLevel) > 0.01f) {
            currentCamera?.cameraControl?.setZoomRatio(zoomLevel)
            lastZoomLevel = zoomLevel
        }
    }
    
    // 更新手電筒
    LaunchedEffect(isFlashOn) {
        currentCamera?.cameraControl?.enableTorch(isFlashOn)
    }
    
    // 更新相機引用
    LaunchedEffect(camera) {
        currentCamera = camera
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView = it }
        },
        modifier = modifier
    )
}

fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap {
    // 檢查圖像格式
    val format = image.format
    val planes = image.planes
    
    return when {
        // YUV 格式（通常有 3 個 planes）
        format == ImageFormat.YUV_420_888 && planes.size >= 3 -> {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height),
                100,
                out
            )
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        // JPEG 格式（通常只有 1 個 plane）
        format == ImageFormat.JPEG && planes.size >= 1 -> {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        // 其他格式或未知格式，嘗試使用第一個 plane
        planes.size >= 1 -> {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) 
                ?: throw IllegalArgumentException("無法解碼圖像格式: $format")
        }
        else -> {
            throw IllegalArgumentException("不支持的圖像格式: $format, planes: ${planes.size}")
        }
    }
}

fun saveImageToGallery(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val displayName = "Magnifier_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 RELATIVE_PATH
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_PICTURES}/Magnifier")
            }
        }
        
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        outputStream.flush()
                        Log.d("Magnifier", "圖片已儲存: $uri")
                        uri
                    } else {
                        Log.e("Magnifier", "圖片壓縮失敗")
                        null
                    }
                } ?: run {
                    Log.e("Magnifier", "無法打開輸出流，URI: $uri")
                    null
                }
            } catch (e: Exception) {
                Log.e("Magnifier", "寫入圖片時發生錯誤", e)
                // 嘗試刪除已創建的條目
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (deleteException: Exception) {
                    Log.e("Magnifier", "刪除失敗的條目時發生錯誤", deleteException)
                }
                null
            }
        } else {
            Log.e("Magnifier", "無法創建 MediaStore 條目，可能是權限問題或儲存空間不足")
            null
        }
    } catch (e: SecurityException) {
        Log.e("Magnifier", "儲存圖片時權限不足", e)
        e.printStackTrace()
        null
    } catch (e: Exception) {
        Log.e("Magnifier", "儲存圖片時發生錯誤", e)
        e.printStackTrace()
        null
    }
}

@Composable
fun MagnifierScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(4f) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var lastSavedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showGallery by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
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
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
    
    // 根據狀態顯示相機或相簿
    if (showGallery) {
        GalleryScreen(
            onBack = { 
                showGallery = false
                // 從相簿返回時，檢查 lastSavedImageUri 是否還存在
                lastSavedImageUri?.let { uri ->
                    val allImages = queryMagnifierImages(context)
                    if (!allImages.contains(uri)) {
                        lastSavedImageUri = null
                    }
                }
            },
            onImageDeleted = { deletedUri ->
                // 如果刪除的圖片是當前顯示的縮圖，清除它
                if (lastSavedImageUri == deletedUri) {
                    lastSavedImageUri = null
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (hasCameraPermission) {
                // 顯示相機預覽
                CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        zoomLevel = zoomLevel,
                        isFlashOn = isFlashOn,
                        camera = camera,
                        onZoomChange = { newZoom: Float ->
                            zoomLevel = newZoom
                        },
                        onCameraReady = { cam, imgCap, provider ->
                            camera = cam
                            imageCapture = imgCap
                            cameraProvider = provider
                        },
                        lifecycleOwner = lifecycleOwner
                    )
                
                // 控制面板
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 放大倍率顯示和滑桿
                    Column {
                        Text(
                            text = "放大倍率: ${String.format("%.1f", zoomLevel)}x",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = zoomLevel,
                            onValueChange = { zoomLevel = it },
                            valueRange = 1f..10f,
                            steps = 89,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // 功能按鈕行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 手電筒開關
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                camera?.cameraControl?.enableTorch(isFlashOn)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = if (isFlashOn) "關閉手電筒" else "開啟手電筒",
                                tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        // 儲存到相簿（直接拍照並儲存）
                        IconButton(
                            onClick = {
                                imageCapture?.let { capture ->
                                    try {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        capture.takePicture(
                                            executor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                                    try {
                                                        val bitmap = imageProxyToBitmap(image)
                                                        val savedUri: Uri? = saveImageToGallery(context, bitmap)
                                                        if (savedUri != null) {
                                                            lastSavedImageUri = savedUri
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "圖片已儲存到相簿",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "儲存失敗，請檢查權限",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("Magnifier", "圖片處理或儲存失敗", e)
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "儲存失敗: ${e.message}",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    } finally {
                                                        image.close()
                                                    }
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e("Magnifier", "拍照失敗", e)
                                        android.widget.Toast.makeText(
                                            context,
                                            "拍照失敗: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = imageCapture != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "拍照儲存",
                                tint = if (imageCapture != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 顯示最後儲存的圖片縮圖
                        if (lastSavedImageUri != null) {
                            IconButton(
                                onClick = {
                                    showGallery = true
                                }
                            ) {
                                AsyncImage(
                                    model = lastSavedImageUri,
                                    contentDescription = "查看最後儲存的圖片",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    showGallery = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "查看相簿",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                } else {
                    Text(
                        text = "需要相機權限才能使用",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
