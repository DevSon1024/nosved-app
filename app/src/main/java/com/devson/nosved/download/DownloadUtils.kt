package com.devson.nosved.download

import java.io.File
/**
 * Extracts the download speed (e.g., "1.2MiB/s") from a yt-dlp progress line.
 */
fun extractSpeed(line: String): String {
    val speedRegex = "([0-9.]+[KMG]iB/s)".toRegex()
    return speedRegex.find(line)?.value ?: ""
}

/**
 * Extracts the estimated time remaining (e.g., "00:30") from a yt-dlp progress line.
 */
fun extractETA(line: String): String {
    val etaRegex = "ETA ([0-9:]+)".toRegex()
    return etaRegex.find(line)?.groupValues?.get(1) ?: ""
}

/**
 * Sanitizes a video title to be used as a safe filename.
 */
fun sanitizeTitle(title: String): String {
    return title
        .replace(Regex("[^a-zA-Z0-9\\s.-]"), "-") // Remove special chars but keep spaces, dots, hyphens
        .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
        .trim()
        .take(100) // Limit length to prevent issues
}