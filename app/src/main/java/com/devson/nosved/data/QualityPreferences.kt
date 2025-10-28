package com.devson.nosved.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quality_settings")

class QualityPreferences(private val context: Context) {

    companion object {
        val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        val DOWNLOAD_MODE_KEY = stringPreferencesKey("download_mode")
        val VIDEO_CONTAINER_KEY = stringPreferencesKey("video_container")
        val AUDIO_CONTAINER_KEY = stringPreferencesKey("audio_container")
    }

    val videoQuality: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[VIDEO_QUALITY_KEY] ?: "720p" }

    val audioQuality: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AUDIO_QUALITY_KEY] ?: "128kbps" }

    val downloadMode: Flow<DownloadMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[DOWNLOAD_MODE_KEY]) {
                "VIDEO_AUDIO" -> DownloadMode.VIDEO_AUDIO
                "AUDIO_ONLY" -> DownloadMode.AUDIO_ONLY
                else -> DownloadMode.VIDEO_AUDIO
            }
        }

    val videoContainer: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[VIDEO_CONTAINER_KEY] ?: "MP4" }

    val audioContainer: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AUDIO_CONTAINER_KEY] ?: "M4A" }

    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY_KEY] = quality
        }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY_KEY] = quality
        }
    }

    suspend fun setDownloadMode(mode: DownloadMode) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_MODE_KEY] = mode.name
        }
    }

    suspend fun setVideoContainer(container: String) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_CONTAINER_KEY] = container
        }
    }

    suspend fun setAudioContainer(container: String) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_CONTAINER_KEY] = container
        }
    }
}

enum class DownloadMode {
    VIDEO_AUDIO,
    AUDIO_ONLY
}

data class QualityOption(
    val label: String,
    val value: String,
    val isDefault: Boolean = false
)

object QualityConstants {
    val VIDEO_MP4_QUALITIES = listOf(
        QualityOption("480p", "480p"),
        QualityOption("720p (HD)", "720p", true),
        QualityOption("1080p (FHD)", "1080p"),
        QualityOption("1440p (2K)", "1440p"),
        QualityOption("2160p (4K)", "2160p")
    )

    val VIDEO_WEBM_QUALITIES = listOf(
        QualityOption("480p", "480p"),
        QualityOption("720p (HD)", "720p", true),
        QualityOption("1080p (FHD)", "1080p"),
        QualityOption("1440p (2K)", "1440p"),
        QualityOption("2160p (4K)", "2160p")
    )

    val AUDIO_M4A_QUALITIES = listOf(
        QualityOption("64kbps", "64kbps"),
        QualityOption("128kbps", "128kbps", true),
        QualityOption("192kbps", "192kbps"),
        QualityOption("256kbps", "256kbps")
    )

    val AUDIO_WEBM_OPUS_QUALITIES = listOf(
        QualityOption("64kbps", "64kbps"),
        QualityOption("128kbps", "128kbps", true),
        QualityOption("160kbps", "160kbps"),
        QualityOption("192kbps", "192kbps")
    )
}