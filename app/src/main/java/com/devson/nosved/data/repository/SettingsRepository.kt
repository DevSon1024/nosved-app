package com.devson.nosved.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.devson.nosved.util.YtDlpUpdateInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Flows for theme configuration
    private val _isDarkThemeFlow = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
    )
    val isDarkThemeFlow: StateFlow<Boolean?> = _isDarkThemeFlow.asStateFlow()

    private val _isAmoledThemeFlow = MutableStateFlow(prefs.getBoolean("is_amoled_theme", false))
    val isAmoledThemeFlow: StateFlow<Boolean> = _isAmoledThemeFlow.asStateFlow()

    private val _dynamicColorFlow = MutableStateFlow(prefs.getBoolean("dynamic_color", false))
    val dynamicColorFlow: StateFlow<Boolean> = _dynamicColorFlow.asStateFlow()

    private val _selectedPaletteFlow = MutableStateFlow(prefs.getString("selected_palette", "BLUE") ?: "BLUE")
    val selectedPaletteFlow: StateFlow<String> = _selectedPaletteFlow.asStateFlow()

    private val _isNavBarTransparentFlow = MutableStateFlow(prefs.getBoolean("is_navbar_transparent", true))
    val isNavBarTransparentFlow: StateFlow<Boolean> = _isNavBarTransparentFlow.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "is_dark_theme" -> {
                _isDarkThemeFlow.value = if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
            }
            "is_amoled_theme" -> {
                _isAmoledThemeFlow.value = prefs.getBoolean("is_amoled_theme", false)
            }
            "dynamic_color" -> {
                _dynamicColorFlow.value = prefs.getBoolean("dynamic_color", false)
            }
            "selected_palette" -> {
                _selectedPaletteFlow.value = prefs.getString("selected_palette", "BLUE") ?: "BLUE"
            }
            "is_navbar_transparent" -> {
                _isNavBarTransparentFlow.value = prefs.getBoolean("is_navbar_transparent", true)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun close() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

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
        prefs.edit().putString(PREF_UPDATE_INTERVAL, interval.name).apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun resetDarkTheme() {
        prefs.edit().remove("is_dark_theme").apply()
    }

    fun setAmoledTheme(enabled: Boolean) {
        prefs.edit().putBoolean("is_amoled_theme", enabled).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun setSelectedPalette(paletteName: String) {
        prefs.edit().putString("selected_palette", paletteName).apply()
    }

    fun setNavBarTransparent(transparent: Boolean) {
        prefs.edit().putBoolean("is_navbar_transparent", transparent).apply()
    }
}