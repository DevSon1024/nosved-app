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
        // Initialize YoutubeDL with minimal settings for speed
        initializeYoutubeDLForSpeed()
    }

    private fun initializeYoutubeDLForSpeed() {
        try {
            // Set global settings for faster execution
            System.setProperty("youtubedl.timeout", "5")
            System.setProperty("youtubedl.retries", "1")
        } catch (e: Exception) {
            Log.w("NosvedApp", "Failed to set system properties", e)
        }
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
        // Cancel any ongoing fetch when URL changes
        currentFetchJob?.cancel()
    }

    fun pasteFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                _currentUrl.value = pastedText

                // Auto-fetch info immediately after paste (like Seal does)
                if (pastedText.isNotBlank() && isValidUrl(pastedText)) {
                    fetchVideoInfo(pastedText)
                }

                showToast("URL pasted from clipboard")
                pastedText
            } else {
                showToast("Clipboard is empty")
                ""
            }
        } catch (e: Exception) {
            showToast("Failed to paste from clipboard")
            ""
        }
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
        // Cancel any existing fetch
        currentFetchJob?.cancel()
        VideoInfoUtil.cancelFetch(url)

        currentFetchJob = viewModelScope.launch {
            _isLoading.value = true
            _videoInfo.value = null
            _selectedVideoFormat.value = null
            _selectedAudioFormat.value = null

            try {
                // Show immediate loading state
                showToast("Fetching video info...")

                val result = VideoInfoUtil.fetchVideoInfoFast(url)

                result.onSuccess { info ->
                    _videoInfo.value = info

                    // Set default formats immediately
                    launch {
                        setDefaultFormats(info)
                    }

                    showToast("Video info loaded successfully")
                }.onFailure { exception ->
                    Log.e("NosvedApp", "Failed to fetch video info", exception)
                    val errorMessage = when {
                        exception is CancellationException -> "Fetch cancelled"
                        exception.message?.contains("timeout") == true -> "Request timed out - try again"
                        exception.message?.contains("network") == true -> "Network error - check connection"
                        else -> "Failed to get video info: ${exception.message}"
                    }
                    showToast(errorMessage)
                }
            } catch (e: CancellationException) {
                Log.d("NosvedApp", "Fetch cancelled")
            } catch (e: Exception) {
                Log.e("NosvedApp", "Unexpected error", e)
                showToast("Unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun setDefaultFormats(info: VideoInfo) {
        withContext(Dispatchers.Default) {
            try {
                // Set best audio format
                val bestAudioFormat = info.formats
                    ?.filter { it.acodec != "none" && it.vcodec == "none" }
                    ?.maxByOrNull { it.abr ?: 0 }

                // Set best video format (720p or best available)
                val bestVideoFormat = info.formats
                    ?.filter { it.vcodec != "none" && it.acodec == "none" }
                    ?.sortedByDescending { it.height ?: 0 }
                    ?.firstOrNull { (it.height ?: 0) <= 720 }
                    ?: info.formats?.filter { it.vcodec != "none" && it.acodec == "none" }?.firstOrNull()

                withContext(Dispatchers.Main) {
                    _selectedAudioFormat.value = bestAudioFormat
                    _selectedVideoFormat.value = bestVideoFormat
                }
            } catch (e: Exception) {
                Log.e("NosvedApp", "Error setting default formats", e)
            }
        }
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
                downloadDao.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nosvedDir = File(downloadDir, "nosved")
                if (!nosvedDir.exists()) {
                    nosvedDir.mkdirs()
                }

                val sanitizedTitle = videoInfo.title?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "video"
                val fileName = "${sanitizedTitle}.mp4"
                val filePath = File(nosvedDir, fileName)

                val request = videoInfo.webpageUrl?.let { YoutubeDLRequest(it) }
                request?.addOption("-o", filePath.absolutePath.replace(".mp4", ".%(ext)s"))
                request?.addOption("-f", "${videoFormat.formatId}+${audioFormat.formatId}")
                request?.addOption("--merge-output-format", "mp4")

                // Optimized download settings
                request?.addOption("--no-mtime")
                request?.addOption("--concurrent-fragments", "8")
                request?.addOption("--fragment-retries", "3")
                request?.addOption("--socket-timeout", "10")
                request?.addOption("--retries", "3")

                if (request != null) {
                    YoutubeDL.getInstance().execute(request) { progress, _, line ->
                        Log.d("NosvedApp", "Download progress: $progress% - $line")

                        viewModelScope.launch {
                            val progressData = DownloadProgress(
                                id = downloadId,
                                progress = progress.toInt(),
                                downloadedSize = 0L,
                                totalSize = videoFormat.fileSize ?: 0L,
                                speed = extractSpeed(line),
                                eta = extractETA(line)
                            )

                            _downloadProgress.value = _downloadProgress.value + (downloadId to progressData)
                            downloadDao.updateDownloadProgress(downloadId, progress.toInt(), 0L)
                            notificationHelper.showDownloadProgressNotification(line)
                        }
                    }

                    downloadDao.updateDownload(
                        downloadDao.getDownloadById(downloadId)?.copy(
                            status = DownloadStatus.COMPLETED,
                            filePath = filePath.absolutePath,
                            fileName = fileName,
                            completedAt = System.currentTimeMillis(),
                            progress = 100
                        ) ?: return@withContext
                    )

                    showToast("Download completed: ${videoInfo.title}")
                    notificationHelper.showDownloadCompleteNotification(
                        videoInfo.title ?: "Unknown Title",
                        filePath.absolutePath
                    )
                }

            } catch (e: YoutubeDLException) {
                Log.e("NosvedApp", "Failed to download video", e)
                showToast("Download failed: ${videoInfo.title}")

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

    private fun extractETA(line: String): String {
        val etaRegex = "ETA ([0-9:]+)".toRegex()
        return etaRegex.find(line)?.groupValues?.get(1) ?: ""
    }
}