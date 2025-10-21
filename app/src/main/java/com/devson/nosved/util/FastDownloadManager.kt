package com.devson.nosved.util

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.*

object FastDownloadManager {

    private const val TAG = "FastDownloadManager"

    fun createOptimizedDownloadRequest(
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat,
        outputPath: String
    ): YoutubeDLRequest? {

        return videoInfo.webpageUrl?.let { url ->
            YoutubeDLRequest(url).apply {
                // Output settings
                addOption("-o", outputPath.replace(".mp4", ".%(ext)s"))
                addOption("-f", "${videoFormat.formatId}+${audioFormat.formatId}")
                addOption("--merge-output-format", "mp4")

                // Speed optimizations
                addOption("--no-mtime")
                addOption("--no-playlist")
                addOption("--concurrent-fragments", "4")
                addOption("--fragment-retries", "2")
                addOption("--socket-timeout", "8")
                addOption("--retries", "2")
                addOption("--no-warnings")

                // Platform-specific optimizations
                when {
                    url.contains("youtube.com") || url.contains("youtu.be") -> {
                        addOption("--youtube-skip-dash-manifest")
                        addOption("--no-check-certificate")
                    }
                    url.contains("instagram.com") -> {
                        addOption("--no-check-certificate")
                    }
                    url.contains("tiktok.com") -> {
                        addOption("--no-check-certificate")
                        addOption("--add-header", "User-Agent:Mozilla/5.0")
                    }
                }
            }
        }
    }

    suspend fun preValidateFormats(
        videoInfo: VideoInfo,
        videoFormat: VideoFormat,
        audioFormat: VideoFormat
    ): Boolean = withContext(Dispatchers.Default) {

        return@withContext try {
            // Quick validation without actual download
            val formats = videoInfo.formats ?: return@withContext false

            val hasVideo = formats.any { it.formatId == videoFormat.formatId }
            val hasAudio = formats.any { it.formatId == audioFormat.formatId }

            hasVideo && hasAudio
        } catch (e: Exception) {
            Log.e(TAG, "Format validation failed", e)
            false
        }
    }
}
