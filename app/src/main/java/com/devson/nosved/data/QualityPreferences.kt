package com.devson.nosved.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quality_settings")

class QualityPreferences(private val context: Context) {

    companion object {
        // Basic Quality Settings
        val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        val DOWNLOAD_MODE_KEY = stringPreferencesKey("download_mode")
        val VIDEO_CONTAINER_KEY = stringPreferencesKey("video_container")
        val AUDIO_CONTAINER_KEY = stringPreferencesKey("audio_container")

        // Audio Enhancements
        val EMBED_METADATA_KEY = booleanPreferencesKey("embed_metadata")
        val CONVERT_TO_MP3_KEY = booleanPreferencesKey("convert_to_mp3")
        val CONVERT_AUDIO_FORMAT_ENABLED_KEY = booleanPreferencesKey("convert_audio_format_enabled")
        val CONVERT_AUDIO_FORMAT_KEY = stringPreferencesKey("convert_audio_format")
        val CROP_ARTWORK_KEY = booleanPreferencesKey("crop_artwork")

        // Subtitle Settings
        val DOWNLOAD_SUBTITLES_KEY = booleanPreferencesKey("download_subtitles")
        val SUBTITLE_FORMAT_KEY = stringPreferencesKey("subtitle_format")
        val CONVERT_SUBTITLES_KEY = booleanPreferencesKey("convert_subtitles")
        val DOWNLOAD_AUTO_CAPTIONS_KEY = booleanPreferencesKey("download_auto_captions")
        val CUSTOM_SUBTITLE_LANGUAGES_KEY = stringPreferencesKey("custom_subtitle_languages")
        val AUTO_TRANSLATED_SUBTITLES_KEY = booleanPreferencesKey("auto_translated_subtitles")
        val EMBED_SUBTITLES_KEY = booleanPreferencesKey("embed_subtitles")
        val KEEP_SUBTITLE_FILES_KEY = booleanPreferencesKey("keep_subtitle_files")

        // Advanced Features
        val ENABLE_SPONSORS_BLOCK_KEY = booleanPreferencesKey("enable_sponsors_block")
        val EXTRACT_AUDIO_KEY = booleanPreferencesKey("extract_audio")
        val KEEP_VIDEO_AFTER_AUDIO_EXTRACTION_KEY = booleanPreferencesKey("keep_video_after_audio_extraction")
        val ENABLE_COOKIES_KEY = booleanPreferencesKey("enable_cookies")
        val MAX_DOWNLOAD_RETRIES_KEY = intPreferencesKey("max_download_retries")
        val PREFERRED_LANGUAGE_KEY = stringPreferencesKey("preferred_language")
        val PREFERRED_VIDEO_FORMAT_KEY = stringPreferencesKey("preferred_video_format")
        val REMUX_VIDEO_CONTAINER_KEY = booleanPreferencesKey("remux_video_container")
        val FORMAT_SORTING_KEY = booleanPreferencesKey("format_sorting")
        val SORTING_FIELDS_KEY = stringPreferencesKey("sorting_fields")
        val FORMAT_SELECTION_KEY = booleanPreferencesKey("format_selection")
        val CLIP_VIDEO_KEY = booleanPreferencesKey("clip_video")
        val MERGE_MULTIPLE_AUDIO_KEY = booleanPreferencesKey("merge_multiple_audio")
    }

    // Basic Quality Flows
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

