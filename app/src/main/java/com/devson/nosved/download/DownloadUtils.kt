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
 * This version is less restrictive to allow emojis and special characters,
 * only removing characters that are illegal in file paths.
 */
fun sanitizeTitle(title: String): String {
    // Regex to match illegal filesystem characters: / \ ? % * : | " < >
    // and control characters (0x00-0x1F)
    val illegalCharsRegex = Regex("[/\\\\?%*:|\"<>\\x00-\\x1F]")

    return title
        .replace(illegalCharsRegex, "-") // Replace illegal chars with a hyphen
        .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
        .trim()
        .take(200) // Increase max length
}