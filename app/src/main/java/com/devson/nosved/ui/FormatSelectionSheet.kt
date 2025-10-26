package com.devson.nosved.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.model.*
import com.yausername.youtubedl_android.mapper.VideoFormat

@Composable
fun FormatSelectionSheet(
    title: String,
    thumbnailUrl: String?,
    formats: List<VideoFormat>,
    selectedVideo: VideoFormat?,
    selectedAudio: VideoFormat?,
    onSelectVideo: (VideoFormat) -> Unit,
    onSelectAudio: (VideoFormat) -> Unit,
    onSelectSuggested: (VideoFormat, VideoFormat) -> Unit,
    onDownload: () -> Unit
) {
    val groups = remember(formats) { groupFormats(formats) }

    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Optional: thumbnail display with Coil AsyncImage
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(onClick = onDownload) {
                    Text("Download")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 600.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (groups.suggested.isNotEmpty()) {
                    item {
                        SectionTitle("Suggested")
                    }
                    items(groups.suggested) { s ->
                        SuggestedTile(
                            pair = s,
                            isSelected = selectedVideo?.formatId == s.video.formatId && selectedAudio?.formatId == s.audio.formatId,
                            onClick = { onSelectSuggested(s.video, s.audio) }
                        )
                    }
                }

                item { SectionTitle("Audio") }
                items(groups.audioOnly) { a ->
                    AudioTile(
                        fmt = a,
                        isSelected = selectedAudio?.formatId == a.formatId,
                        onClick = { onSelectAudio(a) }
                    )
                }

                item { SectionTitle("Video (no audio)") }
                items(groups.videoOnly) { v ->
                    VideoTile(
                        fmt = v,
                        isSelected = selectedVideo?.formatId == v.formatId,
                        onClick = { onSelectVideo(v) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SuggestedTile(pair: SuggestedPair, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, shape = MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(pair.label, style = MaterialTheme.typography.bodyLarge)
        val size = humanSize(pair.estSizeBytes)
        val sub = buildString {
            append("${heightLabel(pair.video)} • ")
            append(codecContainer(pair.video))
            if (size.isNotBlank()) append(" • $size")
        }
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudioTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, shape = MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text("${fmt.formatId ?: ""} - audio only (medium)".trim(), style = MaterialTheme.typography.bodyLarge)
        // Fix: Use fileSize instead of filesize/filesizeApprox
        val size = humanSize(fmt.fileSize)
        val sub = buildString {
            val abr = abrKbps(fmt)
            val cont = codecContainer(fmt)
            if (abr.isNotBlank()) append("$abr • ")
            append(cont)
            if (size.isNotBlank()) append(" • $size")
        }
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VideoTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, shape = MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text("${fmt.formatId ?: ""} - ${heightLabel(fmt)}".trim(), style = MaterialTheme.typography.bodyLarge)
        // Fix: Use fileSize instead of filesize/filesizeApprox
        val size = humanSize(fmt.fileSize)
        val sub = buildString {
            val br = vBitrateKbps(fmt)
            val cont = codecContainer(fmt)
            if (br.isNotBlank()) append("$br • ")
            append(cont)
            if (size.isNotBlank()) append(" • $size")
        }
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
