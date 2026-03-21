package com.google.android.piyush.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity_downloads")
data class EntityDownload(
    @PrimaryKey
    val videoId: String,
    val title: String?,
    val thumbnail: String?,
    val channelId: String?,
    val channelTitle: String?,
    val filePath: String?,
    val downloadId: Long = -1,
    val status: Int = STATUS_PENDING,
    val progress: Int = 0,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_FAILED = 3
        const val STATUS_PAUSED = 4
    }
}
