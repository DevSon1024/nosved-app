package com.devson.nosved

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
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
        }
    }

    fun downloadVideo(url: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val youtubeDLDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Nosved"
                )
                val request = YoutubeDLRequest(url)
                request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    Log.d("NosvedApp", "Download progress: $progress%, line: $line")
                    val progressPercent = progress.toInt()
                    notificationHelper.showDownloadProgressNotification(progressPercent)
                }

                notificationHelper.cancelNotification()
                // You can add a "Download Complete" notification here
            }
        }
    }
}