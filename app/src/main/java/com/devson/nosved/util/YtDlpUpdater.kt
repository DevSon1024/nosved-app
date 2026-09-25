package com.devson.nosved.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

enum class YtDlpUpdateInterval(val days: Int, val displayName: String) {
    NEVER(0, "Never"),
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly")
}

sealed class ComponentDownloadState {
    object Idle : ComponentDownloadState()
    data class Downloading(val component: String, val progress: Int, val message: String) : ComponentDownloadState()
    object Completed : ComponentDownloadState()
    data class Error(val message: String) : ComponentDownloadState()
}

class YtDlpUpdater(private val context: Application) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _componentDownloadState = MutableStateFlow<ComponentDownloadState>(ComponentDownloadState.Idle)
    val componentDownloadState: StateFlow<ComponentDownloadState> = _componentDownloadState.asStateFlow()

    companion object {
        private const val TAG = "YtDlpUpdater"
        private const val PREF_LAST_UPDATE = "last_update_timestamp"
        private const val PREF_UPDATE_INTERVAL = "ytdlp_update_interval"
        private const val PREF_CURRENT_VERSION = "current_version"

        private const val FFMPEG_BINARY_URL = "https://github.com/junkfood02/youtubedl-android/releases/download/0.17.3/ffmpeg-arm64-v8a.zip"
        private const val PYTHON_BINARY_URL = "https://github.com/junkfood02/youtubedl-android/releases/download/0.17.3/python-arm64-v8a.zip"
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun areComponentsExtracted(): Boolean {
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists()) return false
        val ffmpegFile = File(binDir, "ffmpeg")
        val pythonFile = File(binDir, "python")
        val ffmpegSo = File(binDir, "libffmpeg.so")
        val pythonSo = File(binDir, "libpython.so")
        val hasFfmpeg = (ffmpegFile.exists() && ffmpegFile.length() > 0) || (ffmpegSo.exists() && ffmpegSo.length() > 0)
        val hasPython = (pythonFile.exists() && pythonFile.length() > 0) || (pythonSo.exists() && pythonSo.length() > 0)
        return hasFfmpeg && hasPython
    }

    suspend fun ensureComponentsExtracted(
        force: Boolean = false,
        onProgress: ((component: String, progress: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!force && areComponentsExtracted()) {
            val binDir = File(context.filesDir, "bin")
            setDirectoryExecutables(binDir)
            _componentDownloadState.value = ComponentDownloadState.Completed
            return@withContext true
        }

        if (!isNetworkAvailable()) {
            val msg = "No network connection available to download core components."
            Log.e(TAG, msg)
            _componentDownloadState.value = ComponentDownloadState.Error(msg)
            return@withContext false
        }

        try {
            val binDir = File(context.filesDir, "bin")
            if (!binDir.exists()) {
                binDir.mkdirs()
            }

            Log.d(TAG, "Downloading core FFmpeg component...")
            downloadAndExtractComponent(
                componentName = "FFmpeg",
                downloadUrl = FFMPEG_BINARY_URL,
                outputDir = binDir,
                onProgress = onProgress
            )

            Log.d(TAG, "Downloading core Python component...")
            downloadAndExtractComponent(
                componentName = "Python",
                downloadUrl = PYTHON_BINARY_URL,
                outputDir = binDir,
                onProgress = onProgress
            )

            setDirectoryExecutables(binDir)
            _componentDownloadState.value = ComponentDownloadState.Completed
            Log.d(TAG, "Core components extracted and prepared successfully.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and extract core components", e)
            _componentDownloadState.value = ComponentDownloadState.Error(e.message ?: "Failed to download components")
            return@withContext false
        }
    }

    private suspend fun downloadAndExtractComponent(
        componentName: String,
        downloadUrl: String,
        outputDir: File,
        onProgress: ((component: String, progress: Int) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val tempZip = File(context.cacheDir, "${componentName.lowercase()}_temp.zip")
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempZip)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = if (fileLength > 0) ((totalBytesRead * 100) / fileLength).toInt() else 0
                val msg = "Downloading $componentName ($progress%)..."
                _componentDownloadState.value = ComponentDownloadState.Downloading(componentName, progress, msg)
                onProgress?.invoke(componentName, progress)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            ZipInputStream(tempZip.inputStream()).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(outputDir, entry.name.substringAfterLast('/'))
                        FileOutputStream(outFile).use { out ->
                            zipInput.copyTo(out)
                        }
                        outFile.setExecutable(true, false)
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        } finally {
            if (tempZip.exists()) {
                tempZip.delete()
            }
        }
    }

    private fun setDirectoryExecutables(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.setExecutable(true, false)
            }
        }
    }

    suspend fun checkAndUpdate(force: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                ensureComponentsExtracted(force = force)

                val currentTime = System.currentTimeMillis()
                val lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0)
                val interval = getUpdateInterval()

                val shouldUpdate = force || when (interval) {
                    YtDlpUpdateInterval.NEVER -> false
                    else -> {
                        val daysSinceUpdate = (currentTime - lastUpdate) / (1000 * 60 * 60 * 24)
                        daysSinceUpdate >= interval.days
                    }
                }

                if (shouldUpdate) {
                    Log.d(TAG, "Starting YT-DLP update check...")

                    try {
                        try {
                            YoutubeDL.getInstance().init(context)
                            Log.d(TAG, "YoutubeDL initialized")
                        } catch (e: Exception) {
                            Log.d(TAG, "YoutubeDL already initialized")
                        }

                        val channelStr = prefs.getString("ytdlp_update_channel", "STABLE") ?: "STABLE"
                        val updateChannel = if (channelStr == "NIGHTLY") {
                            YoutubeDL.UpdateChannel.NIGHTLY
                        } else {
                            YoutubeDL.UpdateChannel.STABLE
                        }

                        Log.d(TAG, "Updating YT-DLP on channel: $channelStr...")
                        val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(context, updateChannel)
                        Log.d(TAG, "Update status: $updateStatus")

                        val newVersion = try {
                            val ver = YoutubeDL.getInstance().version(context)
                            Log.d(TAG, "New version: $ver")
                            ver ?: "Updated"
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting version", e)
                            "Updated"
                        }

                        prefs.edit()
                            .putLong(PREF_LAST_UPDATE, currentTime)
                            .putString(PREF_CURRENT_VERSION, newVersion)
                            .apply()

                        Log.d(TAG, "YT-DLP updated successfully to version: $newVersion")

                    } catch (e: YoutubeDLException) {
                        Log.e(TAG, "Failed to update YT-DLP", e)
                        prefs.edit()
                            .putLong(PREF_LAST_UPDATE, currentTime)
                            .apply()
                        throw e
                    }
                } else {
                    Log.d(TAG, "YT-DLP update not needed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during YT-DLP update check", e)
            }
        }
    }

    fun getUpdateInterval(): YtDlpUpdateInterval {
        val intervalName = prefs.getString(PREF_UPDATE_INTERVAL, YtDlpUpdateInterval.WEEKLY.name)
        return try {
            YtDlpUpdateInterval.valueOf(intervalName ?: YtDlpUpdateInterval.WEEKLY.name)
        } catch (e: IllegalArgumentException) {
            YtDlpUpdateInterval.WEEKLY
        }
    }

    fun setUpdateInterval(interval: YtDlpUpdateInterval) {
        prefs.edit()
            .putString(PREF_UPDATE_INTERVAL, interval.name)
            .apply()
    }

    fun getLastUpdateTime(): String {
        val lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0)
        return if (lastUpdate == 0L) {
            "Never"
        } else {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            dateFormat.format(Date(lastUpdate))
        }
    }

    fun getCurrentVersion(): String {
        val version = prefs.getString(PREF_CURRENT_VERSION, null)
        return if (version.isNullOrEmpty()) {
            try {
                YoutubeDL.getInstance().version(context) ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            version
        }
    }

    suspend fun forceUpdate() {
        checkAndUpdate(force = true)
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}