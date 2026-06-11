package com.devson.nosved.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.repository.SettingsRepository
import com.devson.nosved.ui.theme.AppThemePalette
import com.devson.nosved.ui.theme.AppThemePaletteHelper
import com.devson.nosved.util.YtDlpUpdateInterval
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application.applicationContext)

    val isDarkTheme: StateFlow<Boolean?> = settingsRepo.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isAmoledTheme: StateFlow<Boolean> = settingsRepo.isAmoledThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val dynamicColor: StateFlow<Boolean> = settingsRepo.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val selectedPalette: StateFlow<AppThemePalette> = settingsRepo.selectedPaletteFlow
        .map { key -> AppThemePaletteHelper.fromKey(key) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemePalette.BLUE)

    val isNavBarTransparent: StateFlow<Boolean> = settingsRepo.isNavBarTransparentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val ytdlpUpdateChannel: StateFlow<String> = settingsRepo.ytdlpUpdateChannelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "STABLE")

    val ytdlpUpdateInterval: StateFlow<YtDlpUpdateInterval> = settingsRepo.ytdlpUpdateIntervalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YtDlpUpdateInterval.WEEKLY)

    val downloadNotification: StateFlow<Boolean> = settingsRepo.downloadNotificationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val configureBeforeDownload: StateFlow<Boolean> = settingsRepo.configureBeforeDownloadFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val saveThumbnail: StateFlow<Boolean> = settingsRepo.saveThumbnailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val detailedOutput: StateFlow<Boolean> = settingsRepo.detailedOutputFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val incognitoMode: StateFlow<Boolean> = settingsRepo.incognitoModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val disablePreview: StateFlow<Boolean> = settingsRepo.disablePreviewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadPlaylist: StateFlow<Boolean> = settingsRepo.downloadPlaylistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val downloadArchive: StateFlow<Boolean> = settingsRepo.downloadArchiveFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val enableSponsorsBlock: StateFlow<Boolean> = settingsRepo.enableSponsorsBlockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { settingsRepo.setDarkTheme(isDark) }
    }

    fun resetDarkTheme() {
        viewModelScope.launch { settingsRepo.resetDarkTheme() }
    }

    fun setAmoledTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAmoledTheme(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }
    }

    fun setSelectedPalette(palette: AppThemePalette) {
        viewModelScope.launch { settingsRepo.setSelectedPalette(palette.name) }
    }

    fun setNavBarTransparent(transparent: Boolean) {
        viewModelScope.launch { settingsRepo.setNavBarTransparent(transparent) }
    }

    fun setYtdlpUpdateChannel(channel: String) {
        viewModelScope.launch { settingsRepo.setYtdlpUpdateChannel(channel) }
    }

    fun setYtdlpUpdateInterval(interval: YtDlpUpdateInterval) {
        viewModelScope.launch { settingsRepo.setUpdateInterval(interval) }
    }

    fun setDownloadNotification(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDownloadNotification(enabled) }
    }

    fun setConfigureBeforeDownload(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setConfigureBeforeDownload(enabled) }
    }

    fun setSaveThumbnail(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSaveThumbnail(enabled) }
    }

    fun setDetailedOutput(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDetailedOutput(enabled) }
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setIncognitoMode(enabled) }
    }

    fun setDisablePreview(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDisablePreview(enabled) }
    }

    fun setDownloadPlaylist(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDownloadPlaylist(enabled) }
    }

    fun setDownloadArchive(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDownloadArchive(enabled) }
    }

    fun setEnableSponsorsBlock(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setEnableSponsorsBlock(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        settingsRepo.close()
    }
}
