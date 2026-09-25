package com.devson.nosved

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NosvedApp : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "NosvedApp"
        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initNativeBinariesAsync()
    }

    private fun initNativeBinariesAsync() {
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@NosvedApp)
                FFmpeg.getInstance().init(this@NosvedApp)
                Aria2c.init(this@NosvedApp)
                _isInitialized.value = true
                Log.d(TAG, "yt-dlp, FFmpeg, and Aria2c initialized on background IO dispatcher")
            } catch (e: YoutubeDLException) {
                Log.e(TAG, "Failed to initialize youtubedl-android", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during native binary initialization", e)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .dispatcher(Dispatchers.IO)
            .allowHardware(true)
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
}
