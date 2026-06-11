package com.devson.nosved.ui.common.sheet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.nosved.ui.groupFormats
import com.devson.nosved.ui.model.*
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
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
    onDownload: () -> Unit,
    onClose: () -> Unit,
    uploader: String? = null,
    duration: String? = null,
    onUpdateTitle: ((String) -> Unit)? = null,
    defaultVideoQuality: String = "720p"
) {
    val context = LocalContext.current
    val groups = remember(formats, defaultVideoQuality) { groupFormats(formats, defaultVideoQuality) }
    var showAllVideos by remember { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editTitleText by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Title") },
            text = {
                OutlinedTextField(
                    value = editTitleText,
                    onValueChange = { editTitleText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateTitle?.invoke(editTitleText)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Format selection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onDownload) {
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Video Preview Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                thumbnailUrl?.let { url ->
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        duration?.let { dur ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = dur,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uploader ?: "Unknown Channel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit title") },
                            onClick = {
                                menuExpanded = false
                                showEditDialog = true
                            }
                        )
                        thumbnailUrl?.let { url ->
                            DropdownMenuItem(
                                text = { Text("View thumbnail") },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Formats List (subtitle section is now inside the list)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subtitle Languages (scrolls with list)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtitle languages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {}
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Hindi (Original)", fontWeight = FontWeight.Medium) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = null
                            )
                        }
                    }
                }

                // Selected Section (Show what the user selected above Suggested)
                if (selectedVideo != null || selectedAudio != null) {
                    item {
                        SectionTitle("Selected")
                    }
                    item {
                        SelectedFormatsTile(video = selectedVideo, audio = selectedAudio)
                    }
                }

                // Suggested Section
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

                // Audio Section (Grid-like Row chunked by 2)
                if (groups.audioOnly.isNotEmpty()) {
                    item {
                        SectionTitle("Audio")
                    }
                    val audioChunks = groups.audioOnly.chunked(2)
                    items(audioChunks) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { fmt ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AudioTile(
                                        fmt = fmt,
                                        isSelected = selectedAudio?.formatId == fmt.formatId,
                                        onClick = { onSelectAudio(fmt) }
                                    )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Video (No Audio) Section (Grid-like Row chunked by 2)
                if (groups.videoOnly.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle("Video (no audio)")
                            if (groups.videoOnly.size > 4) {
                                TextButton(
                                    onClick = { showAllVideos = !showAllVideos },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (showAllVideos) "Show less" else "Show all ${groups.videoOnly.size} items",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    val videoList = if (showAllVideos) groups.videoOnly else groups.videoOnly.take(4)
                    val videoChunks = videoList.chunked(2)
                    items(videoChunks) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { fmt ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    VideoTile(
                                        fmt = fmt,
                                        isSelected = selectedVideo?.formatId == fmt.formatId,
                                        onClick = { onSelectVideo(fmt) }
                                    )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Mixed Video Section (Grid-like Row chunked by 2)
                if (groups.mixed.isNotEmpty()) {
                    item {
                        SectionTitle("Video")
                    }
                    val mixedChunks = groups.mixed.chunked(2)
                    items(mixedChunks) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { fmt ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    MixedTile(
                                        fmt = fmt,
                                        isSelected = selectedVideo?.formatId == fmt.formatId,
                                        onClick = { onSelectVideo(fmt) }
                                    )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Disclaimer note at bottom
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Most video streaming platforms deliver audio and video separately, you can select and merge an audio-only format with a video-only format to a single video.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun FormatCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    line1: String,
    line2: String,
    line3: String,
    modifier: Modifier = Modifier,
    icons: @Composable () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp)
            ) {
                Text(
                    text = line1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line2,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = line3,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                icons()
            }
        }
    }
}

@Composable
private fun SelectedFormatsTile(video: VideoFormat?, audio: VideoFormat?) {
    val title = buildString {
        if (video != null) {
            append(getVideoLabel(video))
        }
        if (audio != null) {
            if (isNotEmpty()) append(" + ")
            append("${audio.formatId ?: ""} - audio only${getAudioLabel(audio)}")
        }
    }
    
    val sizeStr = humanSize((video?.fileSize ?: 0L) + (audio?.fileSize ?: 0L))
    val vTbr = video?.tbr ?: video?.abr ?: 0.0
    val aAbr = audio?.abr ?: audio?.tbr ?: 0.0
    val brStr = formatBitrate(vTbr.toDouble() + aAbr.toDouble())
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(brStr)
        }
    }
    
    val line3 = buildString {
        val ext = video?.ext ?: audio?.ext ?: ""
        val v = video?.vcodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
        val a = audio?.acodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
        val cleanV = if (v.isNotBlank() && v != "NONE") v else ""
        val cleanA = if (a.isNotBlank() && a != "NONE") a else ""
        
        val codecsList = buildList {
            if (cleanV.isNotBlank()) add(cleanV)
            if (cleanA.isNotBlank()) add(cleanA)
        }
        append(ext.uppercase())
        if (codecsList.isNotEmpty()) {
            append(" (${codecsList.joinToString(" ")})")
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 36.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (line2.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line2,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (line3.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = line3,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (video != null) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (audio != null) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedTile(pair: SuggestedPair, isSelected: Boolean, onClick: () -> Unit) {
    val title = "${getVideoLabel(pair.video)} + ${pair.audio.formatId ?: ""} - audio only${getAudioLabel(pair.audio)}"
    val sizeStr = humanSize(pair.estSizeBytes)
    val brStr = formatBitrate(pair.video.tbr ?: pair.video.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(brStr)
        }
    }
    
    val ext = "MKV"
    val v = pair.video.vcodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
    val a = pair.audio.acodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
    val cleanV = if (v.isNotBlank() && v != "NONE") v else ""
    val cleanA = if (a.isNotBlank() && a != "NONE") a else ""
    val codecsList = buildList {
        if (cleanV.isNotBlank()) add(cleanV)
        if (cleanA.isNotBlank()) add(cleanA)
    }
    val codecsStr = if (codecsList.isNotEmpty()) " (${codecsList.joinToString(" ")})" else ""
    val line3 = "$ext$codecsStr"

    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    FormatCard(
        isSelected = isSelected,
        onClick = onClick,
        line1 = title,
        line2 = line2,
        line3 = line3
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AudioTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val title = "${fmt.formatId ?: ""} - audio only${getAudioLabel(fmt)}"
    val sizeStr = humanSize(fmt.fileSize)
    val brStr = formatBitrate(fmt.abr ?: fmt.tbr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(brStr)
        }
    }
    val line3 = formatContainerCodec(fmt)
    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    FormatCard(
        isSelected = isSelected,
        onClick = onClick,
        line1 = title,
        line2 = line2,
        line3 = line3
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun VideoTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val title = getVideoLabel(fmt)
    val sizeStr = humanSize(fmt.fileSize)
    val brStr = formatBitrate(fmt.tbr ?: fmt.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(brStr)
        }
    }
    val line3 = formatContainerCodec(fmt)
    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    FormatCard(
        isSelected = isSelected,
        onClick = onClick,
        line1 = title,
        line2 = line2,
        line3 = line3
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MixedTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val title = getVideoLabel(fmt)
    val sizeStr = humanSize(fmt.fileSize)
    val brStr = formatBitrate(fmt.tbr ?: fmt.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(brStr)
        }
    }
    val line3 = formatContainerCodec(fmt)
    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    FormatCard(
        isSelected = isSelected,
        onClick = onClick,
        line1 = title,
        line2 = line2,
        line3 = line3
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getAudioLabel(fmt: VideoFormat): String {
    val abr = fmt.abr ?: fmt.tbr ?: return ""
    val abrVal = abr.toDouble()
    return if (abrVal >= 100.0) " (medium)" else " [low]"
}

private fun getVideoLabel(fmt: VideoFormat): String {
    val width = fmt.width ?: 0
    val height = fmt.height ?: 0
    val id = fmt.formatId ?: ""
    return if (width > 0 && height > 0) {
        "$id - ${width}x${height} (${height}p)"
    } else if (height > 0) {
        "$id - ${height}p"
    } else {
        id
    }
}

private fun formatBitrate(kbps: Number?): String {
    if (kbps == null) return ""
    val d = kbps.toDouble()
    if (d <= 0.0) return ""
    return if (d >= 1000.0) {
        String.format("%.2f Mbps", d / 1000.0)
    } else {
        if (d == d.toInt().toDouble()) {
            "${d.toInt()} Kbps"
        } else {
            String.format("%.1f Kbps", d)
        }
    }
}

private fun formatContainerCodec(fmt: VideoFormat): String {
    val ext = fmt.ext?.uppercase() ?: ""
    val v = fmt.vcodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
    val a = fmt.acodec?.uppercase()?.replace(Regex(".*\\."), "") ?: ""
    
    val cleanV = if (v.isNotBlank() && v != "NONE") v else ""
    val cleanA = if (a.isNotBlank() && a != "NONE") a else ""
    
    val codecsList = buildList {
        if (cleanV.isNotBlank()) add(cleanV)
        if (cleanA.isNotBlank()) add(cleanA)
    }
    
    val codecsStr = if (codecsList.isNotEmpty()) {
        " (${codecsList.joinToString(" ")})"
    } else {
        ""
    }
    
    return "$ext$codecsStr"
}
