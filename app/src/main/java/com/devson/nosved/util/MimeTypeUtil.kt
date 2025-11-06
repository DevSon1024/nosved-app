package com.devson.nosved.download

import java.io.File
import java.util.Locale

object MimeTypeUtil {
    fun getMimeType(filePath: String, isAudioOnly: Boolean): String {
        val extension = File(filePath).extension.lowercase(Locale.ROOT)

        return when (extension) {
            "mp3" -> "audio/mpeg"
            "m4a", "m4b" -> "audio/mp4a-latm"
            "opus" -> "audio/opus"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"

            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"

            // Fallback
            else -> if (isAudioOnly) "audio/*" else "video/*"
        }
    }
}