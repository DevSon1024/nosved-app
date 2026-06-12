package com.devson.nosved.data.service

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.devson.nosved.util.NotificationHelper
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.data.repository.DownloadRepository
import com.devson.nosved.data.QualityPreferences
import kotlinx.coroutines.flow.first
import com.devson.nosved.util.VideoInfoUtil
import com.devson.nosved.util.extractETA
import com.devson.nosved.util.extractSpeed
import com.devson.nosved.util.sanitizeTitle
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
import java.util.Locale
import java.util.UUID
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
        url: String,
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
            url = url,
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
            videoInfo,
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
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        outputExtension: String,
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId)
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isNotificationEnabled = prefs.getBoolean("download_notification_enabled", true)
        val detailedOutput = prefs.getBoolean("detailed_output", false)
        val saveThumbnail = prefs.getBoolean("save_thumbnail", true)
        val downloadPlaylist = prefs.getBoolean("download_playlist", false)
        val downloadArchive = prefs.getBoolean("download_archive", false)
        val enableSponsorsBlock = prefs.getBoolean("enable_sponsors_block", false)
        val incognitoMode = prefs.getBoolean("incognito_mode", false)
 
        val qualityPrefs = QualityPreferences(context)
        val embedMetadata = qualityPrefs.embedMetadata.first()
        val cropArtwork = qualityPrefs.cropArtwork.first()
        val remuxVideoContainer = qualityPrefs.remuxVideoContainer.first()
        val downloadSubtitlesOption = qualityPrefs.downloadSubtitles.first()
        val subtitleLanguages = qualityPrefs.customSubtitleLanguages.first()
        val convertSubtitles = qualityPrefs.convertSubtitles.first()
        val subtitleFormat = qualityPrefs.subtitleFormat.first()
        val downloadAutoCaptions = qualityPrefs.downloadAutoCaptions.first()
        val autoTranslatedSubtitles = qualityPrefs.autoTranslatedSubtitles.first()
        val embedSubtitles = qualityPrefs.embedSubtitles.first()
        val keepSubtitleFiles = qualityPrefs.keepSubtitleFiles.first()
        val formatSorting = qualityPrefs.formatSorting.first()
        val sortingFields = qualityPrefs.sortingFields.first()
        val preferredVideoQuality = qualityPrefs.videoQuality.first()
        val preferredVideoContainer = qualityPrefs.videoContainer.first().lowercase()
 
        val restrictFilenames = prefs.getBoolean("restrict_filenames", false)
        val template = prefs.getString("output_template", "%(title).200B.%(ext)s") ?: "%(title).200B.%(ext)s"
 
        try {
            if (repository.getDownloadById(downloadId)?.status != DownloadStatus.DOWNLOADING) {
                repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            }
            val nosvedDir = getTargetDir(downloadEntity.url, videoInfo, isAudioOnly = false, prefs = prefs)
 
            val actualExtension = if (remuxVideoContainer) "mkv" else outputExtension
            val finalFilePath = File(nosvedDir, "${sanitizedTitle}.${actualExtension}")
 
            val request = YoutubeDLRequest(downloadEntity.url)
            request.addOption("-o", File(nosvedDir, template).absolutePath)
 
            if (restrictFilenames) {
                request.addOption("--restrict-filenames")
            }
 
            val isPlaylist = isPlaylistUrl(downloadEntity.url)
            val formatStr = if (isPlaylist && downloadPlaylist) {
                val height = parseQualityFromString(preferredVideoQuality)
                val ext = if (preferredVideoContainer == "mp4" || preferredVideoContainer == "legacy") "mp4" else "webm"
                if (ext == "mp4") {
                    "bestvideo[height<=$height][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio/best"
                } else {
                    "bestvideo[height<=$height]+bestaudio/best"
                }
            } else {
                "${videoFormat.formatId}+${audioFormat.formatId}/best"
            }
            request.addOption("-f", formatStr)
 
            if (remuxVideoContainer) {
                request.addOption("--remux-video", "mkv")
                request.addOption("--merge-output-format", "mkv")
            } else {
                request.addOption("--merge-output-format", outputExtension)
            }
 
            request.addOption(
                "--user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            request.addOption("--referer", "https://www.youtube.com/")
            request.addOption("--no-warnings")
            request.addOption("--socket-timeout", "10")
            request.addOption("--retries", "3")
            request.addOption("--fragment-retries", "3")
 
            if (detailedOutput) {
                request.addOption("-v")
            }
            if (saveThumbnail) {
                request.addOption("--write-thumbnail")
            }
            if (!downloadPlaylist) {
                request.addOption("--no-playlist")
            }
            if (downloadArchive) {
                val archiveFile = File(context.filesDir, "download_archive.txt")
                request.addOption("--download-archive", archiveFile.absolutePath)
            }
            if (enableSponsorsBlock) {
                request.addOption("--sponsorblock-remove", "all")
            }
 
            if (embedMetadata) {
                request.addOption("--embed-metadata")
                request.addOption("--embed-thumbnail")
                request.addOption("--convert-thumbnails", "jpg")
 
                if (cropArtwork) {
                    try {
                        val configFile = File(context.cacheDir, "crop_config_${downloadId}.txt")
                        configFile.writeText("""--ppa "ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"""""")
                        request.addOption("--config", configFile.absolutePath)
                    } catch (_: Exception) {}
                }
            }
 
            val finalDownloadSubtitles = downloadSubtitles || downloadSubtitlesOption
            val finalSubtitleLang = if (subtitleLang.isNotEmpty()) subtitleLang else subtitleLanguages
 
            if (finalDownloadSubtitles) {
                if (downloadAutoCaptions) {
                    request.addOption("--write-auto-subs")
                    if (!autoTranslatedSubtitles) {
                        request.addOption("--extractor-args", "youtube:skip=translated_subs")
                    }
                }
                if (finalSubtitleLang.isNotEmpty()) {
                    request.addOption("--sub-langs", finalSubtitleLang)
                }
                if (embedSubtitles) {
                    request.addOption("--embed-subs")
                    if (keepSubtitleFiles) {
                        request.addOption("--write-subs")
                    }
                } else {
                    request.addOption("--write-subs")
                }
                if (convertSubtitles && subtitleFormat != "undefined" && subtitleFormat.isNotEmpty()) {
                    request.addOption("--convert-subs", subtitleFormat)
                }
            }
 
            if (formatSorting && sortingFields.isNotEmpty()) {
                request.addOption("-S", sortingFields)
            }
 
            var capturedFilePath: String? = null
 
            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val path = extractDestinationPath(line)
                    if (path != null) {
                        capturedFilePath = path
                    }
                    val newDescription = parseTaskDescription(line)
                    val oldProgress = progressFlow.value[downloadId]
                    val taskDescription =
                        newDescription ?: oldProgress?.taskDescription ?: "Downloading..."
 
                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L,
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line),
                        taskDescription = taskDescription
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    if (isNotificationEnabled) {
                        notificationHelper.showDownloadProgressNotification(
                            notificationId,
                            downloadEntity.title,
                            line
                        )
                    }
                }
            }
 
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId)
                return@withContext
            }
 
            val resolvedFilePath = if (!capturedFilePath.isNullOrBlank()) {
                File(capturedFilePath!!)
            } else {
                val ext = if (remuxVideoContainer) "mkv" else outputExtension
                val evaluatedName = template
                    .replace("%(title).200B", sanitizedTitle)
                    .replace("%(title)s", sanitizedTitle)
                    .replace("%(ext)s", ext)
                    .replace("%(id)s", downloadEntity.id)
                    .replace(Regex("%\\(\\w+\\)(?:\\.\\d+B)?s"), "")
                File(nosvedDir, evaluatedName)
            }
 
            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = resolvedFilePath.absolutePath,
                    fileName = resolvedFilePath.name,
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null
                )
            )
            showToast("Download completed: ${downloadEntity.title}")
            if (isNotificationEnabled) {
                notificationHelper.showDownloadCompleteNotification(
                    notificationId,
                    downloadEntity.title,
                    resolvedFilePath.absolutePath,
                    isAudioOnly = false
                )
            }
 
        } catch (e: Exception) {
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                showToast("Download cancelled: ${downloadEntity.title}")
            } else {
                showToast("Download failed: ${e.message ?: ""}")
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
            if (incognitoMode) {
                repository.deleteDownload(downloadId)
            }
            try {
                val configFile = File(context.cacheDir, "crop_config_${downloadId}.txt")
                if (configFile.exists()) {
                    configFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates a new DownloadEntity for an audio-only download,
     * inserts it into the repository, and starts the download execution.
     */
    suspend fun startAudioDownload(
        url: String,
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
            url = url,
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
            videoInfo,
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
        videoInfo: VideoInfo,
        audioFormat: VideoFormat,
        sanitizedTitle: String,
        audioExtension: String,
        downloadSubtitles: Boolean,
        subtitleLang: String
    ) = withContext(Dispatchers.IO) {
        val downloadId = downloadEntity.id
        val notificationId = getNotificationId(downloadId)
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isNotificationEnabled = prefs.getBoolean("download_notification_enabled", true)
        val detailedOutput = prefs.getBoolean("detailed_output", false)
        val saveThumbnail = prefs.getBoolean("save_thumbnail", true)
        val downloadPlaylist = prefs.getBoolean("download_playlist", false)
        val downloadArchive = prefs.getBoolean("download_archive", false)
        val enableSponsorsBlock = prefs.getBoolean("enable_sponsors_block", false)
        val incognitoMode = prefs.getBoolean("incognito_mode", false)
 
        val qualityPrefs = QualityPreferences(context)
        val embedMetadata = qualityPrefs.embedMetadata.first()
        val cropArtwork = qualityPrefs.cropArtwork.first()
        val downloadSubtitlesOption = qualityPrefs.downloadSubtitles.first()
        val subtitleLanguages = qualityPrefs.customSubtitleLanguages.first()
        val convertSubtitles = qualityPrefs.convertSubtitles.first()
        val subtitleFormat = qualityPrefs.subtitleFormat.first()
        val downloadAutoCaptions = qualityPrefs.downloadAutoCaptions.first()
        val autoTranslatedSubtitles = qualityPrefs.autoTranslatedSubtitles.first()
        val formatSorting = qualityPrefs.formatSorting.first()
        val sortingFields = qualityPrefs.sortingFields.first()
        val convertAudioEnabled = qualityPrefs.convertAudioFormatEnabled.first()
        val convertAudioFormat = qualityPrefs.convertAudioFormat.first()
        val preferredAudioQuality = qualityPrefs.audioQuality.first()
        val preferredAudioContainer = qualityPrefs.audioContainer.first().lowercase()
 
        val restrictFilenames = prefs.getBoolean("restrict_filenames", false)
        val template = prefs.getString("output_template", "%(title).200B.%(ext)s") ?: "%(title).200B.%(ext)s"
 
        try {
            if (repository.getDownloadById(downloadId)?.status != DownloadStatus.DOWNLOADING) {
                repository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            }
            val nosvedDir = getTargetDir(downloadEntity.url, videoInfo, isAudioOnly = true, prefs = prefs)
 
            val targetAudioExtension = if (convertAudioEnabled) convertAudioFormat else audioExtension
            val finalFilePath = File(nosvedDir, "${sanitizedTitle}.${targetAudioExtension}")
 
            val request = YoutubeDLRequest(downloadEntity.url)
            request.addOption("-o", File(nosvedDir, template).absolutePath)
 
            if (restrictFilenames) {
                request.addOption("--restrict-filenames")
            }
 
            val isPlaylist = isPlaylistUrl(downloadEntity.url)
            val formatStr = if (isPlaylist && downloadPlaylist) {
                val bitrate = parseQualityFromString(preferredAudioQuality)
                val ext = if (preferredAudioContainer == "m4a") "m4a" else "webm"
                "bestaudio[abr<=$bitrate][ext=$ext]/bestaudio[abr<=$bitrate]/bestaudio"
            } else {
                audioFormat.formatId ?: "bestaudio"
            }
            request.addOption("-f", formatStr)
            request.addOption("-x") // Extract audio
            request.addOption("--audio-format", targetAudioExtension)
            request.addOption("--no-warnings")
 
            if (detailedOutput) {
                request.addOption("-v")
            }
            if (saveThumbnail) {
                request.addOption("--write-thumbnail")
            }
            if (!downloadPlaylist) {
                request.addOption("--no-playlist")
            }
            if (downloadArchive) {
                val archiveFile = File(context.filesDir, "download_archive.txt")
                request.addOption("--download-archive", archiveFile.absolutePath)
            }
            if (enableSponsorsBlock) {
                request.addOption("--sponsorblock-remove", "all")
            }
 
            if (embedMetadata) {
                request.addOption("--embed-metadata")
                request.addOption("--embed-thumbnail")
                request.addOption("--convert-thumbnails", "jpg")
 
                if (cropArtwork) {
                    try {
                        val configFile = File(context.cacheDir, "crop_config_${downloadId}.txt")
                        configFile.writeText("""--ppa "ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"""""")
                        request.addOption("--config", configFile.absolutePath)
                    } catch (_: Exception) {}
                }
            }
 
            val finalDownloadSubtitles = downloadSubtitles || downloadSubtitlesOption
            val finalSubtitleLang = if (subtitleLang.isNotEmpty()) subtitleLang else subtitleLanguages
 
            if (finalDownloadSubtitles) {
                if (downloadAutoCaptions) {
                    request.addOption("--write-auto-subs")
                    if (!autoTranslatedSubtitles) {
                        request.addOption("--extractor-args", "youtube:skip=translated_subs")
                    }
                }
                if (finalSubtitleLang.isNotEmpty()) {
                    request.addOption("--sub-langs", finalSubtitleLang)
                }
                request.addOption("--write-subs")
                if (convertSubtitles && subtitleFormat != "undefined" && subtitleFormat.isNotEmpty()) {
                    request.addOption("--convert-subs", subtitleFormat)
                }
            }
 
            if (formatSorting && sortingFields.isNotEmpty()) {
                request.addOption("-S", sortingFields)
            }
 
            var capturedFilePath: String? = null
 
            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                coroutineScope.launch(Dispatchers.IO) {
                    val path = extractDestinationPath(line)
                    if (path != null) {
                        capturedFilePath = path
                    }
                    val newDescription = parseTaskDescription(line)
                    val oldProgress = progressFlow.value[downloadId]
                    val taskDescription =
                        newDescription ?: oldProgress?.taskDescription ?: "Downloading audio..."
 
                    val progressData = DownloadProgress(
                        id = downloadId,
                        progress = if (progress > 0) progress.toInt() else 0,
                        downloadedSize = 0L,
                        totalSize = downloadEntity.fileSize,
                        speed = extractSpeed(line),
                        eta = extractETA(line),
                        taskDescription = taskDescription
                    )
                    progressFlow.value = progressFlow.value + (downloadId to progressData)
                    repository.updateDownloadProgress(downloadId, progressData.progress, 0L)
                    if (isNotificationEnabled) {
                        notificationHelper.showDownloadProgressNotification(
                            notificationId,
                            downloadEntity.title,
                            line
                        )
                    }
                }
            }
 
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                notificationHelper.cancelNotification(notificationId)
                return@withContext
            }
 
            val resolvedFilePath = if (!capturedFilePath.isNullOrBlank()) {
                File(capturedFilePath!!)
            } else {
                val ext = if (convertAudioEnabled) convertAudioFormat else audioExtension
                val evaluatedName = template
                    .replace("%(title).200B", sanitizedTitle)
                    .replace("%(title)s", sanitizedTitle)
                    .replace("%(ext)s", ext)
                    .replace("%(id)s", downloadEntity.id)
                    .replace(Regex("%\\(\\w+\\)(?:\\.\\d+B)?s"), "")
                File(nosvedDir, evaluatedName)
            }
 
            repository.updateDownload(
                downloadEntity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = resolvedFilePath.absolutePath,
                    fileName = resolvedFilePath.name,
                    completedAt = System.currentTimeMillis(),
                    progress = 100,
                    error = null
                )
            )
            showToast("Audio download completed: ${downloadEntity.title}")
            if (isNotificationEnabled) {
                notificationHelper.showDownloadCompleteNotification(
                    notificationId,
                    downloadEntity.title,
                    resolvedFilePath.absolutePath,
                    isAudioOnly = true
                )
            }
 
        } catch (e: Exception) {
            if (repository.getDownloadById(downloadId)?.status == DownloadStatus.CANCELLED) {
                showToast("Download cancelled: ${downloadEntity.title}")
            } else {
                showToast("Audio download failed: ${e.message ?: ""}")
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
            if (incognitoMode) {
                repository.deleteDownload(downloadId)
            }
            try {
                val configFile = File(context.cacheDir, "crop_config_${downloadId}.txt")
                if (configFile.exists()) {
                    configFile.delete()
                }
            } catch (_: Exception) {}
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
            } catch (e: Exception) { /* Ignore */
            }
        }

        try {
            val videoInfoResult =
                VideoInfoUtil.fetchVideoInfoProgressive(existingEntity.url) { /* no progress */ }
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
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }
                        .maxByOrNull { it.abr ?: 0 }

                if (selectedAudio == null) throw Exception("Could not find suitable audio format")

                val updatedEntity = queuedEntity.copy(
                    fileSize = selectedAudio.fileSize ?: 0L,
                    audioFormat = "${selectedAudio.abr}kbps"
                )
                repository.updateDownload(updatedEntity)

                // Defaulting to false for subtitles on redownload
                executeAudioDownload(
                    updatedEntity,
                    videoInfo,
                    selectedAudio,
                    sanitizedTitle,
                    selectedAudio.ext ?: "mp3",
                    false,
                    ""
                )
 
            } else {
                val targetVideoHeight = parseQualityFromString(existingEntity.videoFormat)
                val targetAudioBitrate = parseQualityFromString(existingEntity.audioFormat)
 
                val selectedVideo = findNearestVideoFormat(formats, targetVideoHeight, "mp4")
                    ?: findNearestVideoFormat(formats, targetVideoHeight, "webm")
                    ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }
                        .maxByOrNull { it.height ?: 0 }
 
                val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, "m4a")
                    ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }
                        .maxByOrNull { it.abr ?: 0 }
 
                if (selectedVideo == null || selectedAudio == null) throw Exception("Could not find suitable video/audio formats")
 
                val newTotalSize = (selectedVideo.fileSize ?: 0L) + (selectedAudio.fileSize ?: 0L)
                val updatedEntity = queuedEntity.copy(
                    fileSize = if (newTotalSize > 0) newTotalSize else 0L,
                    videoFormat = "${selectedVideo.height}p",
                    audioFormat = "${selectedAudio.abr}kbps"
                )
                repository.updateDownload(updatedEntity)
 
                // Defaulting to false for subtitles on redownload
                executeVideoDownload(
                    updatedEntity,
                    videoInfo,
                    selectedVideo,
                    selectedAudio,
                    sanitizedTitle,
                    "mp4",
                    false,
                    ""
                )
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
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPlaylistUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.contains("list=", ignoreCase = true) || 
               trimmed.contains("/playlist", ignoreCase = true)
    }

    private fun getTargetDir(
        url: String,
        videoInfo: VideoInfo?,
        isAudioOnly: Boolean,
        prefs: android.content.SharedPreferences
    ): File {
        val key = if (isAudioOnly) "audio_download_folder" else "video_download_folder"
        val defaultPath = if (isAudioOnly) {
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/nosved/Audio"
        } else {
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/nosved"
        }
        val basePath = prefs.getString(key, defaultPath)?.ifBlank { defaultPath } ?: defaultPath
        var dir = File(basePath)

        val saveToSubdirWebsite = prefs.getBoolean("save_to_subdirectory_website", false)
        val saveToSubdirPlaylist = prefs.getBoolean("save_to_subdirectory_playlist", false)

        if (saveToSubdirWebsite) {
            val domain = getCleanDomain(url)
            dir = File(dir, domain)
        }

        if (saveToSubdirPlaylist && videoInfo != null) {
            val playlistName = try {
                val method = videoInfo.javaClass.methods.find { it.name == "getPlaylist" || it.name == "playlist" }
                method?.invoke(videoInfo) as? String
            } catch (e: Exception) {
                try {
                    val field = videoInfo.javaClass.fields.find { it.name == "playlist" }
                    field?.get(videoInfo) as? String
                } catch (ex: Exception) {
                    null
                }
            }
            if (!playlistName.isNullOrBlank()) {
                val sanitizedPlaylist = sanitizeTitle(playlistName)
                dir = File(dir, sanitizedPlaylist)
            }
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCleanDomain(url: String): String {
        return try {
            val host = java.net.URI(url).host?.lowercase(Locale.ROOT) ?: return "Other"
            when {
                host.contains("youtube.com") || host.contains("youtu.be") -> "Youtube"
                host.contains("vimeo.com") -> "Vimeo"
                host.contains("soundcloud.com") -> "SoundCloud"
                host.contains("instagram.com") -> "Instagram"
                host.contains("tiktok.com") -> "TikTok"
                host.contains("facebook.com") || host.contains("fb.watch") -> "Facebook"
                host.contains("twitter.com") || host.contains("x.com") -> "X"
                else -> {
                    val cleanHost = host.removePrefix("www.")
                    val domainName = cleanHost.substringBefore(".")
                    domainName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
            }
        } catch (e: Exception) {
            "Other"
        }
    }

    private fun extractDestinationPath(line: String): String? {
        return when {
            line.contains("[download] Destination:") -> {
                line.substringAfter("[download] Destination:").trim()
            }
            line.contains("[Merger] Merging formats into") -> {
                line.substringAfter("[Merger] Merging formats into").trim().removeSurrounding("\"")
            }
            line.contains("[ExtractAudio] Destination:") -> {
                line.substringAfter("[ExtractAudio] Destination:").trim()
            }
            line.contains("has already been downloaded") && line.contains("[download]") -> {
                line.substringAfter("[download]").substringBefore("has already been downloaded").trim()
            }
            line.contains("Merging formats into") -> {
                line.substringAfter("Merging formats into").trim().removeSurrounding("\"")
            }
            else -> null
        }
    }
}