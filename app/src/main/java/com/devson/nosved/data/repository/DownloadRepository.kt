package com.devson.nosved.data.repository

import com.devson.nosved.data.DownloadDao
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Repository for managing all download data operations, interacting directly with the DownloadDao.
 */
class DownloadRepository(private val downloadDao: DownloadDao) {

    // Single source of truth flow from Room, avoiding multiple SQLite observers
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    // Derived in-memory flows with zero SQLite contention
    val runningDownloads: Flow<List<DownloadEntity>> = allDownloads.map { list ->
        list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    }
    val completedDownloads: Flow<List<DownloadEntity>> = allDownloads.map { list ->
        list.filter { it.status == DownloadStatus.COMPLETED }
    }
    val failedDownloads: Flow<List<DownloadEntity>> = allDownloads.map { list ->
        list.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
    }

    suspend fun getDownloadById(id: String): DownloadEntity? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun insertDownload(download: DownloadEntity) {
        downloadDao.insertDownload(download)
    }

    suspend fun updateDownloadStatus(id: String, status: DownloadStatus) {
        downloadDao.updateDownloadStatus(id, status)
    }

    suspend fun updateDownloadProgress(id: String, progress: Int, downloadedSize: Long) {
        downloadDao.updateDownloadProgress(id, progress, downloadedSize)
    }

    suspend fun updateDownload(download: DownloadEntity) {
        downloadDao.updateDownload(download)
    }

    /**
     * Deletes a download from the database and removes its associated file from storage.
     */
    suspend fun deleteDownload(downloadId: String) {
        val download = getDownloadById(downloadId)
        download?.let {
            // Delete the file from storage if it exists
            it.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete the record from the database
            downloadDao.deleteDownloadById(downloadId)
        }
    }
}