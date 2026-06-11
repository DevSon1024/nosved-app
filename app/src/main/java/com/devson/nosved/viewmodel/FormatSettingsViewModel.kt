package com.devson.nosved.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.DownloadMode
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.util.YtDlpUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FormatSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val qualityPrefs = QualityPreferences(application.applicationContext)

    val videoQuality: StateFlow<String> = qualityPrefs.videoQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "720p")

    val audioQuality: StateFlow<String> = qualityPrefs.audioQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "128kbps")

    val downloadMode: StateFlow<DownloadMode> = qualityPrefs.downloadMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadMode.VIDEO_AUDIO)

    val videoContainer: StateFlow<String> = qualityPrefs.videoContainer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "MP4")

    val audioContainer: StateFlow<String> = qualityPrefs.audioContainer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "M4A")

    val embedMetadata: StateFlow<Boolean> = qualityPrefs.embedMetadata
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val convertToMp3: StateFlow<Boolean> = qualityPrefs.convertToMp3
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val convertAudioFormatEnabled: StateFlow<Boolean> = qualityPrefs.convertAudioFormatEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val convertAudioFormat: StateFlow<String> = qualityPrefs.convertAudioFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "mp3")

    val cropArtwork: StateFlow<Boolean> = qualityPrefs.cropArtwork
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadSubtitles: StateFlow<Boolean> = qualityPrefs.downloadSubtitles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val subtitleFormat: StateFlow<String> = qualityPrefs.subtitleFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "undefined")

    val convertSubtitles: StateFlow<Boolean> = qualityPrefs.convertSubtitles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadAutoCaptions: StateFlow<Boolean> = qualityPrefs.downloadAutoCaptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val customSubtitleLanguages: StateFlow<String> = qualityPrefs.customSubtitleLanguages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en,es,fr")

    val autoTranslatedSubtitles: StateFlow<Boolean> = qualityPrefs.autoTranslatedSubtitles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val embedSubtitles: StateFlow<Boolean> = qualityPrefs.embedSubtitles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val keepSubtitleFiles: StateFlow<Boolean> = qualityPrefs.keepSubtitleFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val preferredVideoFormat: StateFlow<String> = qualityPrefs.preferredVideoFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "quality")

    val remuxVideoContainer: StateFlow<Boolean> = qualityPrefs.remuxVideoContainer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val formatSorting: StateFlow<Boolean> = qualityPrefs.formatSorting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sortingFields: StateFlow<String> = qualityPrefs.sortingFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val formatSelection: StateFlow<Boolean> = qualityPrefs.formatSelection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val clipVideo: StateFlow<Boolean> = qualityPrefs.clipVideo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val mergeMultipleAudio: StateFlow<Boolean> = qualityPrefs.mergeMultipleAudio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setVideoQuality(quality: String) {
        viewModelScope.launch { qualityPrefs.setVideoQuality(quality) }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch { qualityPrefs.setAudioQuality(quality) }
    }

    fun setDownloadMode(mode: DownloadMode) {
        viewModelScope.launch { qualityPrefs.setDownloadMode(mode) }
    }

    fun setVideoContainer(container: String) {
        viewModelScope.launch { qualityPrefs.setVideoContainer(container) }
    }

    fun setAudioContainer(container: String) {
        viewModelScope.launch { qualityPrefs.setAudioContainer(container) }
    }

    fun setEmbedMetadata(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setEmbedMetadata(enabled) }
    }

    fun setConvertToMp3(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setConvertToMp3(enabled) }
    }

    fun setConvertAudioFormatEnabled(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setConvertAudioFormatEnabled(enabled) }
    }

    fun setConvertAudioFormat(format: String) {
        viewModelScope.launch { qualityPrefs.setConvertAudioFormat(format) }
    }

    fun setCropArtwork(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setCropArtwork(enabled) }
    }

    fun setDownloadSubtitles(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setDownloadSubtitles(enabled) }
    }

    fun setSubtitleFormat(format: String) {
        viewModelScope.launch { qualityPrefs.setSubtitleFormat(format) }
    }

    fun setConvertSubtitles(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setConvertSubtitles(enabled) }
    }

    fun setDownloadAutoCaptions(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setDownloadAutoCaptions(enabled) }
    }

    fun setCustomSubtitleLanguages(languages: String) {
        viewModelScope.launch { qualityPrefs.setCustomSubtitleLanguages(languages) }
    }

    fun setAutoTranslatedSubtitles(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setAutoTranslatedSubtitles(enabled) }
    }

    fun setEmbedSubtitles(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setEmbedSubtitles(enabled) }
    }

    fun setKeepSubtitleFiles(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setKeepSubtitleFiles(enabled) }
    }

    fun setPreferredVideoFormat(format: String) {
        viewModelScope.launch { qualityPrefs.setPreferredVideoFormat(format) }
    }

    fun setRemuxVideoContainer(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setRemuxVideoContainer(enabled) }
    }

    fun setFormatSorting(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setFormatSorting(enabled) }
    }

    fun setSortingFields(fields: String) {
        viewModelScope.launch { qualityPrefs.setSortingFields(fields) }
    }

    fun setFormatSelection(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setFormatSelection(enabled) }
    }

    fun setClipVideo(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setClipVideo(enabled) }
    }

    fun setMergeMultipleAudio(enabled: Boolean) {
        viewModelScope.launch { qualityPrefs.setMergeMultipleAudio(enabled) }
    }

    fun getYtDlpVersion(): String {
        return try {
            YtDlpUpdater(getApplication()).getCurrentVersion()
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
