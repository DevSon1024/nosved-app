package com.devson.nosved.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class YtDlpUpdateInterval(val days: Int, val displayName: String) {
    NEVER(0, "Never"),
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly")
}

class YtDlpUpdater(private val context: Application) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ytdlp_updater", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "YtDlpUpdater"
        private const val PREF_LAST_UPDATE = "last_update_timestamp"
        private const val PREF_UPDATE_INTERVAL = "update_interval"
        private const val PREF_CURRENT_VERSION = "current_version"
    }

    suspend fun checkAndUpdate(force: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
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
                        // Initialize YoutubeDL if not already done
                        try {
                            YoutubeDL.getInstance().init(context)
                            Log.d(TAG, "YoutubeDL initialized")
                        } catch (e: Exception) {
                            // Already initialized
                            Log.d(TAG, "YoutubeDL already initialized")
                        }

                        // Update YT-DLP
                        Log.d(TAG, "Updating YT-DLP...")
                        val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(context)
                        Log.d(TAG, "Update status: $updateStatus")

                        // Get updated version info
                        val newVersion = try {
                            val ver = YoutubeDL.getInstance().version(context)
                            Log.d(TAG, "New version: $ver")
                            ver ?: "Updated"
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting version", e)
                            "Updated"
                        }

                        // Save update info
                        prefs.edit()
                            .putLong(PREF_LAST_UPDATE, currentTime)
                            .putString(PREF_CURRENT_VERSION, newVersion)
                            .apply()

                        Log.d(TAG, "YT-DLP updated successfully to version: $newVersion")

                    } catch (e: YoutubeDLException) {
                        Log.e(TAG, "Failed to update YT-DLP", e)

                        // Still save the timestamp even if update fails
                        // (might already be up to date)
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
                // Don't throw - allow app to continue
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
            // Try to get version directly
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