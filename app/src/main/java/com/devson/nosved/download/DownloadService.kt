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
     * Helper to parse the yt-dlp output line for the current task.
     */
    private fun parseTaskDescription(line: String): String? {
        return when {
            line.contains("[Merger]") -> "Merging files..."
            line.contains("[ExtractAudio]") -> "Extracting audio..."
            // Check for audio-specific extensions
            line.contains("[download]") && (line.contains(".m4a") || line.contains(".opus") || line.contains(".mp3")) -> "Downloading audio..."
            // Check for video-specific extensions
            line.contains("[download]") && (line.contains(".mp4") || line.contains(".mkv") || line.contains(".webm")) -> "Downloading video..."
            // Generic download, but often the first step
            line.contains("[download] Destination:") -> "Initializing download..."
            // Fallback for any other download progress
            line.contains("[download]") && line.contains("ETA") -> "Downloading..."
            else -> null // No relevant update
        }
    }

    /**
     * Creates a new DownloadEntity for a video and audio merge,
     * inserts it into the repository, and starts the download execution.
     */
    suspend fun startVideoDownload(
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        customTitle: String,
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) {
        val downloadId = UUID.randomUUID().toString()
        val totalSize = (videoFormat.fileSize ?: 0L) + (audioFormat.fileSize ?: 0L)
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }
        val sanitizedTitle = sanitizeTitle(titleToUse) // From DownloadUtils.kt
        val outputExtension = "mp4"

        // Request medium quality (mq) instead of high quality (hq)
        val thumbnailUrl = videoInfo.thumbnail?.replace("hqdefault.jpg", "mqdefault.jpg")
            ?: videoInfo.thumbnail

        val downloadEntity = DownloadEntity(
            id = downloadId,
            title = titleToUse,
            url = videoInfo.webpageUrl ?: "",
            thumbnail = thumbnailUrl, // Use compressed thumbnail URL
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
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId)
        try {
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

            if (downloadSubtitles) {
                request.addOption("--write-subs")
                request.addOption("--sub-lang", subtitleLang)
                request.addOption("--embed-subs")
                request.addOption("--convert-subs", "srt")
            }

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val newDescription = parseTaskDescription(line)
                    val oldProgress = progressFlow.value[downloadId]
                    val taskDescription = newDescription ?: oldProgress?.taskDescription ?: "Downloading..."

                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L,
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line),
                        taskDescription = taskDescription // Set the task description
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    notificationHelper.showDownloadProgressNotification(notificationId, downloadEntity.title, line)
                }
            }

            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId)
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${outputExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null
                )
            )
            showToast("✅ Download completed: ${downloadEntity.title}")
            notificationHelper.showDownloadCompleteNotification(
                notificationId,
                downloadEntity.title,
                finalFilePath.absolutePath,
                isAudioOnly = false // This is a video download
            )

        } catch (e: Exception) {
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
            notificationHelper.cancelNotification(notificationId)
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
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) {
        val downloadId = UUID.randomUUID().toString()
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }
        val sanitizedTitle = sanitizeTitle(titleToUse)
        val audioExtension = audioFormat.ext ?: "mp3"

        // Request medium quality (mq) instead of high quality (hq)
        val thumbnailUrl = videoInfo.thumbnail?.replace("hqdefault.jpg", "mqdefault.jpg")
            ?: videoInfo.thumbnail

        val downloadEntity = DownloadEntity(
            id = downloadId,
            title = "${titleToUse} (Audio)",
            url = videoInfo.webpageUrl ?: "",
            thumbnail = thumbnailUrl, // Use compressed thumbnail URL
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
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId)
        try {
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

            if (downloadSubtitles) {
                request.addOption("--write-subs")
                request.addOption("--sub-lang", subtitleLang)
                request.addOption("--convert-subs", "srt")
            }

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val newDescription = parseTaskDescription(line)
                    val oldProgress = progressFlow.value[downloadId]
                    val taskDescription = newDescription ?: oldProgress?.taskDescription ?: "Downloading audio..."

                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L,
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line),
                        taskDescription = taskDescription // Set the task description
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    notificationHelper.showDownloadProgressNotification(notificationId, downloadEntity.title, line)
                }
            }

            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId)
                return@withContext
            }

            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFilePath.absolutePath,
                    fileName = "${sanitizedTitle}.${audioExtension}",
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null
                )
            )
            showToast("✅ Audio download completed: ${downloadEntity.title}")
            notificationHelper.showDownloadCompleteNotification(
                notificationId,
                downloadEntity.title,
                finalFilePath.absolutePath,
                isAudioOnly = true // This is an audio download
            )

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
            notificationHelper.cancelNotification(notificationId)
        } finally {
            progressFlow.value = progressFlow.value - downloadId
        }
    }

    // *** HELPER FUNCTIONS (For redownload logic) ***

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

        showToast("🔄 Re-fetching info for redownload...")

        val queuedEntity = existingEntity.copy(
            status = DownloadStatus.QUEUED,
            progress = 0,
            completedAt = null,
            error = null,
            filePath = null,
            fileName = null
        )
        repository.updateDownload(queuedEntity)

        existingEntity.filePath?.let {
            try {
                val oldFile = File(it)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            } catch (e: Exception) { /* Ignore */ }
        }

        try {
            val videoInfoResult = VideoInfoUtil.fetchVideoInfoProgressive(existingEntity.url) { /* no progress */ }
            val videoInfo = videoInfoResult.getOrThrow()

            val formats = videoInfo.formats ?: run {
                throw Exception("No formats found for video")
            }

            val titleToUse = existingEntity.title.replace(" (Audio)", "")
            val sanitizedTitle = sanitizeTitle(titleToUse)

            if (existingEntity.videoFormat == "Audio Only") {
                val targetAudioBitrate = parseQualityFromString(existingEntity.audioFormat)
                val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, "m4a")
                    ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }

                if (selectedAudio == null) throw Exception("Could not find suitable audio format")

                val updatedEntity = queuedEntity.copy(
                    fileSize = selectedAudio.fileSize ?: 0L,
                    audioFormat = "${selectedAudio.abr}kbps"
                )
                repository.updateDownload(updatedEntity)

                // Defaulting to false for subtitles on redownload
                executeAudioDownload(updatedEntity, selectedAudio, sanitizedTitle, selectedAudio.ext ?: "mp3", false, "")

            } else {
                val targetVideoHeight = parseQualityFromString(existingEntity.videoFormat)
                val targetAudioBitrate = parseQualityFromString(existingEntity.audioFormat)

                val selectedVideo = findNearestVideoFormat(formats, targetVideoHeight, "mp4")
                    ?: findNearestVideoFormat(formats, targetVideoHeight, "webm")
                    ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }.maxByOrNull { it.height ?: 0 }

                val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, "m4a")
                    ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }

                if (selectedVideo == null || selectedAudio == null) throw Exception("Could not find suitable video/audio formats")

                val newTotalSize = (selectedVideo.fileSize ?: 0L) + (selectedAudio.fileSize ?: 0L)
                val updatedEntity = queuedEntity.copy(
                    fileSize = if (newTotalSize > 0) newTotalSize else 0L,
                    videoFormat = "${selectedVideo.height}p",
                    audioFormat = "${selectedAudio.abr}kbps"
                )
                repository.updateDownload(updatedEntity)

                // Defaulting to false for subtitles on redownload
                executeVideoDownload(updatedEntity, selectedVideo, selectedAudio, sanitizedTitle, "mp4", false, "")
            }

        } catch (e: Exception) {
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
     */
    suspend fun cancelDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        repository.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
        notificationHelper.cancelNotification(getNotificationId(downloadId))
        progressFlow.value = progressFlow.value - downloadId
        download?.let { showToast("Download cancelled: ${it.title}") }
    }

    /**
     * Retries a failed or cancelled download.
     */
    suspend fun retryDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId)
        if (download != null && (download.status == DownloadStatus.FAILED || download.status == DownloadStatus.CANCELLED)) {
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
     * This version fixes the "Unresolved reference 'title'" error.
     */
    suspend fun deleteDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val download = repository.getDownloadById(downloadId) // Get the full download object

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

        // *** THIS IS THE FIX ***
        // We use the 'download' variable we already fetched
        download?.title?.let { showToast("Download deleted: $it") }
    }

    private fun showToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}