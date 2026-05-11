package com.example.magnifier.data.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

fun queryMagnifierImages(context: Context): List<Uri> {
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
                val contentUri = ContentUris.withAppendedId(
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

fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri? {
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
