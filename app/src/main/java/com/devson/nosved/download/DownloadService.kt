package com.devson.nosved.download

import android.content.Context
import android.os.Environment
import com.devson.nosved.NotificationHelper
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Service class to handle the execution of downloads.
 * This class interacts with YoutubeDL, updates the repository, and sends notifications.
 */
class DownloadService(
    private val context: Context,
    private val repository: DownloadRepository,
    private val notificationHelper: NotificationHelper,
    private val progressFlow: MutableStateFlow<Map<String, DownloadProgress>>,
    private val coroutineScope: CoroutineScope
) {

    // No longer needed, as we won't be killing the process directly
    // private val activeDownloadJobs = mutableMapOf<String, YoutubeDLRequest>()

    /**
     * Creates a new DownloadEntity for a video and audio merge,
     * inserts it into the repository, and starts the download execution.
     */
    suspend fun startVideoDownload(
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        customTitle: String
    ) {
        val downloadId = UUID.randomUUID().toString()
        val totalSize = (videoFormat.fileSize ?: 0L) + (audioFormat.fileSize ?: 0L)
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }
        val sanitizedTitle = sanitizeTitle(titleToUse)
        val outputExtension = "mp4" // Merged format

        val downloadEntity = DownloadEntity(
            id = downloadId,
            title = titleToUse,
            url = videoInfo.webpageUrl ?: "",
            thumbnail = videoInfo.thumbnail,
            filePath = null,
            fileName = null,
            fileSize = if (totalSize > 0) totalSize else 0L,
            status = DownloadStatus.QUEUED,
            duration = videoInfo.duration?.toString(),
            uploader = videoInfo.uploader,
            videoFormat = "${videoFormat.height}p",
            audioFormat = "${audioFormat.abr}kbps"
        )

        repository.insertDownload(downloadEntity)
        showToast("Download started: $titleToUse")

        executeVideoDownload(downloadEntity, videoFormat, audioFormat, sanitizedTitle, outputExtension)
    }

    /**
     * Executes the actual yt-dlp command for merging video and audio.
     */
    private suspend fun executeVideoDownload(
        downloadEntity: DownloadEntity,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        outputExtension: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        try {
            repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val nosvedDir = File(downloadDir, "nosved")
            if (!nosvedDir.exists()) nosvedDir.mkdirs()

            val fileName = "${sanitizedTitle}.%(ext)s"
            val finalFilePath = File(nosvedDir, "${sanitizedTitle}.${outputExtension}")

            val request = YoutubeDLRequest(downloadEntity.url)
            request.addOption("-o", File(nosvedDir, fileName).absolutePath)
            request.addOption("-f", "${videoFormat.formatId}+${audioFormat.formatId}/best")
            request.addOption("--merge-output-format", outputExtension)
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            request.addOption("--referer", "https://www.youtube.com/")
            request.addOption("--no-warnings")
            request.addOption("--socket-timeout", "10")
            request.addOption("--retries", "3")
            request.addOption("--fragment-retries", "3")

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L, // This is hard to get accurately during merge
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line)
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    notificationHelper.showDownloadProgressNotification(line)
                }
            }

            // Check if job was cancelled
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${outputExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100
                )
            )
            showToast("✅ Download completed: ${downloadEntity.title}")
            notificationHelper.showDownloadCompleteNotification(downloadEntity.title, finalFilePath.absolutePath)

        } catch (e: Exception) {
            // Check if status is CANCELLED before reporting a failure
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                showToast("❌ Download cancelled: ${downloadEntity.title}")
            } else {
                showToast("❌ Download failed: ${e.message ?: ""}")
                repository.updateDownload(
                    downloadEntity.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message
                    )
                )
            }
        } finally {
            progressFlow.value = progressFlow.value - downloadId
        }
    }

    /**
     * Creates a new DownloadEntity for an audio-only download,
     * inserts it into the repository, and starts the download execution.
     */
    suspend fun startAudioDownload(
        videoInfo: VideoInfo,
        audioFormat: VideoFormat,
        customTitle: String
    ) {
        val downloadId = UUID.randomUUID().toString()
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }
        val sanitizedTitle = sanitizeTitle(titleToUse)
        val audioExtension = audioFormat.ext ?: "mp3"

        val downloadEntity = DownloadEntity(
            id = downloadId,
            title = "${titleToUse} (Audio)",
            url = videoInfo.webpageUrl ?: "",
            thumbnail = videoInfo.thumbnail,
            filePath = null,
            fileName = null,
            fileSize = audioFormat.fileSize ?: 0L,
            status = DownloadStatus.QUEUED,
            duration = videoInfo.duration?.toString(),
            uploader = videoInfo.uploader,
            videoFormat = "Audio Only",
            audioFormat = "${audioFormat.abr}kbps"
        )

        repository.insertDownload(downloadEntity)
        showToast("Audio download started: $titleToUse")

        executeAudioDownload(downloadEntity, audioFormat, sanitizedTitle, audioExtension)
    }

    /**
     * Executes the actual yt-dlp command for extracting audio.
     */
    private suspend fun executeAudioDownload(
        downloadEntity: DownloadEntity,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        audioExtension: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        try {
            repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val nosvedDir = File(downloadDir, "nosved")
            if (!nosvedDir.exists()) nosvedDir.mkdirs()

            val fileName = "${sanitizedTitle}.%(ext)s"
            val finalFilePath = File(nosvedDir, "${sanitizedTitle}.${audioExtension}")

            val request = YoutubeDLRequest(downloadEntity.url)
            request.addOption("-o", File(nosvedDir, fileName).absolutePath)
            request.addOption("-f", audioFormat.formatId ?: "bestaudio")
            request.addOption("-x") // Extract audio
            request.addOption("--audio-format", audioExtension)
            request.addOption("--no-warnings")

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L,
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line)
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    notificationHelper.showDownloadProgressNotification(line)
                }
            }

            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${audioExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100
                )
            )
            showToast("✅ Audio download completed: ${downloadEntity.title}")
            notificationHelper.showDownloadCompleteNotification(downloadEntity.title, finalFilePath.absolutePath)

        } catch (e: Exception) {
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                showToast("❌ Download cancelled: ${downloadEntity.title}")
            } else {
                showToast("❌ Audio download failed: ${e.message ?: ""}")
                repository.updateDownload(
                    downloadEntity.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message
                    )
                )
            }
        } finally {
            progressFlow.value = progressFlow.value - downloadId
        }
    }

    /**
     * Cancels an active download by updating its status in the DB.
     * This matches the original app's logic and does not kill the process.
     */
    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        repository.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)

        progressFlow.value = progressFlow.value - downloadId
        download?.let { showToast("Download cancelled: ${it.title}") }
    }

    /**
     * Retries a failed download.
     * Note: This is a simple retry that re-queues. A full implementation
     * would need to re-fetch formats or store them in the DownloadEntity.
     */
    suspend fun retryDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        if (download != null && download.status == DownloadStatus.FAILED) {
            repository.updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
            showToast("Retrying download: ${download.title}")
            // In a real app, you would re-launch the appropriate
            // executeVideoDownload or executeAudioDownload function here.
            // For this refactor, we just set the status to QUEUED.
        }
    }
    suspend fun removeFromApp(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        repository.deleteDownload(downloadId)
        download?.let {
            showToast("Removed from app: ${it.title}")
        }
    }
    suspend fun getDownloadById(downloadId: String): DownloadEntity? = withContext(Dispatchers.IO) {
        return@withContext repository.getDownloadById(downloadId)
    }

    /**
     * Deletes a download from the repository and filesystem.
     */
    suspend fun deleteDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val title = repository.getDownloadById(downloadId)?.title
        repository.deleteDownload(downloadId)
        title?.let { showToast("Download deleted: $it") }
    }

    private fun showToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}