package com.devson.nosved

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    fun fetchVideoInfo(url: String) {
        viewModelScope.launch {
            _videoInfo.value = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().getInfo(url)
                } catch (e: Exception) {
                    Log.e("NosvedApp", "Failed to fetch video info", e)
                    null
                }
            }
        }
    }
}