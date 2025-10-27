package com.devson.nosved.util

import com.yausername.youtubedl_android.mapper.VideoFormat

object QualityHelper {

    fun findNearestVideoQuality(
        formats: List<VideoFormat>,
        targetHeight: Int,
        preferredContainer: String = "mp4"
    ): VideoFormat? {
        val videoFormats = formats.filter {
            it.vcodec != "none" && it.acodec == "none"
        }

        if (videoFormats.isEmpty()) return null

        // First, try to find exact match with preferred container
        val exactMatch = videoFormats.find {
            it.height == targetHeight && it.ext.equals(preferredContainer, ignoreCase = true)
        }
        if (exactMatch != null) return exactMatch

        // Find nearest lower quality with preferred container
        val lowerQualities = videoFormats.filter {
            (it.height ?: 0) <= targetHeight &&
                    it.ext.equals(preferredContainer, ignoreCase = true)
        }.sortedByDescending { it.height }

        if (lowerQualities.isNotEmpty()) return lowerQualities.first()

        // Fallback to any container with lower quality
        val anyLowerQualities = videoFormats.filter {
            (it.height ?: 0) <= targetHeight
        }.sortedByDescending { it.height }

        if (anyLowerQualities.isNotEmpty()) return anyLowerQualities.first()

        // If no lower quality available, return the lowest available
        return videoFormats.minByOrNull { it.height ?: Int.MAX_VALUE }
    }

    fun findNearestAudioQuality(
        formats: List<VideoFormat>,
        targetBitrate: Int,
        preferredContainer: String = "m4a"
    ): VideoFormat? {
        val audioFormats = formats.filter {
            it.acodec != "none" && it.vcodec == "none"
        }

        if (audioFormats.isEmpty()) return null

        // First, try exact match with preferred container
        val exactMatch = audioFormats.find {
            it.abr == targetBitrate && it.ext.equals(preferredContainer, ignoreCase = true)
        }
        if (exactMatch != null) return exactMatch

        // Find nearest lower quality with preferred container
        val lowerQualities = audioFormats.filter {
            (it.abr ?: 0) <= targetBitrate &&
                    it.ext.equals(preferredContainer, ignoreCase = true)
        }.sortedByDescending { it.abr }

        if (lowerQualities.isNotEmpty()) return lowerQualities.first()

        // Fallback to any container with lower quality
        val anyLowerQualities = audioFormats.filter {
            (it.abr ?: 0) <= targetBitrate
        }.sortedByDescending { it.abr }

        if (anyLowerQualities.isNotEmpty()) return anyLowerQualities.first()

        // Return the lowest available if no suitable quality found
        return audioFormats.minByOrNull { it.abr ?: Int.MAX_VALUE }
    }

    fun parseQualityFromString(quality: String): Int {
        return quality.replace("p", "").replace("kbps", "").toIntOrNull() ?: 0
    }

    fun getFilteredFormats(
        formats: List<VideoFormat>,
        videoContainer: String,
        audioContainer: String
    ): FilteredFormats {
        val videoFormats = formats.filter {
            it.vcodec != "none" && it.acodec == "none" &&
                    it.ext.equals(videoContainer, ignoreCase = true)
        }.sortedByDescending { it.height }

        val audioFormats = formats.filter {
            it.acodec != "none" && it.vcodec == "none" &&
                    it.ext.equals(audioContainer, ignoreCase = true)
        }.sortedByDescending { it.abr }

        return FilteredFormats(videoFormats, audioFormats)
    }
}

data class FilteredFormats(
    val video: List<VideoFormat>,
    val audio: List<VideoFormat>
)
