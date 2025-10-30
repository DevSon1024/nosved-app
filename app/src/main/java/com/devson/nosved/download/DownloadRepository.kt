package com.devson.nosved.download

import com.devson.nosved.data.DownloadDao
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository for managing all download data operations, interacting directly with the DownloadDao.
 */
class DownloadRepository(private val downloadDao: DownloadDao) {

    // Flows observed by the ViewModel
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val runningDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadsByStatus(DownloadStatus.DOWNLOADING)
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadsByStatus(DownloadStatus.COMPLETED)
    val failedDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadsByStatus(DownloadStatus.FAILED)

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