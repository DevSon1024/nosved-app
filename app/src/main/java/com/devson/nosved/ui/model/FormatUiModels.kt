package com.devson.nosved.ui.model

import com.yausername.youtubedl_android.mapper.VideoFormat

data class SuggestedPair(
    val video: VideoFormat,
    val audio: VideoFormat,
    val label: String,
    val estSizeBytes: Long?
)

fun humanSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun abrKbps(fmt: VideoFormat): String {
    val abr = fmt.abr ?: return ""
    return "${abr} Kbps"
}

fun vBitrateKbps(fmt: VideoFormat): String {
    val br = fmt.tbr ?: return ""
    return "${br} Kbps"
}

fun heightLabel(fmt: VideoFormat): String {
    val h = fmt.height ?: return ""
    val fps = fmt.fps?.toInt()?.let { if (it > 0) "${it}p" else "" } ?: ""
    return if (fps.isBlank()) "${h}p" else "${h}p${fps}"
}

fun codecContainer(fmt: VideoFormat): String {
    val v = fmt.vcodec?.uppercase() ?: ""
    val a = fmt.acodec?.uppercase() ?: ""
    val ext = fmt.ext?.uppercase() ?: ""
    val list = buildList {
        if (ext.isNotBlank()) add(ext)
        if (v.isNotBlank() && v != "NONE") add(v)
        if (a.isNotBlank() && a != "NONE") add(a)
    }
    return list.joinToString(" ")
}

fun isAudioOnly(fmt: VideoFormat): Boolean =
    (fmt.vcodec == null || fmt.vcodec == "none") && (fmt.acodec != null && fmt.acodec != "none")

fun isVideoOnly(fmt: VideoFormat): Boolean =
    (fmt.vcodec != null && fmt.vcodec != "none") && (fmt.acodec == null || fmt.acodec == "none")
