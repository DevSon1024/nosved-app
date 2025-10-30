package com.devson.nosved

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.*
import com.devson.nosved.download.DownloadRepository
import com.devson.nosved.download.DownloadService
import com.devson.nosved.util.VideoInfoUtil
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DownloadDatabase.getDatabase(application)
    // 1. Init Repository
    private val downloadRepository = DownloadRepository(database.downloadDao())
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

    // 2. Init Service
    private val downloadService = DownloadService(
        context,
        downloadRepository,
        notificationHelper,
        _downloadProgress,
        viewModelScope
    )

    private var currentFetchJob: Job? = null

    // 3. Get flows from repository
    val allDownloads = downloadRepository.allDownloads
    val runningDownloads = downloadRepository.runningDownloads
    val completedDownloads = downloadRepository.completedDownloads
    val failedDownloads = downloadRepository.failedDownloads

    init {
        notificationHelper.createNotificationChannel()
        initializeYoutubeDLLikeSeal()
        clearYoutubeDLCache()
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
        currentFetchJob?.cancel()
    }

    // This function is just for URL state management, it's fine
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") ||
                url.contains("youtube.com") || url.contains("youtu.be") ||
                url.contains("instagram.com") || url.contains("tiktok.com") ||
                url.contains("twitter.com") || url.contains("facebook.com")
    }

    // This is UI logic, keep it
    fun clearUrl() {
        currentFetchJob?.cancel()
        VideoInfoUtil.cancelFetch(_currentUrl.value)
        _currentUrl.value = ""
        _videoInfo.value = null
        _selectedVideoFormat.value = null
        _selectedAudioFormat.value = null
    }

    // This is info-fetching logic, not download execution. Keep it.
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

    // This is UI logic, keep it
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

    // These are UI state logic, keep them
    fun selectVideoFormat(format: VideoFormat) {
        _selectedVideoFormat.value = format
    }

    fun selectAudioFormat(format: VideoFormat) {
        _selectedAudioFormat.value = format
    }

    // This logic is about *selecting* quality, not executing download. Keep it.
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

    private fun parseQualityFromString(q: String): Int {
        return q.lowercase(Locale.ROOT)
            .replace("p", "")
            .replace("kbps", "")
            .trim()
            .toIntOrNull() ?: 0
    }


    // === 4. REFACTORED DOWNLOAD FUNCTIONS ===

    /**
     * This function now delegates the actual download to the DownloadService.
     */
    fun downloadVideoWithQuality(
        videoInfo: VideoInfo,
        customTitle: String,
        downloadMode: DownloadMode,
        preferredVideoQuality: String,
        preferredAudioQuality: String,
        preferredAudioContainer: String = "m4a",
        preferredVideoContainer: String = "mp4"
    ) {
        viewModelScope.launch (Dispatchers.IO) {
            val formats = videoInfo.formats ?: return@launch

            val targetVideoHeight = parseQualityFromString(preferredVideoQuality)
            val targetAudioBitrate = parseQualityFromString(preferredAudioQuality)

            when (downloadMode) {
                DownloadMode.AUDIO_ONLY -> {
                    val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, preferredAudioContainer)
                        ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")

                    if (selectedAudio != null) {
                        // Delegate to service
                        downloadService.startAudioDownload(videoInfo, selectedAudio, customTitle)
                        _selectedAudioFormat.value = selectedAudio
                        _selectedVideoFormat.value = null
                    } else showToast("❌ No suitable audio format found.")
                }
                DownloadMode.VIDEO_AUDIO -> {
                    val selectedVideo = findNearestVideoFormat(formats, targetVideoHeight, preferredVideoContainer)
                        ?: findNearestVideoFormat(formats, targetVideoHeight, "webm")
                    val selectedAudio = findNearestAudioFormat(formats, targetAudioBitrate, preferredAudioContainer)
                        ?: findNearestAudioFormat(formats, targetAudioBitrate, "webm")

                    if (selectedVideo != null && selectedAudio != null) {
                        // Delegate to service
                        downloadService.startVideoDownload(videoInfo, selectedVideo, selectedAudio, customTitle)
                        _selectedVideoFormat.value = selectedVideo
                        _selectedAudioFormat.value = selectedAudio
                    } else showToast("❌ No suitable video/audio format found.")
                }
            }
        }
    }

    /**
     * Deprecated: This function is replaced by downloadVideoWithQuality and delegates
     * to DownloadService.startVideoDownload
     */
    fun downloadVideo(videoInfo: VideoInfo, videoFormat: VideoFormat, audioFormat: VideoFormat, customTitle: String) {
        viewModelScope.launch (Dispatchers.IO) {
            downloadService.startVideoDownload(videoInfo, videoFormat, audioFormat, customTitle)
        }
    }

    /**
     * Delegate cancellation to the DownloadService.
     */
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            downloadService.cancelDownload(downloadId)
        }
    }

    /**
     * Delegate retry to the DownloadService.
     */
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            downloadService.retryDownload(downloadId)
        }
    }

    /**
     * Delegate deletion to the DownloadService.
     */
    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            downloadService.deleteDownload(downloadId)
        }
    }

    // === END OF REFACTORED FUNCTIONS ===


    // These are UI/Clipboard functions, keep them
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

    // This is UI/Clipboard logic, keep it
    private fun isValidUrlQuick(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) &&
                (url.contains("youtube.com") || url.contains("youtu.be") ||
                        url.contains("instagram.com") || url.contains("tiktok.com") ||
                        url.contains("twitter.com") || url.contains("facebook.com") ||
                        url.contains("vimeo.com"))
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

    // These helpers are now in DownloadUtils.kt

    // These are fine to keep
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