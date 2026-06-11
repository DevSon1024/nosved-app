package com.devson.nosved.ui

import com.devson.nosved.ui.model.SuggestedPair
import com.devson.nosved.ui.model.heightLabel
import com.yausername.youtubedl_android.mapper.VideoFormat

data class FormatGroups(
    val audioOnly: List<VideoFormat>,
    val videoOnly: List<VideoFormat>,
    val mixed: List<VideoFormat>,
    val suggested: List<SuggestedPair>
)

fun groupFormats(all: List<VideoFormat>, defaultQuality: String): FormatGroups {
    val audio = all.filter { (it.vcodec == null || it.vcodec == "none") && (it.acodec != null && it.acodec != "none") }
        .sortedByDescending { it.abr ?: 0 }
    val video = all.filter { (it.vcodec != null && it.vcodec != "none") && (it.acodec == null || it.acodec == "none") }
        .sortedWith(compareByDescending<VideoFormat> { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })
    val mixed = all.filter { (it.vcodec != null && it.vcodec != "none") && (it.acodec != null && it.acodec != "none") }
        .sortedWith(compareByDescending<VideoFormat> { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })

    val pairs = buildList {
        val bestAudio = audio.firstOrNull()
        if (bestAudio != null && video.isNotEmpty()) {
            val targetHeight = defaultQuality.filter { it.isDigit() }.toIntOrNull() ?: -1
            val targetVideos = if (targetHeight > 0) {
                video.filter { (it.height ?: 0) == targetHeight }
            } else {
                emptyList()
            }
            
            val selectedVideos = if (targetVideos.isNotEmpty()) {
                targetVideos
            } else {
                val maxHeight = video.maxOfOrNull { it.height ?: 0 } ?: 0
                video.filter { (it.height ?: 0) == maxHeight }
            }
            
            for (v in selectedVideos) {
                val est = (v.fileSize ?: 0L) + (bestAudio.fileSize ?: 0L)
                add(
                    SuggestedPair(
                        video = v,
                        audio = bestAudio,
                        label = "${heightLabel(v)} + audio only",
                        estSizeBytes = if (est > 0) est else null
                    )
                )
            }
        }
    }

    return FormatGroups(audioOnly = audio, videoOnly = video, mixed = mixed, suggested = pairs)
}
