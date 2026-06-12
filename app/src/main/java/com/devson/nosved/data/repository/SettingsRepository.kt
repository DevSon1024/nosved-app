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

    // YT-DLP update configurations
    private val _ytdlpUpdateChannelFlow = MutableStateFlow(prefs.getString("ytdlp_update_channel", "STABLE") ?: "STABLE")
    val ytdlpUpdateChannelFlow: StateFlow<String> = _ytdlpUpdateChannelFlow.asStateFlow()

    private val _ytdlpUpdateIntervalFlow = MutableStateFlow(
        try {
            YtDlpUpdateInterval.valueOf(prefs.getString("ytdlp_update_interval", YtDlpUpdateInterval.WEEKLY.name) ?: YtDlpUpdateInterval.WEEKLY.name)
        } catch (e: Exception) {
            YtDlpUpdateInterval.WEEKLY
        }
    )
    val ytdlpUpdateIntervalFlow: StateFlow<YtDlpUpdateInterval> = _ytdlpUpdateIntervalFlow.asStateFlow()

    // General settings configurations
    private val _downloadNotificationFlow = MutableStateFlow(prefs.getBoolean("download_notification_enabled", true))
    val downloadNotificationFlow: StateFlow<Boolean> = _downloadNotificationFlow.asStateFlow()

    private val _configureBeforeDownloadFlow = MutableStateFlow(prefs.getBoolean("configure_before_download", true))
    val configureBeforeDownloadFlow: StateFlow<Boolean> = _configureBeforeDownloadFlow.asStateFlow()

    private val _saveThumbnailFlow = MutableStateFlow(prefs.getBoolean("save_thumbnail", true))
    val saveThumbnailFlow: StateFlow<Boolean> = _saveThumbnailFlow.asStateFlow()

    private val _detailedOutputFlow = MutableStateFlow(prefs.getBoolean("detailed_output", false))
    val detailedOutputFlow: StateFlow<Boolean> = _detailedOutputFlow.asStateFlow()

    // Privacy settings configurations
    private val _incognitoModeFlow = MutableStateFlow(prefs.getBoolean("incognito_mode", false))
    val incognitoModeFlow: StateFlow<Boolean> = _incognitoModeFlow.asStateFlow()

    private val _disablePreviewFlow = MutableStateFlow(prefs.getBoolean("disable_preview", false))
    val disablePreviewFlow: StateFlow<Boolean> = _disablePreviewFlow.asStateFlow()

    // Advanced settings configurations
    private val _downloadPlaylistFlow = MutableStateFlow(prefs.getBoolean("download_playlist", false))
    val downloadPlaylistFlow: StateFlow<Boolean> = _downloadPlaylistFlow.asStateFlow()

    private val _downloadArchiveFlow = MutableStateFlow(prefs.getBoolean("download_archive", false))
    val downloadArchiveFlow: StateFlow<Boolean> = _downloadArchiveFlow.asStateFlow()

    private val _enableSponsorsBlockFlow = MutableStateFlow(prefs.getBoolean("enable_sponsors_block", false))
    val enableSponsorsBlockFlow: StateFlow<Boolean> = _enableSponsorsBlockFlow.asStateFlow()

    // Directory settings configurations
    private val _videoDownloadFolderFlow = MutableStateFlow(
        prefs.getString("video_download_folder", "/storage/emulated/0/Download/Nosved") ?: "/storage/emulated/0/Download/Nosved"
    )
    val videoDownloadFolderFlow: StateFlow<String> = _videoDownloadFolderFlow.asStateFlow()

    private val _audioDownloadFolderFlow = MutableStateFlow(
        prefs.getString("audio_download_folder", "/storage/emulated/0/Download/Nosved/Audio") ?: "/storage/emulated/0/Download/Nosved/Audio"
    )
    val audioDownloadFolderFlow: StateFlow<String> = _audioDownloadFolderFlow.asStateFlow()

    private val _saveToSubdirectoryWebsiteFlow = MutableStateFlow(prefs.getBoolean("save_to_subdirectory_website", false))
    val saveToSubdirectoryWebsiteFlow: StateFlow<Boolean> = _saveToSubdirectoryWebsiteFlow.asStateFlow()

    private val _saveToSubdirectoryPlaylistFlow = MutableStateFlow(prefs.getBoolean("save_to_subdirectory_playlist", false))
    val saveToSubdirectoryPlaylistFlow: StateFlow<Boolean> = _saveToSubdirectoryPlaylistFlow.asStateFlow()

    private val _outputTemplateFlow = MutableStateFlow(
        prefs.getString("output_template", "%(title).200B.%(ext)s") ?: "%(title).200B.%(ext)s"
    )
    val outputTemplateFlow: StateFlow<String> = _outputTemplateFlow.asStateFlow()

    private val _restrictFilenamesFlow = MutableStateFlow(prefs.getBoolean("restrict_filenames", false))
    val restrictFilenamesFlow: StateFlow<Boolean> = _restrictFilenamesFlow.asStateFlow()

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
            "ytdlp_update_channel" -> {
                _ytdlpUpdateChannelFlow.value = prefs.getString("ytdlp_update_channel", "STABLE") ?: "STABLE"
            }
            "ytdlp_update_interval" -> {
                _ytdlpUpdateIntervalFlow.value = try {
                    YtDlpUpdateInterval.valueOf(prefs.getString("ytdlp_update_interval", YtDlpUpdateInterval.WEEKLY.name) ?: YtDlpUpdateInterval.WEEKLY.name)
                } catch (e: Exception) {
                    YtDlpUpdateInterval.WEEKLY
                }
            }
            "download_notification_enabled" -> {
                _downloadNotificationFlow.value = prefs.getBoolean("download_notification_enabled", true)
            }
            "configure_before_download" -> {
                _configureBeforeDownloadFlow.value = prefs.getBoolean("configure_before_download", true)
            }
            "save_thumbnail" -> {
                _saveThumbnailFlow.value = prefs.getBoolean("save_thumbnail", true)
            }
            "detailed_output" -> {
                _detailedOutputFlow.value = prefs.getBoolean("detailed_output", false)
            }
            "incognito_mode" -> {
                _incognitoModeFlow.value = prefs.getBoolean("incognito_mode", false)
            }
            "disable_preview" -> {
                _disablePreviewFlow.value = prefs.getBoolean("disable_preview", false)
            }
            "download_playlist" -> {
                _downloadPlaylistFlow.value = prefs.getBoolean("download_playlist", false)
            }
            "download_archive" -> {
                _downloadArchiveFlow.value = prefs.getBoolean("download_archive", false)
            }
            "enable_sponsors_block" -> {
                _enableSponsorsBlockFlow.value = prefs.getBoolean("enable_sponsors_block", false)
            }
            "video_download_folder" -> {
                _videoDownloadFolderFlow.value = prefs.getString("video_download_folder", "/storage/emulated/0/Download/Nosved") ?: "/storage/emulated/0/Download/Nosved"
            }
            "audio_download_folder" -> {
                _audioDownloadFolderFlow.value = prefs.getString("audio_download_folder", "/storage/emulated/0/Download/Nosved/Audio") ?: "/storage/emulated/0/Download/Nosved/Audio"
            }
            "save_to_subdirectory_website" -> {
                _saveToSubdirectoryWebsiteFlow.value = prefs.getBoolean("save_to_subdirectory_website", false)
            }
            "save_to_subdirectory_playlist" -> {
                _saveToSubdirectoryPlaylistFlow.value = prefs.getBoolean("save_to_subdirectory_playlist", false)
            }
            "output_template" -> {
                _outputTemplateFlow.value = prefs.getString("output_template", "%(title).200B.%(ext)s") ?: "%(title).200B.%(ext)s"
            }
            "restrict_filenames" -> {
                _restrictFilenamesFlow.value = prefs.getBoolean("restrict_filenames", false)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun close() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun getUpdateInterval(): YtDlpUpdateInterval {
        val intervalName = prefs.getString("ytdlp_update_interval", YtDlpUpdateInterval.WEEKLY.name)
        return try {
            YtDlpUpdateInterval.valueOf(intervalName ?: YtDlpUpdateInterval.WEEKLY.name)
        } catch (e: IllegalArgumentException) {
            YtDlpUpdateInterval.WEEKLY
        }
    }

    fun setUpdateInterval(interval: YtDlpUpdateInterval) {
        prefs.edit().putString("ytdlp_update_interval", interval.name).apply()
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

    fun setYtdlpUpdateChannel(channel: String) {
        prefs.edit().putString("ytdlp_update_channel", channel).apply()
    }

    fun setDownloadNotification(enabled: Boolean) {
        prefs.edit().putBoolean("download_notification_enabled", enabled).apply()
    }

    fun setConfigureBeforeDownload(enabled: Boolean) {
        prefs.edit().putBoolean("configure_before_download", enabled).apply()
    }

    fun setSaveThumbnail(enabled: Boolean) {
        prefs.edit().putBoolean("save_thumbnail", enabled).apply()
    }

    fun setDetailedOutput(enabled: Boolean) {
        prefs.edit().putBoolean("detailed_output", enabled).apply()
    }

    fun setIncognitoMode(enabled: Boolean) {
        prefs.edit().putBoolean("incognito_mode", enabled).apply()
    }

    fun setDisablePreview(enabled: Boolean) {
        prefs.edit().putBoolean("disable_preview", enabled).apply()
    }

    fun setDownloadPlaylist(enabled: Boolean) {
        prefs.edit().putBoolean("download_playlist", enabled).apply()
    }

    fun setDownloadArchive(enabled: Boolean) {
        prefs.edit().putBoolean("download_archive", enabled).apply()
    }

    fun setEnableSponsorsBlock(enabled: Boolean) {
        prefs.edit().putBoolean("enable_sponsors_block", enabled).apply()
    }

    fun setVideoDownloadFolder(folder: String) {
        prefs.edit().putString("video_download_folder", folder).apply()
    }

    fun setAudioDownloadFolder(folder: String) {
        prefs.edit().putString("audio_download_folder", folder).apply()
    }

    fun setSaveToSubdirectoryWebsite(enabled: Boolean) {
        prefs.edit().putBoolean("save_to_subdirectory_website", enabled).apply()
    }

    fun setSaveToSubdirectoryPlaylist(enabled: Boolean) {
        prefs.edit().putBoolean("save_to_subdirectory_playlist", enabled).apply()
    }

    fun setOutputTemplate(template: String) {
        prefs.edit().putString("output_template", template).apply()
    }

    fun setRestrictFilenames(enabled: Boolean) {
        prefs.edit().putBoolean("restrict_filenames", enabled).apply()
    }
}