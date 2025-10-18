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
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // New state for URL management
    private val _currentUrl = MutableStateFlow("")
    val currentUrl = _currentUrl.asStateFlow()

    private val notificationHelper = NotificationHelper(application)

    // Download flows from database
    val allDownloads = downloadDao.getAllDownloads()
    val runningDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.DOWNLOADING)
    val completedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.COMPLETED)
    val failedDownloads = downloadDao.getDownloadsByStatus(DownloadStatus.FAILED)

    init {
        notificationHelper.createNotificationChannel()
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun pasteFromClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                _currentUrl.value = pastedText
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

    fun clearUrl() {
        _currentUrl.value = ""
        _videoInfo.value = null
        _selectedVideoFormat.value = null
        _selectedAudioFormat.value = null
    }

    fun fetchVideoInfo(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _videoInfo.value = null
            _selectedVideoFormat.value = null
            _selectedAudioFormat.value = null

            _videoInfo.value = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().getInfo(url)
                } catch (e: Exception) {
                    Log.e("NosvedApp", "Failed to fetch video info", e)
                    showToast("Failed to fetch video information")
                    null
                }
            }

            // Set default audio format (best quality)
            _videoInfo.value?.let { info ->
                val bestAudioFormat = info.formats
                    ?.filter { it.acodec != "none" && it.vcodec == "none" }
                    ?.maxByOrNull { it.abr ?: 0 }
                _selectedAudioFormat.value = bestAudioFormat
            }

            _isLoading.value = false
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

            // Show toast for download started
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

                if (request != null) {
                    YoutubeDL.getInstance().execute(request) { progress, _, line ->
                        Log.d("NosvedApp", "Download progress: $progress% - $line")

                        viewModelScope.launch {
                            val progressData = DownloadProgress(
                                id = downloadId,
                                progress = progress.toInt(),
                                downloadedSize = 0L, // YoutubeDL doesn't provide exact bytes
                                totalSize = videoFormat.fileSize ?: 0L,
                                speed = extractSpeed(line),
                                eta = extractETA(line)
                            )

                            _downloadProgress.value = _downloadProgress.value + (downloadId to progressData)
                            downloadDao.updateDownloadProgress(downloadId, progress.toInt(), 0L)
                            notificationHelper.showDownloadProgressNotification(line)
                        }
                    }

                    // Update completion status
                    downloadDao.updateDownload(
                        downloadDao.getDownloadById(downloadId)?.copy(
                            status = DownloadStatus.COMPLETED,
                            filePath = filePath.absolutePath,
                            fileName = fileName,
                            completedAt = System.currentTimeMillis(),
                            progress = 100
                        ) ?: return@withContext
                    )

                    // Show success toast
                    showToast("Download completed: ${videoInfo.title}")

                    notificationHelper.showDownloadCompleteNotification(
                        videoInfo.title ?: "Unknown Title",
                        filePath.absolutePath
                    )
                }

            } catch (e: YoutubeDLException) {
                Log.e("NosvedApp", "Failed to download video", e)

                // Show failure toast
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
                // Re-fetch video info and restart download
                // This is a simplified version - you might want to store format info
            }
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            download?.let {
                // Delete file if it exists
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
