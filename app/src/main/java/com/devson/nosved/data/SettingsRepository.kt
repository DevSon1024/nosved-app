package com.devson.nosved.data

import android.content.Context
import android.content.SharedPreferences
import com.devson.nosved.util.YtDlpUpdateInterval

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_UPDATE_INTERVAL = "ytdlp_update_interval"
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
}