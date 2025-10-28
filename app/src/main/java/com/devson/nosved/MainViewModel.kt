package com.devson.nosved

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.*
import com.devson.nosved.util.VideoInfoUtil
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DownloadDatabase.getDatabase(application)
    private val downloadDao = database.downloadDao()
    private val context = application.applicationContext

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedVideoFormat = MutableStateFlow<VideoFormat?>(null)
    val selectedVideoFormat = _selectedVideoFormat.asStateFlow()

    private val _selectedAudioFormat = MutableStateFlow<VideoFormat?>(null)
    val selectedAudioFormat = _selectedAudioFormat.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl = _currentUrl.asStateFlow()

    private val notificationHelper = NotificationHelper(application)

    private var currentFetchJob: Job? = null

    val allDownloads = downloadDao.getAllDownloads()
    val runningDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.DOWNLOADING)
    val completedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.COMPLETED)
    val failedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.FAILED)

    init {
        notificationHelper.createNotificationChannel()
        initializeYoutubeDLLikeSeal()
        clearYoutubeDLCache()
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
        currentFetchJob?.cancel()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") ||
                url.contains("youtube.com") || url.contains("youtu.be") ||
                url.contains("instagram.com") || url.contains("tiktok.com") ||
                url.contains("twitter.com") || url.contains("facebook.com")
    }

    fun clearUrl() {
        currentFetchJob?.cancel()
        VideoInfoUtil.cancelFetch(_currentUrl.value)
        _currentUrl.value = ""
        _videoInfo.value = null
        _selectedVideoFormat.value = null
        _selectedAudioFormat.value = null
    }

    fun fetchVideoInfo(url: String) {
        currentFetchJob?.cancel()
        VideoInfoUtil.cancelFetch(url)

        currentFetchJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _videoInfo.value = null
            _selectedVideoFormat.value = null
            _selectedAudioFormat.value = null

            try {
                VideoInfoUtil.fetchVideoInfoProgressive(url) { progress ->
                    when (progress.stage) {
                        "Validating URL" -> showToast("🔍 Validating URL...")
                        "Extracting info" -> showToast("⚡ Extracting video info...")
                        "Complete", "Cache hit" -> {
                            progress.basicInfo?.let { info ->
                                _videoInfo.value = info
                                launch(Dispatchers.Default) {
                                    setDefaultFormatsOptimized(info)
                                }
                                showToast("✅ Video info loaded!")
                            }
                        }
                    }
                }.onFailure { exception ->
                    val errorMessage = when {
                        exception is CancellationException -> "❌ Fetch cancelled"
                        exception is TimeoutCancellationException -> "⏱️ Request timed out - try again"
                        exception.message?.contains("network") == true -> "🌐 Network error - check connection"
                        exception.message?.contains("Invalid") == true -> "❌ Invalid or unsupported URL"
                        else -> "❌ Failed to get video info"
                    }
                    showToast(errorMessage)
                }
            } catch (e: Exception) {
                showToast("❌ Unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun setDefaultFormatsOptimized(info: VideoInfo) {
        withContext(Dispatchers.Default) {
            try {
                val formats = info.formats ?: return@withContext
                val audioJob = async { formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 } }
                val videoJob = async {
                    formats.filter { it.vcodec != "none" && it.acodec == "none" }
                        .sortedByDescending { it.height ?: 0 }
                        .find { (it.height ?: 0) in 480..720 }
                        ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }.maxByOrNull { it.height ?: 0 }
                }
                val bestAudio = audioJob.await()
                val bestVideo = videoJob.await()
                withContext(Dispatchers.Main) {
                    _selectedAudioFormat.value = bestAudio
                    _selectedVideoFormat.value = bestVideo
                }
            } catch (e: Exception) {
                // Skip
            }
        }
    }

    fun selectVideoFormat(format: VideoFormat) {
        _selectedVideoFormat.value = format
    }

    fun selectAudioFormat(format: VideoFormat) {
        _selectedAudioFormat.value = format
    }
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
        // Only equal or lower than requested, sorted descending
        val lowerOrEqual = candidates.filter { (it.abr ?: 0) <= preferredBitrate }
            .sortedByDescending { it.abr }

        // Pick highest available ≤ preferredBitrate, only if available
        if (lowerOrEqual.isNotEmpty()) return lowerOrEqual.first()
        // If none, pick highest of all candidates in that container
        return candidates.maxByOrNull { it.abr ?: 0 }
    }

    /**
     * Finds the best video format by requiring:
     * - Matching container
     * - Equal or lower than the preferred height, but as high as possible
     * - If nothing, picks lowest of that container only
     */
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

    fun downloadVideoWithQuality(
        videoInfo: VideoInfo,
        customTitle: String,
        downloadMode: DownloadMode,
        preferredVideoQuality: String,
        preferredAudioQuality: String,
        preferredAudioContainer: String = "m4a",
        preferredVideoContainer: String = "mp4"
    ) {
        viewModelScope.launch (Dispatchers.IO){
            val formats = videoInfo.formats ?: return@launch

            val targetVideoHeight = parseQualityFromString(preferredVideoQuality)
            val targetAudioBitrate = parseQualityFromString(preferredAudioQuality)

            var selectedVideo: VideoFormat? = null
            var selectedAudio: VideoFormat? = null

            when (downloadMode) {
                DownloadMode.AUDIO_ONLY -> {
                    selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, preferredAudioContainer)
                        ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    if (selectedAudio != null) {
                        downloadAudioOnly(videoInfo, selectedAudio, customTitle)
                        _selectedAudioFormat.value = selectedAudio
                        _selectedVideoFormat.value = null
                    } else showToast("❌ No suitable audio format found.")
                }
                DownloadMode.VIDEO_AUDIO -> {
                    selectedVideo = findNearestVideoFormat(formats, targetVideoHeight, preferredVideoContainer)
                        ?: findNearestVideoFormat(formats, targetVideoHeight, "webm")
                    selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, preferredAudioContainer)
                        ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")
                    if (selectedVideo != null && selectedAudio != null) {
                        downloadVideo(videoInfo, selectedVideo, selectedAudio, customTitle)
                        _selectedVideoFormat.value = selectedVideo
                        _selectedAudioFormat.value = selectedAudio
                    } else showToast("❌ No suitable video/audio format found.")
                }
            }
        }
    }

    private fun parseQualityFromString(q: String): Int {
        // Fix: Use lowercase() instead of toLowerCase(Locale.ROOT)
        return q.lowercase(Locale.ROOT)
            .replace("p", "")
            .replace("kbps", "")
            .trim()
            .toIntOrNull() ?: 0
    }

    // === DOWNLOAD FUNCTIONS ===

    fun downloadVideo(videoInfo: VideoInfo, videoFormat: VideoFormat, audioFormat: VideoFormat, customTitle: String) {
        viewModelScope.launch (Dispatchers.IO){
            val downloadId = UUID.randomUUID().toString()
            // Calculate the total size
            val totalSize = (videoFormat.fileSize ?: 0L) + (audioFormat.fileSize ?: 0L)

            val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }

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
            downloadDao.insertDownload(downloadEntity)
            showToast("Download started: $titleToUse")
            startDownload(downloadId, videoInfo, videoFormat, audioFormat, titleToUse)
        }
    }

    private suspend fun startDownload(
        downloadId: String,
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        titleToUse: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                clearYoutubeDLCache()
                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nosvedDir = File(downloadDir, "nosved")
                if (!nosvedDir.exists()) nosvedDir.mkdirs()
                val sanitizedTitle = titleToUse.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                val fileName = "${sanitizedTitle}.%(ext)s"
                val filePath = File(nosvedDir, "${sanitizedTitle}.mp4")
                val request = YoutubeDLRequest(videoInfo.webpageUrl ?: "")
                request.addOption("-o", File(nosvedDir, fileName).absolutePath)
                request.addOption("-f", "${videoFormat.formatId}+${audioFormat.formatId}/best")
                request.addOption("--merge-output-format", "mp4")
                request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                request.addOption("--referer", "https://www.youtube.com/")
                request.addOption("--no-warnings")
                request.addOption("--socket-timeout", "10")
                request.addOption("--retries", "3")
                request.addOption("--fragment-retries", "3")

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val progressData = DownloadProgress(
                            id = downloadId,
                            progress = if (progress > 0) progress.toInt() else 0,
                            downloadedSize = 0L,
                            totalSize = videoFormat.fileSize ?: 0L,
                            speed = extractSpeed(line),
                            eta = extractETA(line)
                        )
                        _downloadProgress.value = _downloadProgress.value + (downloadId to progressData)
                        downloadDao.updateDownloadProgress(downloadId, progressData.progress, 0L)
                        notificationHelper.showDownloadProgressNotification(line)
                    }
                }

                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)?.copy(
                        status = DownloadStatus.COMPLETED,
                        filePath = filePath.absolutePath,
                        fileName = fileName.replace(".%(ext)s", ".mp4"),
                        completedAt = System.currentTimeMillis(),
                        progress = 100
                    ) ?: return@withContext
                )
                showToast("✅ Download completed: $titleToUse")
                notificationHelper.showDownloadCompleteNotification(titleToUse, filePath.absolutePath)
            } catch (e: YoutubeDLException) {
                showToast("❌ Download failed: ${e.message ?: ""}")
                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)?.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message
                    ) ?: return@withContext
                )
            }
        }
    }

    private suspend fun downloadAudioOnly(videoInfo: VideoInfo, audioFormat: VideoFormat, customTitle: String) {
        val downloadId = UUID.randomUUID().toString()
        val titleToUse = customTitle.ifBlank { videoInfo.title ?: "Unknown Title" }

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
        downloadDao.insertDownload(downloadEntity)
        showToast("Audio download started: $titleToUse")
        startAudioDownload(downloadId, videoInfo, audioFormat, titleToUse)
    }

    private suspend fun startAudioDownload(
        downloadId: String,
        videoInfo: VideoInfo,
        audioFormat: VideoFormat,
        titleToUse: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                clearYoutubeDLCache()
                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nosvedDir = File(downloadDir, "nosved")
                if (!nosvedDir.exists()) nosvedDir.mkdirs()
                val sanitizedTitle = titleToUse.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                val fileName = "${sanitizedTitle}.%(ext)s"
                val audioExtension = audioFormat.ext ?: "mp3"
                val filePath = File(nosvedDir, "${sanitizedTitle}.${audioExtension}")

                val request = YoutubeDLRequest(videoInfo.webpageUrl ?: "")
                request.addOption("-o", File(nosvedDir, fileName).absolutePath)
                request.addOption("-f", audioFormat.formatId ?: "bestaudio")
                request.addOption("-x") // Extract audio
                request.addOption("--audio-format", audioExtension)
                request.addOption("--no-warnings")

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val progressData = DownloadProgress(
                            id = downloadId,
                            progress = if (progress > 0) progress.toInt() else 0,
                            downloadedSize = 0L,
                            totalSize = audioFormat.fileSize ?: 0L,
                            speed = extractSpeed(line),
                            eta = extractETA(line)
                        )
                        _downloadProgress.value = _downloadProgress.value + (downloadId to progressData)
                        downloadDao.updateDownloadProgress(downloadId, progressData.progress, 0L)
                        notificationHelper.showDownloadProgressNotification(line)
                    }
                }

                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)?.copy(
                        status = DownloadStatus.COMPLETED,
                        filePath = filePath.absolutePath,
                        fileName = "${sanitizedTitle}.${audioExtension}",
                        completedAt = System.currentTimeMillis(),
                        progress = 100
                    ) ?: return@withContext
                )
                showToast("✅ Audio download completed: $titleToUse")
                notificationHelper.showDownloadCompleteNotification(
                    "${titleToUse} (Audio)", filePath.absolutePath
                )
            } catch (e: Exception) {
                showToast("❌ Audio download failed: ${e.message ?: ""}")
                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)?.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message
                    ) ?: return@withContext
                )
            }
        }
    }

    fun pasteUrlOnly(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                _currentUrl.value = pastedText
                if (pastedText.isNotBlank()) showToast("📋 URL pasted successfully")
                else showToast("📋 Clipboard is empty")
                pastedText
            } else {
                showToast("📋 Clipboard is empty")
                ""
            }
        } catch (e: Exception) {
            showToast("❌ Failed to paste from clipboard")
            ""
        }
    }

    fun pasteFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                _currentUrl.value = pastedText
                if (pastedText.isNotBlank() && isValidUrlQuick(pastedText)) {
                    showToast("🔗 URL pasted - fetching info...")
                    fetchVideoInfo(pastedText)
                } else if (pastedText.isNotBlank()) {
                    showToast("⚠️ Invalid URL format")
                }
                pastedText
            } else {
                showToast("📋 Clipboard is empty")
                ""
            }
        } catch (e: Exception) {
            showToast("❌ Failed to paste from clipboard")
            ""
        }
    }

    private fun isValidUrlQuick(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) &&
                (url.contains("youtube.com") || url.contains("youtu.be") ||
                        url.contains("instagram.com") || url.contains("tiktok.com") ||
                        url.contains("twitter.com") || url.contains("facebook.com") ||
                        url.contains("vimeo.com"))
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch (Dispatchers.IO) {
            val download = downloadDao.getDownloadById(downloadId)
            downloadDao.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
            _downloadProgress.value = _downloadProgress.value - downloadId
            download?.let { showToast("Download cancelled: ${it.title}") }
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch (Dispatchers.IO){
            val download = downloadDao.getDownloadById(downloadId)
            if (download != null && download.status == DownloadStatus.FAILED) {
                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
                showToast("Retrying download: ${download.title}")
            }
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val download = downloadDao.getDownloadById(downloadId)
            download?.let {
                it.filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                downloadDao.deleteDownloadById(downloadId)
                showToast("Download deleted: ${it.title}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentFetchJob?.cancel()
        VideoInfoUtil.clearCache()
    }

    private fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractSpeed(line: String): String {
        val speedRegex = "([0-9.]+[KMG]iB/s)".toRegex()
        return speedRegex.find(line)?.value ?: ""
    }

    private fun extractETA(line: String): String {
        val etaRegex = "ETA ([0-9:]+)".toRegex()
        return etaRegex.find(line)?.groupValues?.get(1) ?: ""
    }

    private fun initializeYoutubeDLLikeSeal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(context)
            } catch (_: Exception) {}
        }
    }

    private fun clearYoutubeDLCache() {
        try {
            YoutubeDL.getInstance().run {
                val cacheDir = File(context.cacheDir, "youtube-dl")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            }
        } catch (_: Exception) {}
    }
}