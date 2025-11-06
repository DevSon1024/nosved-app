package com.devson.nosved.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val thumbnail: String?,
    val filePath: String?,
    val fileName: String?,
    val fileSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: DownloadStatus,
    val progress: Int = 0,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val duration: String? = null,
    val uploader: String? = null,
    val videoFormat: String? = null,
    val audioFormat: String? = null
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val id: String,
    val progress: Int = 0,
    val downloadedSize: Long = 0L,
    val totalSize: Long = 0L,
    val speed: String = "",
    val eta: String = "",
    val taskDescription: String? = "Initializing..."
)
