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
import java.util.UUID

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

    // Keep track of current fetch job to cancel if needed
    private var currentFetchJob: Job? = null

    // Download flows from database
    val allDownloads = downloadDao.getAllDownloads()
    val runningDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.DOWNLOADING)
    val completedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.COMPLETED)
    val failedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.FAILED)

    init {
        notificationHelper.createNotificationChannel()
        initializeYoutubeDLLikeSeal() // New method
        clearYoutubeDLCache() // Clear cache on startup
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
        // Cancel any ongoing fetch when URL changes
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

    // Replace the existing fetchVideoInfo method with this optimized version
    fun fetchVideoInfo(url: String) {
        // Cancel any existing fetch
        currentFetchJob?.cancel()
        VideoInfoUtil.cancelFetch(url)

        currentFetchJob = viewModelScope.launch {
            _isLoading.value = true
            _videoInfo.value = null
            _selectedVideoFormat.value = null
            _selectedAudioFormat.value = null

            try {
                VideoInfoUtil.fetchVideoInfoProgressive(url) { progress ->
                    when (progress.stage) {
                        "Validating URL" -> showToast("🔍 Validating URL...")
                        "Extracting info" -> showToast("⚡ Extracting video info...")
                        "Complete" -> {
                            progress.basicInfo?.let { info ->
                                _videoInfo.value = info
                                // Set default formats in background
                                launch(Dispatchers.Default) {
                                    setDefaultFormatsOptimized(info)
                                }
                                showToast("✅ Video info loaded!")
                            }
                        }
                        "Cache hit" -> {
                            progress.basicInfo?.let { info ->
                                _videoInfo.value = info
                                launch(Dispatchers.Default) {
                                    setDefaultFormatsOptimized(info)
                                }
                                showToast("⚡ Loaded from cache")
                            }
                        }
                    }
                }.onFailure { exception ->
                    Log.e("NosvedApp", "Failed to fetch video info", exception)
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
                Log.e("NosvedApp", "Unexpected error", e)
                showToast("❌ Unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add this new optimized method for setting default formats
    private suspend fun setDefaultFormatsOptimized(info: VideoInfo) {
        withContext(Dispatchers.Default) {
            try {
                val formats = info.formats ?: return@withContext

                // Parallel processing for format selection
                val audioJob = async {
                    formats.filter { it.acodec != "none" && it.vcodec == "none" }
                        .maxByOrNull { it.abr ?: 0 }
                }

                val videoJob = async {
                    formats.filter { it.vcodec != "none" && it.acodec == "none" }
                        .sortedByDescending { it.height ?: 0 }
                        .find { (it.height ?: 0) in 480..720 }
                        ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }
                            .maxByOrNull { it.height ?: 0 }
                }

                val bestAudio = audioJob.await()
                val bestVideo = videoJob.await()

                withContext(Dispatchers.Main) {
                    _selectedAudioFormat.value = bestAudio
                    _selectedVideoFormat.value = bestVideo
                }
            } catch (e: Exception) {
                Log.e("NosvedApp", "Error setting default formats", e)
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

                // Only paste, don't auto-fetch
                if (pastedText.isNotBlank()) {
                    showToast("📋 URL pasted successfully")
                } else {
                    showToast("📋 Clipboard is empty")
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

    fun pasteFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                _currentUrl.value = pastedText

                // Auto-fetch with improved speed (like Seal)
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

    // Add this helper method
    private fun isValidUrlQuick(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) &&
                (url.contains("youtube.com") || url.contains("youtu.be") ||
                        url.contains("instagram.com") || url.contains("tiktok.com") ||
                        url.contains("twitter.com") || url.contains("facebook.com") ||
                        url.contains("vimeo.com"))
    }

    fun selectVideoFormat(format: VideoFormat) {
        _selectedVideoFormat.value = format
    }

    fun selectAudioFormat(format: VideoFormat) {
        _selectedAudioFormat.value = format
    }

    fun downloadVideo(videoInfo: VideoInfo, videoFormat: VideoFormat, audioFormat: VideoFormat) {
        viewModelScope.launch {
            val downloadId = UUID.randomUUID().toString()

            val downloadEntity = DownloadEntity(
                id = downloadId,
                title = videoInfo.title ?: "Unknown Title",
                url = videoInfo.webpageUrl ?: "",
                thumbnail = videoInfo.thumbnail,
                filePath = null,
                fileName = null,
                status = DownloadStatus.QUEUED,
                duration = videoInfo.duration?.toString(),
                uploader = videoInfo.uploader,
                videoFormat = "${videoFormat.height}p",
                audioFormat = "${audioFormat.abr}kbps"
            )

            downloadDao.insertDownload(downloadEntity)
            showToast("Download started: ${videoInfo.title}")
            startDownload(downloadId, videoInfo, videoFormat, audioFormat)
        }
    }

    private suspend fun startDownload(
        downloadId: String,
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Clear cache before download
                clearYoutubeDLCache()

                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nosvedDir = File(downloadDir, "nosved")
                if (!nosvedDir.exists()) {
                    nosvedDir.mkdirs()
                }

                val sanitizedTitle = videoInfo.title?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "video"
                val fileName = "${sanitizedTitle}.%(ext)s"
                val filePath = File(nosvedDir, "${sanitizedTitle}.mp4")

                val request = videoInfo.webpageUrl?.let { YoutubeDLRequest(it) }

                // CORRECTED: Remove invalid options and use only supported ones
                request?.apply {
                    addOption("-o", File(nosvedDir, fileName).absolutePath)
                    addOption("-f", "${videoFormat.formatId}+${audioFormat.formatId}/best")
                    addOption("--merge-output-format", "mp4")

                    // ONLY use supported options for your version
                    addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    addOption("--referer", "https://www.youtube.com/")

                    // Performance options (these are widely supported)
                    addOption("--no-warnings")
                    addOption("--socket-timeout", "10")
                    addOption("--retries", "3")
                    addOption("--fragment-retries", "3")

                    // REMOVED these problematic options:
                    // addOption("--extract-flat", "false")  // ❌ This caused the error
                    // addOption("--no-cache-dir")          // ❌ May not be supported
                    // addOption("--concurrent-fragments", "4") // ❌ May cause issues
                }

                if (request != null) {
                    try {
                        YoutubeDL.getInstance().execute(request) { progress, _, line ->
                            Log.d("NosvedApp", "Download progress: $progress% - $line")

                            viewModelScope.launch {
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

                        // Success
                        downloadDao.updateDownload(
                            downloadDao.getDownloadById(downloadId)?.copy(
                                status = DownloadStatus.COMPLETED,
                                filePath = filePath.absolutePath,
                                fileName = fileName.replace(".%(ext)s", ".mp4"),
                                completedAt = System.currentTimeMillis(),
                                progress = 100
                            ) ?: return@withContext
                        )

                        showToast("✅ Download completed: ${videoInfo.title}")
                        notificationHelper.showDownloadCompleteNotification(
                            videoInfo.title ?: "Unknown Title",
                            filePath.absolutePath
                        )

                    } catch (e: YoutubeDLException) {
                        // Handle specific errors
                        when {
                            e.message?.contains("403") == true -> {
                                Log.w("NosvedApp", "403 error, trying simple download...")

                                // Fallback to simplest possible request
                                val simpleRequest = YoutubeDLRequest(videoInfo.webpageUrl!!)
                                simpleRequest.addOption("-o", File(nosvedDir, fileName).absolutePath)
                                simpleRequest.addOption("-f", "best")

                                try {
                                    YoutubeDL.getInstance().execute(simpleRequest)
                                    showToast("✅ Download completed (simple mode): ${videoInfo.title}")
                                } catch (retryError: Exception) {
                                    throw retryError
                                }
                            }
                            e.message?.contains("no such option") == true -> {
                                showToast("❌ Incompatible download options - updating needed")
                                throw e
                            }
                            else -> throw e
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("NosvedApp", "Failed to download video (${videoInfo.title})", e)

                val errorMessage = when {
                    e.message?.contains("no such option") == true -> "❌ App needs update for this feature"
                    e.message?.contains("403") == true -> "❌ Access blocked - Try again later"
                    e.message?.contains("network") == true -> "❌ Network error"
                    else -> "❌ Download failed: ${e.message?.take(50)}"
                }

                showToast(errorMessage)

                downloadDao.updateDownload(
                    downloadDao.getDownloadById(downloadId)?.copy(
                        status = DownloadStatus.FAILED,
                        error = e.message
                    ) ?: return@withContext
                )
            }
        }
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            downloadDao.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
            _downloadProgress.value = _downloadProgress.value - downloadId

            download?.let {
                showToast("Download cancelled: ${it.title}")
            }
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            if (download != null && download.status == DownloadStatus.FAILED) {
                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
                showToast("Retrying download: ${download.title}")
            }
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            download?.let {
                it.filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
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

    private fun initializeYoutubeDLLikeSeal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update youtube-dl on app start
                YoutubeDL.getInstance().updateYoutubeDL(context)
                Log.d("NosvedApp", "YoutubeDL updated successfully")
            } catch (e: Exception) {
                Log.w("NosvedApp", "Failed to update YoutubeDL", e)
            }
        }
    }

    private fun clearYoutubeDLCache() {
        try {
            YoutubeDL.getInstance().run {
                // Clear cache directory
                val cacheDir = File(context.cacheDir, "youtube-dl")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            }
            Log.d("NosvedApp", "YoutubeDL cache cleared")
        } catch (e: Exception) {
            Log.e("NosvedApp", "Failed to clear cache", e)
        }
    }


    private fun extractETA(line: String): String {
        val etaRegex = "ETA ([0-9:]+)".toRegex()
        return etaRegex.find(line)?.groupValues?.get(1) ?: ""
    }
}