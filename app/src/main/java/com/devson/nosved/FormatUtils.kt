package com.devson.nosved

import com.yausername.youtubedl_android.mapper.VideoFormat

fun VideoFormat.getFormattedFileSize(): String {
    val sizeInMB = this.fileSize?.toFloat()?.div(1024f * 1024f)
    return if (sizeInMB != null) {
        String.format("%.2f MB", sizeInMB)
    } else {
        "Unknown size"
    }
}