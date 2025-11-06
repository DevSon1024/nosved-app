package com.devson.nosved.download

import android.content.Context
import android.os.Environment
import com.devson.nosved.NotificationHelper
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.util.VideoInfoUtil
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
import java.util.*
import kotlin.math.abs

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

    // Helper to generate a stable, unique integer ID for notifications
    private fun getNotificationId(downloadId: String): Int = abs(downloadId.hashCode())

    /**
     * Creates a new DownloadEntity for a video and audio merge,
     * inserts it into the repository, and starts the download execution.
     */
    suspend fun startVideoDownload(
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        customTitle: String,
        downloadSubtitles: Boolean, // Added for subtitles
        subtitleLang: String      // Added for subtitles
    ) {
        val downloadId = UUID.randomUUID().toString()
        val totalSize = (videoFormat.fileSize ?: 0L) + (audioFormat.fileSize ?: 0L)
        // Use the new, less restrictive sanitizeTitle function
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
            // You could also store subtitle info here
        )

        repository.insertDownload(downloadEntity)
        showToast("Download started: $titleToUse")

        executeVideoDownload(
            downloadEntity,
            videoFormat,
            audioFormat,
            sanitizedTitle,
            outputExtension,
            downloadSubtitles,
            subtitleLang
        )
    }

    /**
     * Executes the actual yt-dlp command for merging video and audio.
     */
    private suspend fun executeVideoDownload(
        downloadEntity: DownloadEntity,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        outputExtension: String,
        downloadSubtitles: Boolean, // Added
        subtitleLang: String      // Added
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId) // Get unique ID
        try {
            // Only update to DOWNLOADING if it's not already
            if (repository.getDownloadById(downloadId)?.status != DownloadStatus.DOWNLOADING) {
                repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            }
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

            // Add Subtitle Options
            if (downloadSubtitles) {
                request.addOption("--write-subs")
                request.addOption("--sub-lang", subtitleLang)
                request.addOption("--embed-subs") // Embed into video file
                request.addOption("--convert-subs", "srt") // Convert to srt first
            }

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
                    // Use new notification method
                    notificationHelper.showDownloadProgressNotification(notificationId, downloadEntity.title, line)
                }
            }

            // Check if job was cancelled
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId) // Cancel notification
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${outputExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null // Clear any previous error
                )
            )
            showToast("✅ Download completed: ${downloadEntity.title}")
            // Show complete notification (replaces progress one)
            notificationHelper.showDownloadCompleteNotification(notificationId, downloadEntity.title, finalFilePath.absolutePath)

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
            notificationHelper.cancelNotification(notificationId) // Cancel on failure/cancel
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
        customTitle: String,
        downloadSubtitles: Boolean, // Added
        subtitleLang: String      // Added
    ) {
        val downloadId = UUID.randomUUID().toString()
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }
        // Use the new, less restrictive sanitizeTitle function
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

        executeAudioDownload(
            downloadEntity,
            audioFormat,
            sanitizedTitle,
            audioExtension,
            downloadSubtitles,
            subtitleLang
        )
    }

    /**
     * Executes the actual yt-dlp command for extracting audio.
     */
    private suspend fun executeAudioDownload(
        downloadEntity: DownloadEntity,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        audioExtension: String,
        downloadSubtitles: Boolean, // Added
        subtitleLang: String      // Added
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId) // Get unique ID
        try {
            // Only update to DOWNLOADING if it's not already
            if (repository.getDownloadById(downloadId)?.status != DownloadStatus.DOWNLOADING) {
                repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            }
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

            // Add Subtitle Options
            if (downloadSubtitles) {
                request.addOption("--write-subs")
                request.addOption("--sub-lang", subtitleLang)
                request.addOption("--convert-subs", "srt")
                // Don't embed subs in audio, just save as separate file
            }

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
                    // Use new notification method
                    notificationHelper.showDownloadProgressNotification(notificationId, downloadEntity.title, line)
                }
            }

            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId) // Cancel notification
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${audioExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null // Clear any previous error
                )
            )
            showToast("✅ Audio download completed: ${downloadEntity.title}")
            // Show complete notification (replaces progress one)
            notificationHelper.showDownloadCompleteNotification(notificationId, downloadEntity.title, finalFilePath.absolutePath)

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
            notificationHelper.cancelNotification(notificationId) // Cancel on failure/cancel
        } finally {
            progressFlow.value = progressFlow.value - downloadId
        }
    }

    // *** HELPER FUNCTIONS (Copied from MainViewModel) ***

    private fun findNearestAudioFormat(
        formats: List<VideoFormat>,
        preferredBitrate: Int,
        preferredContainer: String
    ): VideoFormat? {
        val candidates = formats.filter {
            it.vcodec == "none" &&
                    it.acodec != "none" &&
                    it.abr != null &&
                    it.ext.equals(preferredContainer, ignoreCase = true)
        }
        val lowerOrEqual = candidates.filter { (it.abr ?: 0) <= preferredBitrate }
            .sortedByDescending { it.abr }
        if (lowerOrEqual.isNotEmpty()) return lowerOrEqual.first()
        return candidates.maxByOrNull { it.abr ?: 0 }
    }

    private fun findNearestVideoFormat(
        formats: List<VideoFormat>,
        preferredHeight: Int,
        preferredContainer: String
    ): VideoFormat? {
        val candidates = formats.filter {
            it.vcodec != "none" &&
                    it.acodec == "none" &&
                    it.height != null &&
                    it.ext.equals(preferredContainer, ignoreCase = true)
        }
        val lowerOrEqual = candidates.filter { (it.height ?: 0) <= preferredHeight }
            .sortedByDescending { it.height }
        if (lowerOrEqual.isNotEmpty()) return lowerOrEqual.first()
        return candidates.maxByOrNull { it.height ?: 0 }
    }

    private fun parseQualityFromString(q: String?): Int {
        if (q == null) return 0
        return q.lowercase(Locale.ROOT)
            .replace("p", "")
            .replace("kbps", "")
            .trim()
            .toIntOrNull() ?: 0
    }

    // *** END OF HELPER FUNCTIONS ***


    /**
     * Re-downloads an existing item using its stored quality settings.
     * This will reset the item's progress and re-fetch formats.
     */
    suspend fun redownloadVideoItem(downloadId: String) = withContext(Dispatchers.IO) {
        val existingEntity = repository.getDownloadById(downloadId)
        if (existingEntity == null) {
            showToast("❌ Redownload failed: Item not found")
            return@withContext
        }

        // 1. Show immediate feedback
        showToast("🔄 Re-fetching info for redownload...")

        // 2. Update status to QUEUED immediately
        // We set filePath to null so it's clear the old file is invalid
        val queuedEntity = existingEntity.copy(
            status = DownloadStatus.QUEUED,
            progress = 0,
            completedAt = null,
            error = null,
            filePath = null, // Old file path is no longer valid
            fileName = null
        )
        repository.updateDownload(queuedEntity)

        // 3. Delete the old file if it exists
        existingEntity.filePath?.let {
            try {
                val oldFile = File(it)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            } catch (e: Exception) {
                // Ignore if deletion fails, yt-dlp will just overwrite
            }
        }

        try {
            // 4. Fetch VideoInfo
            val videoInfoResult = VideoInfoUtil.fetchVideoInfoProgressive(existingEntity.url) { /* no progress */ }
            val videoInfo = videoInfoResult.getOrThrow() // Let the catch block handle failure

            val formats = videoInfo.formats ?: run {
                throw Exception("No formats found for video")
            }

            // 5. Find matching formats based on stored preferences
            // Remove " (Audio)" from title if it exists, to get base title
            val titleToUse = existingEntity.title.replace(" (Audio)", "")
            val sanitizedTitle = sanitizeTitle(titleToUse)

            if (existingEntity.videoFormat == "Audio Only") {
                // --- AUDIO ONLY REDOWNLOAD ---
                val targetAudioBitrate = parseQualityFromString(existingEntity.audioFormat)
                // Try to find the same container, fallback to any
                val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, "m4a")
                    ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }

                if (selectedAudio == null) throw Exception("Could not find suitable audio format")

                // Update entity with new file size and exact format info
                val updatedEntity = queuedEntity.copy(
                    fileSize = selectedAudio.fileSize ?: 0L,
                    audioFormat = "${selectedAudio.abr}kbps" // Update in case bitrate changed
                )
                repository.updateDownload(updatedEntity)

                // Start executeAudioDownload
                // NOTE: We don't know if user wanted subtitles. Defaulting to false.
                // A better fix is to store this preference in DownloadEntity
                executeAudioDownload(updatedEntity, selectedAudio, sanitizedTitle, selectedAudio.ext ?: "mp3", false, "")

            } else {
                // --- VIDEO + AUDIO REDOWNLOAD ---
                val targetVideoHeight = parseQualityFromString(existingEntity.videoFormat)
                val targetAudioBitrate = parseQualityFromString(existingEntity.audioFormat)

                val selectedVideo = findNearestVideoFormat(formats, targetVideoHeight, "mp4")
                    ?: findNearestVideoFormat(formats, targetVideoHeight, "webm")
                    ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }.maxByOrNull { it.height ?: 0 }

                val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, "m4a")
                    ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }

                if (selectedVideo == null || selectedAudio == null) throw Exception("Could not find suitable video/audio formats")

                // Update entity with new file size and exact format info
                val newTotalSize = (selectedVideo.fileSize ?: 0L) + (selectedAudio.fileSize ?: 0L)
                val updatedEntity = queuedEntity.copy(
                    fileSize = if (newTotalSize > 0) newTotalSize else 0L,
                    videoFormat = "${selectedVideo.height}p", // Update in case height changed
                    audioFormat = "${selectedAudio.abr}kbps" // Update in case bitrate changed
                )
                repository.updateDownload(updatedEntity)

                // Start executeVideoDownload
                // NOTE: Defaulting to false for subtitles
                executeVideoDownload(updatedEntity, selectedVideo, selectedAudio, sanitizedTitle, "mp4", false, "")
            }

        } catch (e: Exception) {
            // 6. Handle failure
            val errorMsg = e.message ?: "Unknown error"
            showToast("❌ Redownload failed: $errorMsg")
            repository.updateDownload(
                queuedEntity.copy(
                    status = DownloadStatus.FAILED,
                    error = "Redownload failed: $errorMsg"
                )
            )
        }
    }

    /**
     * Cancels an active download by updating its status in the DB.
     * This matches the original app's logic and does not kill the process.
     */
    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        repository.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)

        // Also cancel its notification
        notificationHelper.cancelNotification(getNotificationId(downloadId))

        progressFlow.value = progressFlow.value - downloadId
        download?.let { showToast("Download cancelled: ${it.title}") }
    }

    /**
     * Retries a failed download.
     * This now just calls the new redownload function.
     */
    suspend fun retryDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        if (download != null && (download.status == DownloadStatus.FAILED || download.status == DownloadStatus.CANCELLED)) {
            // Use the new redownload logic
            redownloadVideoItem(downloadId)
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
        val download = repository.getDownloadById(downloadId)
        // Delete file first
        download?.filePath?.let {
            try {
                val oldFile = File(it)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            } catch (e: Exception) {
                showToast("Could not delete file: ${e.message}")
            }
        }
        // Then delete from repository
        repository.deleteDownload(downloadId)
        download?.title?.let { showToast("Download deleted: $it") }
    }

    private fun showToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}