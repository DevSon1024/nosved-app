package com.devson.nosved

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showQualityDialog = MutableStateFlow(false)
    val showQualityDialog = _showQualityDialog.asStateFlow()

    private val notificationHelper = NotificationHelper(application)

    init {
        notificationHelper.createNotificationChannel()
    }

    fun fetchVideoInfo(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _videoInfo.value = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().getInfo(url)
                } catch (e: Exception) {
                    Log.e("NosvedApp", "Failed to fetch video info", e)
                    null
                }
            }
            _isLoading.value = false
            if (_videoInfo.value != null) {
                _showQualityDialog.value = true
            }
        }
    }

    fun showQualityDialog() {
        _showQualityDialog.value = true
    }

    fun hideQualityDialog() {
        _showQualityDialog.value = false
    }

    fun downloadVideo(videoInfo: VideoInfo, format: VideoFormat) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val nosvedDir = File(downloadDir, "nosved")
                if (!nosvedDir.exists()) {
                    nosvedDir.mkdirs()
                }

                val request = videoInfo.webpageUrl?.let { YoutubeDLRequest(it) }
                request?.addOption("-o", nosvedDir.absolutePath + "/%(title)s.%(ext)s")
                // Use the selected format's ID
                request?.addOption("-f", format.formatId.toString())

                try {
                    if (request != null) {
                        YoutubeDL.getInstance().execute(request) { _, _, line ->
                            Log.d("NosvedApp", "Download progress line: $line")
                            notificationHelper.showDownloadProgressNotification(line)
                        }
                    }
                } catch (e: YoutubeDLException) {
                    Log.e("NosvedApp", "Failed to download video", e)
                }

                notificationHelper.showDownloadCompleteNotification(
                    videoInfo.title.toString(),
                    nosvedDir.absolutePath
                )
            }
        }
    }
}