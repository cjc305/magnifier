package com.example.magnifier.data.media

import android.graphics.Bitmap
import android.net.Uri

interface MediaRepository {
    suspend fun queryMagnifierImages(): List<Uri>
    suspend fun save(bitmap: Bitmap, displayName: String? = null): Result<Uri>
    suspend fun delete(uris: Set<Uri>): DeletionResult
}

data class DeletionResult(
    val deleted: Set<Uri>,
    val failed: Set<Uri>,
) {
    val deletedCount: Int get() = deleted.size
    val failedCount: Int get() = failed.size
}