    // Audio Enhancement Flows
    val embedMetadata: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[EMBED_METADATA_KEY] ?: true }

    val convertToMp3: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CONVERT_TO_MP3_KEY] ?: false }

    val convertAudioFormatEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CONVERT_AUDIO_FORMAT_ENABLED_KEY] ?: false }

    val convertAudioFormat: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CONVERT_AUDIO_FORMAT_KEY] ?: "mp3" }

    val cropArtwork: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CROP_ARTWORK_KEY] ?: false }

    // Subtitle Flows
    val downloadSubtitles: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DOWNLOAD_SUBTITLES_KEY] ?: false }

    val subtitleFormat: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SUBTITLE_FORMAT_KEY] ?: "undefined" }

    val convertSubtitles: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CONVERT_SUBTITLES_KEY] ?: false }

    val downloadAutoCaptions: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DOWNLOAD_AUTO_CAPTIONS_KEY] ?: false }

    val customSubtitleLanguages: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CUSTOM_SUBTITLE_LANGUAGES_KEY] ?: "en,es,fr" }

    val autoTranslatedSubtitles: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_TRANSLATED_SUBTITLES_KEY] ?: false }

    val embedSubtitles: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[EMBED_SUBTITLES_KEY] ?: true }

    val keepSubtitleFiles: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEEP_SUBTITLE_FILES_KEY] ?: false }

    // Advanced Feature Flows
    val enableSponsorsBlock: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ENABLE_SPONSORS_BLOCK_KEY] ?: false }

    val extractAudio: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[EXTRACT_AUDIO_KEY] ?: false }

    val keepVideoAfterAudioExtraction: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEEP_VIDEO_AFTER_AUDIO_EXTRACTION_KEY] ?: false }

    val enableCookies: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ENABLE_COOKIES_KEY] ?: false }

    val maxDownloadRetries: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MAX_DOWNLOAD_RETRIES_KEY] ?: 3 }

    val preferredLanguage: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PREFERRED_LANGUAGE_KEY] ?: "en" }

    val preferredVideoFormat: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PREFERRED_VIDEO_FORMAT_KEY] ?: "quality" }

    val remuxVideoContainer: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[REMUX_VIDEO_CONTAINER_KEY] ?: false }

    val formatSorting: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[FORMAT_SORTING_KEY] ?: false }

    val sortingFields: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SORTING_FIELDS_KEY] ?: "" }

    val formatSelection: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[FORMAT_SELECTION_KEY] ?: true }

    val clipVideo: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CLIP_VIDEO_KEY] ?: false }

    val mergeMultipleAudio: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[MERGE_MULTIPLE_AUDIO_KEY] ?: false }

    // Basic Setters
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

    // Audio Enhancement Setters
    suspend fun setEmbedMetadata(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EMBED_METADATA_KEY] = enabled
        }
    }

    suspend fun setConvertToMp3(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONVERT_TO_MP3_KEY] = enabled
        }
    }

    suspend fun setConvertAudioFormatEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONVERT_AUDIO_FORMAT_ENABLED_KEY] = enabled
        }
    }

    suspend fun setConvertAudioFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[CONVERT_AUDIO_FORMAT_KEY] = format
        }
    }

    suspend fun setCropArtwork(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CROP_ARTWORK_KEY] = enabled
        }
    }

    // Subtitle Setters
    suspend fun setDownloadSubtitles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_SUBTITLES_KEY] = enabled
        }
    }

    suspend fun setSubtitleFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_FORMAT_KEY] = format
        }
    }

    suspend fun setConvertSubtitles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONVERT_SUBTITLES_KEY] = enabled
        }
    }

    suspend fun setDownloadAutoCaptions(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_AUTO_CAPTIONS_KEY] = enabled
        }
    }

    suspend fun setCustomSubtitleLanguages(languages: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_SUBTITLE_LANGUAGES_KEY] = languages
        }
    }

    suspend fun setAutoTranslatedSubtitles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_TRANSLATED_SUBTITLES_KEY] = enabled
        }
    }

    suspend fun setEmbedSubtitles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EMBED_SUBTITLES_KEY] = enabled
        }
    }

    suspend fun setKeepSubtitleFiles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SUBTITLE_FILES_KEY] = enabled
        }
    }

    // Advanced Feature Setters
    suspend fun setEnableSponsorsBlock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SPONSORS_BLOCK_KEY] = enabled
        }
    }

    suspend fun setExtractAudio(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXTRACT_AUDIO_KEY] = enabled
        }
    }

    suspend fun setKeepVideoAfterAudioExtraction(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_VIDEO_AFTER_AUDIO_EXTRACTION_KEY] = enabled
        }
    }

    suspend fun setEnableCookies(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_COOKIES_KEY] = enabled
        }
    }

    suspend fun setMaxDownloadRetries(retries: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_DOWNLOAD_RETRIES_KEY] = retries
        }
    }

    suspend fun setPreferredLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_LANGUAGE_KEY] = language
        }
    }

    suspend fun setPreferredVideoFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_VIDEO_FORMAT_KEY] = format
        }
    }

    suspend fun setRemuxVideoContainer(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMUX_VIDEO_CONTAINER_KEY] = enabled
        }
    }

    suspend fun setFormatSorting(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FORMAT_SORTING_KEY] = enabled
        }
    }

    suspend fun setSortingFields(fields: String) {
        context.dataStore.edit { preferences ->
            preferences[SORTING_FIELDS_KEY] = fields
        }
    }

    suspend fun setFormatSelection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FORMAT_SELECTION_KEY] = enabled
        }
    }

    suspend fun setClipVideo(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLIP_VIDEO_KEY] = enabled
        }
    }

    suspend fun setMergeMultipleAudio(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MERGE_MULTIPLE_AUDIO_KEY] = enabled
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
    // Enhanced Video Qualities with Best/Lowest options
    val ENHANCED_VIDEO_MP4_QUALITIES = listOf(
        QualityOption("Best Quality", "best"),
        QualityOption("2160p (4K)", "2160p"),
        QualityOption("1440p (2K)", "1440p"),
        QualityOption("1080p (FHD)", "1080p"),
        QualityOption("720p (HD)", "720p", true),
        QualityOption("480p", "480p"),
        QualityOption("360p", "360p"),
        QualityOption("240p", "240p"),
        QualityOption("144p", "144p"),
        QualityOption("Lowest Quality", "worst")
    )

    val ENHANCED_VIDEO_WEBM_QUALITIES = listOf(
        QualityOption("Best Quality", "best"),
        QualityOption("2160p (4K)", "2160p"),
        QualityOption("1440p (2K)", "1440p"),
        QualityOption("1080p (FHD)", "1080p"),
        QualityOption("720p (HD)", "720p", true),
        QualityOption("480p", "480p"),
        QualityOption("360p", "360p"),
        QualityOption("240p", "240p"),
        QualityOption("144p", "144p"),
        QualityOption("Lowest Quality", "worst")
    )

    // Enhanced Audio Qualities with unlimited option
    val ENHANCED_AUDIO_M4A_QUALITIES = listOf(
        QualityOption("Unlimited Quality", "unlimited"),
        QualityOption("320kbps", "320kbps"),
        QualityOption("256kbps", "256kbps"),
        QualityOption("192kbps", "192kbps"),
        QualityOption("128kbps", "128kbps", true),
        QualityOption("96kbps", "96kbps"),
        QualityOption("64kbps", "64kbps"),
        QualityOption("48kbps", "48kbps")
    )

    val ENHANCED_AUDIO_WEBM_OPUS_QUALITIES = listOf(
        QualityOption("Unlimited Quality", "unlimited"),
        QualityOption("256kbps", "256kbps"),
        QualityOption("192kbps", "192kbps"),
        QualityOption("160kbps", "160kbps"),
        QualityOption("128kbps", "128kbps", true),
        QualityOption("96kbps", "96kbps"),
        QualityOption("64kbps", "64kbps"),
        QualityOption("48kbps", "48kbps")
    )

    // Legacy qualities for backwards compatibility
    val VIDEO_MP4_QUALITIES = ENHANCED_VIDEO_MP4_QUALITIES.filter {
        !listOf("best", "worst").contains(it.value)
    }.take(5)

    val VIDEO_WEBM_QUALITIES = ENHANCED_VIDEO_WEBM_QUALITIES.filter {
        !listOf("best", "worst").contains(it.value)
    }.take(5)

    val AUDIO_M4A_QUALITIES = ENHANCED_AUDIO_M4A_QUALITIES.filter {
        it.value != "unlimited"
    }.take(4)

    val AUDIO_WEBM_OPUS_QUALITIES = ENHANCED_AUDIO_WEBM_OPUS_QUALITIES.filter {
        it.value != "unlimited"
    }.take(4)
}