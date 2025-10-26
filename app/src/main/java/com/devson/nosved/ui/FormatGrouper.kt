package com.devson.nosved.ui

import com.devson.nosved.ui.model.SuggestedPair
import com.devson.nosved.ui.model.heightLabel
import com.yausername.youtubedl_android.mapper.VideoFormat

data class FormatGroups(
    val audioOnly: List<VideoFormat>,
    val videoOnly: List<VideoFormat>,
    val suggested: List<SuggestedPair>
)

fun groupFormats(all: List<VideoFormat>): FormatGroups {
    val audio = all.filter { (it.vcodec == null || it.vcodec == "none") && (it.acodec != null && it.acodec != "none") }
        .sortedByDescending { it.abr ?: 0 }
    val video = all.filter { (it.vcodec != null && it.vcodec != "none") && (it.acodec == null || it.acodec == "none") }
        .sortedWith(compareByDescending<VideoFormat> { it.height ?: 0 }.thenByDescending { it.tbr ?: 0.0 })

    // Suggested pairs: pick top 3 useful combos
    val pairs = buildList {
        val targetHeights = listOf(720, 480, 360)
        for (h in targetHeights) {
            val v = video.firstOrNull { (it.height ?: 0) >= h }
                ?: continue
            val a = audio.firstOrNull() ?: continue
            // Use fileSize instead of filesize/filesizeApprox
            val est = (v.fileSize ?: 0L) + (a.fileSize ?: 0L)
            add(SuggestedPair(v, a, "${heightLabel(v)} + audio only (medium)", if (est > 0) est else null))
        }
        // Fallback best
        if (isEmpty() && video.isNotEmpty() && audio.isNotEmpty()) {
            val v = video.first()
            val a = audio.first()
            val est = (v.fileSize ?: 0L) + (a.fileSize ?: 0L)
            add(SuggestedPair(v, a, "${heightLabel(v)} + audio only", if (est > 0) est else null))
        }
    }

    return FormatGroups(audioOnly = audio, videoOnly = video, suggested = pairs)
}
