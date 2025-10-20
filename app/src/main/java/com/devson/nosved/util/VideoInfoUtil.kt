package com.devson.nosved.util

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

object VideoInfoUtil {

    private const val TAG = "VideoInfoUtil"
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    // Cache for recently fetched video info to avoid repeated calls
    private val infoCache = mutableMapOf<String, VideoInfo>()
    private val activeJobs = mutableMapOf<String, Job>()

    suspend fun fetchVideoInfoFast(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        // Check cache first
        infoCache[url]?.let { cachedInfo ->
            Log.d(TAG, "Returning cached info for: $url")
            return@withContext Result.success(cachedInfo)
        }

        // Cancel any existing job for the same URL
        activeJobs[url]?.cancel()

        val job = async {
            runCatching {
                // Much shorter timeout for faster failure
                withTimeout(10000) { // 10 seconds only
                    fetchInfoWithMinimalOptions(url)
                }
            }
        }

        activeJobs[url] = job

        try {
            val result = job.await()
            activeJobs.remove(url)

            result.onSuccess { info ->
                // Cache successful results
                infoCache[url] = info
                // Clean cache if it gets too large
                if (infoCache.size > 50) {
                    infoCache.clear()
                }
            }

            result
        } catch (e: Exception) {
            activeJobs.remove(url)
            Result.failure(e)
        }
    }

    private suspend fun fetchInfoWithMinimalOptions(url: String): VideoInfo {
        // Try the fastest approach first - minimal options
        val fastRequest = YoutubeDLRequest(url).apply {
            // Absolute minimum options for maximum speed
            addOption("--dump-single-json")
            addOption("--no-playlist")
            addOption("--skip-download")
            addOption("--socket-timeout", "3") // Very short timeout
            addOption("-q") // Quiet mode for less processing
            addOption("--no-warnings")
            addOption("--no-check-certificate")
        }

        return try {
            Log.d(TAG, "Attempting fast fetch for: $url")
            val response = YoutubeDL.getInstance().execute(fastRequest, null, null)

            // Parse JSON response manually if possible
            if (response.out.contains("\"title\"")) {
                try {
                    jsonFormat.decodeFromString<VideoInfo>(response.out)
                } catch (e: Exception) {
                    Log.w(TAG, "JSON parsing failed, trying getInfo")
                    YoutubeDL.getInstance().getInfo(url)
                }
            } else {
                YoutubeDL.getInstance().getInfo(url)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fast fetch failed, trying basic getInfo: ${e.message}")
            // Last resort - basic getInfo but with timeout
            withTimeout(8000) {
                YoutubeDL.getInstance().getInfo(url)
            }
        }
    }

    fun cancelFetch(url: String) {
        activeJobs[url]?.cancel()
        activeJobs.remove(url)
    }

    fun clearCache() {
        infoCache.clear()
    }
}