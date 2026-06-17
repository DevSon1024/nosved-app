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

    val videoDownloadFolder: StateFlow<String> = settingsRepo.videoDownloadFolderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "/storage/emulated/0/Download/Nosved")

    val audioDownloadFolder: StateFlow<String> = settingsRepo.audioDownloadFolderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "/storage/emulated/0/Download/Nosved/Audio")

    val saveToSubdirectoryWebsite: StateFlow<Boolean> = settingsRepo.saveToSubdirectoryWebsiteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val saveToSubdirectoryPlaylist: StateFlow<Boolean> = settingsRepo.saveToSubdirectoryPlaylistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val outputTemplate: StateFlow<String> = settingsRepo.outputTemplateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "%(title).200B.%(ext)s")

    val restrictFilenames: StateFlow<Boolean> = settingsRepo.restrictFilenamesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val limitSpeed: StateFlow<Boolean> = settingsRepo.limitSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val maxSpeedKb: StateFlow<String> = settingsRepo.maxSpeedKbFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "1024")

    val downloadUsingCellular: StateFlow<Boolean> = settingsRepo.downloadUsingCellularFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val useAria2c: StateFlow<Boolean> = settingsRepo.useAria2cFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val multiThreadedDownloadThreads: StateFlow<Int> = settingsRepo.multiThreadedDownloadThreadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 8)

    val forceIpv4: StateFlow<Boolean> = settingsRepo.forceIpv4Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val useProxy: StateFlow<Boolean> = settingsRepo.useProxyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val proxyUrl: StateFlow<String> = settingsRepo.proxyUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

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

    fun setVideoDownloadFolder(folder: String) {
        viewModelScope.launch { settingsRepo.setVideoDownloadFolder(folder) }
    }

    fun setAudioDownloadFolder(folder: String) {
        viewModelScope.launch { settingsRepo.setAudioDownloadFolder(folder) }
    }

    fun setSaveToSubdirectoryWebsite(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSaveToSubdirectoryWebsite(enabled) }
    }

    fun setSaveToSubdirectoryPlaylist(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSaveToSubdirectoryPlaylist(enabled) }
    }

    fun setOutputTemplate(template: String) {
        viewModelScope.launch { settingsRepo.setOutputTemplate(template) }
    }

    fun setRestrictFilenames(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setRestrictFilenames(enabled) }
    }

    fun setLimitSpeed(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setLimitSpeed(enabled) }
    }

    fun setMaxSpeedKb(speed: String) {
        viewModelScope.launch { settingsRepo.setMaxSpeedKb(speed) }
    }

    fun setDownloadUsingCellular(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDownloadUsingCellular(enabled) }
    }

    fun setUseAria2c(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setUseAria2c(enabled) }
    }

    fun setMultiThreadedDownloadThreads(threads: Int) {
        viewModelScope.launch { settingsRepo.setMultiThreadedDownloadThreads(threads) }
    }

    fun setForceIpv4(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setForceIpv4(enabled) }
    }

    fun setUseProxy(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setUseProxy(enabled) }
    }

    fun setProxyUrl(url: String) {
        viewModelScope.launch { settingsRepo.setProxyUrl(url) }
    }

    override fun onCleared() {
        super.onCleared()
        settingsRepo.close()
    }
}
