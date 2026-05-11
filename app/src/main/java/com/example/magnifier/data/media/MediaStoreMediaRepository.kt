package com.example.magnifier.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "MediaRepo"
private const val MAGNIFIER_RELATIVE_PATH_LIKE = "%/Magnifier/%"

class MediaStoreMediaRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaRepository {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun queryMagnifierImages(): List<Uri> = withContext(dispatcher) {
        val imageUris = mutableListOf<Uri>()
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
            )
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            } else null
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(MAGNIFIER_RELATIVE_PATH_LIKE)
            } else null
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    imageUris.add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查詢圖片失敗", e)
        }
        imageUris
    }

    override suspend fun save(bitmap: Bitmap, displayName: String?): Result<Uri> =
        withContext(dispatcher) {
            runCatching {
                val name = displayName ?: defaultDisplayName()
                val values = buildContentValues(name)
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("無法建立 MediaStore 條目，可能是權限問題或儲存空間不足")
                writeBitmap(uri, bitmap)
                Log.d(TAG, "圖片已儲存: $uri")
                uri
            }.onFailure { e ->
                Log.e(TAG, "儲存圖片時發生錯誤", e)
            }
        }

    override suspend fun delete(uris: Set<Uri>): DeletionResult = withContext(dispatcher) {
        val deleted = mutableSetOf<Uri>()
        val failed = mutableSetOf<Uri>()
        for (uri in uris) {
            if (deleteOne(uri)) deleted += uri else failed += uri
        }
        DeletionResult(deleted, failed)
    }

    // ---- private helpers ----

    private fun defaultDisplayName(): String =
        "Magnifier_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.jpg"

    private fun buildContentValues(displayName: String): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 RELATIVE_PATH
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Magnifier")
        }
    }

    private fun writeBitmap(uri: Uri, bitmap: Bitmap) {
        try {
            val outputStream = resolver.openOutputStream(uri)
                ?: error("無法打開輸出流: $uri")
            outputStream.use { os ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)) {
                    error("Bitmap.compress 回傳 false")
                }
                os.flush()
            }
        } catch (e: Exception) {
            // 清理已建立的 entry，避免留下空 row
            try {
                resolver.delete(uri, null, null)
            } catch (deleteException: Exception) {
                Log.e(TAG, "清理失敗 entry 時發生錯誤", deleteException)
            }
            throw e
        }
    }

    private fun deleteOne(uri: Uri): Boolean {
        try {
            // 嘗試使用 DocumentsContract 刪除（適用於 document URI）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                DocumentsContract.isDocumentUri(context, uri)
            ) {
                try {
                    if (DocumentsContract.deleteDocument(resolver, uri)) {
                        Log.d(TAG, "使用 DocumentsContract 刪除: $uri")
                        return true
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "DocumentsContract 失敗，嘗試 MediaStore: $uri", e)
                }
            }
            // MediaStore fallback
            val result = resolver.delete(uri, null, null)
            return if (result > 0) {
                Log.d(TAG, "使用 MediaStore 刪除: $uri")
                true
            } else {
                Log.w(TAG, "刪除返回 0，可能圖片不存在或無權限: $uri")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "刪除圖片權限不足: $uri", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "刪除圖片失敗: $uri", e)
            return false
        }
    }
}
