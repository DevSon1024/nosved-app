package com.devson.nosved.data

import android.content.Context
import android.content.SharedPreferences

enum class YtDlpUpdateInterval(val value: String) {
    NEVER("never"),
    ON_STARTUP("on_startup"),
    DAILY("daily"),
    WEEKLY("weekly")
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nosved_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_UPDATE_INTERVAL = "update_interval"

        // 1 day in milliseconds
        const val DAY_IN_MS = 1000 * 60 * 60 * 24
        // 7 days in milliseconds
        const val WEEK_IN_MS = DAY_IN_MS * 7
    }

    fun getLastUpdateCheck(): Long {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
    }

    fun setLastUpdateCheck(time: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, time).apply()
    }

    fun getUpdateInterval(): YtDlpUpdateInterval {
        val intervalValue = prefs.getString(KEY_UPDATE_INTERVAL, YtDlpUpdateInterval.ON_STARTUP.value)
        return YtDlpUpdateInterval.values().find { it.value == intervalValue }
            ?: YtDlpUpdateInterval.ON_STARTUP
    }

    fun setUpdateInterval(interval: YtDlpUpdateInterval) {
        prefs.edit().putString(KEY_UPDATE_INTERVAL, interval.value).apply()
    }
}