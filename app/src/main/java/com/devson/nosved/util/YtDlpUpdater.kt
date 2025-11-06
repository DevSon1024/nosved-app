package com.devson.nosved.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.devson.nosved.data.SettingsRepository
import com.devson.nosved.data.YtDlpUpdateInterval
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YtDlpUpdater(context: Context) {

    private val settings = SettingsRepository(context)
    private val appContext = context.applicationContext

    /**
     * Checks if an update is needed based on settings and performs it.
     * @param force Overrides settings and forces an update check.
     */
    suspend fun checkAndUpdate(force: Boolean = false) = withContext(Dispatchers.IO) {
        val interval = settings.getUpdateInterval()
        if (interval == YtDlpUpdateInterval.NEVER && !force) {
            Log.d("YtDlpUpdater", "Updates are disabled.")
            return@withContext
        }

        val lastCheck = settings.getLastUpdateCheck()
        val now = System.currentTimeMillis()

        val shouldCheck = force || when (interval) {
            YtDlpUpdateInterval.NEVER -> false
            YtDlpUpdateInterval.ON_STARTUP -> true // Always check on startup
            YtDlpUpdateInterval.DAILY -> (now - lastCheck) > SettingsRepository.DAY_IN_MS
            YtDlpUpdateInterval.WEEKLY -> (now - lastCheck) > SettingsRepository.WEEK_IN_MS
        }

        if (shouldCheck) {
            Log.d("YtDlpUpdater", "Checking for yt-dlp update...")
            try {
                // This call internally checks if an update is needed
                val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(appContext)

                // *** THIS IS THE FIX ***
                // We check against the UpdateStatus enum, not a Boolean
                if (updateStatus == YoutubeDL.UpdateStatus.DONE) {
                    Log.d("YtDlpUpdater", "yt-dlp updated successfully.")
                    showToast("yt-dlp updated")
                } else {
                    Log.d("YtDlpUpdater", "yt-dlp is already up to date.")
                    if (force) {
                        showToast("yt-dlp is already up to date")
                    }
                }
                settings.setLastUpdateCheck(now) // Save last check time regardless of result

            } catch (e: Exception) {
                Log.e("YtDlpUpdater", "yt-dlp update failed", e)
                if (force) {
                    showToast("yt-dlp update failed: ${e.message}")
                }
            }
        } else {
            Log.d("YtDlpUpdater", "Not time to check for update yet.")
        }
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}