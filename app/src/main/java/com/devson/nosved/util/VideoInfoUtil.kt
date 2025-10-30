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

    // Enhanced caching with expiry
    private val infoCache = mutableMapOf<String, CachedVideoInfo>()
    private val activeJobs = mutableMapOf<String, Job>()
    private val platformOptimizations = initializePlatformSettings()

    data class CachedVideoInfo(
        val videoInfo: VideoInfo,
        val timestamp: Long,
        val expiryMs: Long = 300000 // 5 minutes
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > expiryMs
    }

    data class ProgressiveResult(
        val basicInfo: VideoInfo?,
        val isComplete: Boolean,
        val stage: String
    )

    private fun initializePlatformSettings(): Map<String, PlatformConfig> {
        return mapOf(
            "youtube.com" to PlatformConfig(
                timeout = 4000,
                extraOptions = listOf("--youtube-skip-dash-manifest", "--format", "best[height<=720]")
            ),
            "youtu.be" to PlatformConfig(
                timeout = 4000,
                extraOptions = listOf("--youtube-skip-dash-manifest", "--format", "best[height<=720]")
            ),
            "instagram.com" to PlatformConfig(
                timeout = 3000,
                extraOptions = listOf("--no-check-certificate", "--format", "mp4")
            ),
            "tiktok.com" to PlatformConfig(
                timeout = 5000,
                extraOptions = listOf("--format", "best", "--no-playlist")
            )
        )
    }

    data class PlatformConfig(
        val timeout: Long,
        val extraOptions: List<String>
    )

    suspend fun fetchVideoInfoProgressive(
        url: String,
        onProgress: suspend (ProgressiveResult) -> Unit
    ): Result<VideoInfo> = withContext(Dispatchers.IO) {

        // Stage 1: Check cache
        val cachedInfo = infoCache[url]
        if (cachedInfo != null && !cachedInfo.isExpired) {
            Log.d(TAG, "Returning cached info for: $url")
            onProgress(ProgressiveResult(cachedInfo.videoInfo, true, "Cache hit"))
            return@withContext Result.success(cachedInfo.videoInfo)
        }

        // Cancel any existing job
        activeJobs[url]?.cancel()

        val job = async {
            try {
                // Stage 2: Quick validation (500ms)
                onProgress(ProgressiveResult(null, false, "Validating URL"))

                if (!isValidVideoUrl(url)) {
                    throw IllegalArgumentException("Invalid video URL")
                }

                // Stage 3: Platform-optimized extraction
                onProgress(ProgressiveResult(null, false, "Extracting info"))

                val platformConfig = detectPlatform(url)
                val info = extractWithOptimizedSettings(url, platformConfig)

                // Cache successful result
                infoCache[url] = CachedVideoInfo(info, System.currentTimeMillis())
                cleanExpiredCache()

                onProgress(ProgressiveResult(info, true, "Complete"))
                info

            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch video info for $url", e)
                throw e
            }
        }

        activeJobs[url] = job

        try {
            val result = job.await()
            activeJobs.remove(url)
            Result.success(result)
        } catch (e: Exception) {
            activeJobs.remove(url)
            Result.failure(e)
        }
    }

    private fun detectPlatform(url: String): PlatformConfig {
        return platformOptimizations.entries.find { (domain, _) ->
            url.contains(domain, ignoreCase = true)
        }?.value ?: PlatformConfig(
            timeout = 6000,
            extraOptions = listOf("--format", "best[height<=720]")
        )
    }

    private suspend fun extractWithOptimizedSettings(url: String, config: PlatformConfig): VideoInfo {
        return withTimeout(config.timeout) {
            // Try ultra-fast extraction first
            try {
                extractUltraFast(url, config)
            } catch (e: Exception) {
                Log.w(TAG, "Ultra-fast failed, trying standard method")
                extractStandard(url, config)
            }
        }
    }

    private suspend fun extractUltraFast(url: String, config: PlatformConfig): VideoInfo {
        val request = YoutubeDLRequest(url).apply {
            // Ultra-aggressive speed options
            addOption("--no-playlist")
            addOption("--skip-download")
            addOption("--socket-timeout", "2")
            addOption("--fragment-retries", "1")
            addOption("--retries", "1")
            addOption("--no-warnings")
            addOption("--quiet")
            addOption("--no-check-certificate")
            addOption("--no-cache-dir")
            addOption("--concurrent-fragments", "1")

            // Add platform-specific options
            config.extraOptions.chunked(2).forEach { pair ->
                if (pair.size == 2) {
                    addOption(pair[0], pair[1])
                } else {
                    addOption(pair[0])
                }
            }
        }

        // --- FIX ---
        // Was: return YoutubeDL.getInstance().getInfo(url)
        // Now:
        return YoutubeDL.getInstance().getInfo(request)
    }

    private suspend fun extractStandard(url: String, config: PlatformConfig): VideoInfo {
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--socket-timeout", "5")
            config.extraOptions.chunked(2).forEach { pair ->
                if (pair.size == 2) {
                    addOption(pair[0], pair[1])
                } else {
                    addOption(pair[0])
                }
            }
        }

        // --- FIX ---
        // Was: return YoutubeDL.getInstance().getInfo(url)
        // Now:
        return YoutubeDL.getInstance().getInfo(request)
    }

    private fun isValidVideoUrl(url: String): Boolean {
        val videoPatterns = listOf(
            "youtube.com", "youtu.be", "instagram.com", "tiktok.com",
            "twitter.com", "facebook.com", "vimeo.com", "dailymotion.com"
        )
        return videoPatterns.any { pattern ->
            url.contains(pattern, ignoreCase = true)
        } && (url.startsWith("http://") || url.startsWith("https://"))
    }

    private fun cleanExpiredCache() {
        if (infoCache.size > 50) {
            val iterator = infoCache.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isExpired) {
                    iterator.remove()
                }
            }
        }
    }

    // Legacy method for backward compatibility
    suspend fun fetchVideoInfoFast(url: String): Result<VideoInfo> {
        return fetchVideoInfoProgressive(url) { /* no progress callback */ }
    }

    fun cancelFetch(url: String) {
        activeJobs[url]?.cancel()
        activeJobs.remove(url)
    }

    fun clearCache() {
        infoCache.clear()
    }
}