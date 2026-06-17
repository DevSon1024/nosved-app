package com.devson.nosved.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
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
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.ui.groupFormats
import com.devson.nosved.ui.model.*
import com.devson.nosved.viewmodel.MainViewModel
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val defaultVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")

    val videoInfo by viewModel.videoInfo.collectAsState()
    val selectedVideo by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudio by viewModel.selectedAudioFormat.collectAsState()

    val info = videoInfo ?: run { onNavigateBack(); return }

    val formats: List<VideoFormat> = info.formats ?: emptyList()
    val groups = remember(formats, defaultVideoQuality) { groupFormats(formats, defaultVideoQuality) }

    // Editable title — local to this screen
    var customTitle by remember(info) { mutableStateOf(info.title ?: "") }
    var showAllVideos by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editTitleText by remember { mutableStateOf(customTitle) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Title", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Customize the title of the video before downloading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customTitle = editTitleText
                        showEditDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format selection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val v = selectedVideo
                            val a = selectedAudio
                            if (v != null || a != null) {
                                viewModel.downloadVideo(info, v, a, customTitle)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Three-dot overflow menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
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
                            info.thumbnail?.let { url ->
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            //  Video Preview Header 
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    info.thumbnail?.let { url ->
                        Box(
                            modifier = Modifier
                                .width(112.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            info.duration?.let { dur ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.8f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatScreenDuration(dur),
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
                            text = customTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.uploader ?: "Unknown Channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            //  Subtitle Languages 
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
                        .padding(top = 6.dp),
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

            //  Selected Section 
            if (selectedVideo != null || selectedAudio != null) {
                item { FssSectionTitle("Selected") }
                item { FssSelectedFormatsTile(video = selectedVideo, audio = selectedAudio) }
            }

            //  Suggested Section 
            if (groups.suggested.isNotEmpty()) {
                item { FssSectionTitle("Suggested") }
                items(groups.suggested) { s ->
                    FssSuggestedTile(
                        pair = s,
                        isSelected = selectedVideo?.formatId == s.video.formatId &&
                                selectedAudio?.formatId == s.audio.formatId,
                        onClick = {
                            viewModel.selectVideoFormat(s.video)
                            viewModel.selectAudioFormat(s.audio)
                        }
                    )
                }
            }

            //  Audio Section 
            if (groups.audioOnly.isNotEmpty()) {
                item { FssSectionTitle("Audio") }
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
                                FssAudioTile(
                                    fmt = fmt,
                                    isSelected = selectedAudio?.formatId == fmt.formatId,
                                    onClick = { viewModel.selectAudioFormat(fmt) }
                                )
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            //  Video (No Audio) Section 
            if (groups.videoOnly.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FssSectionTitle("Video (no audio)")
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
                items(videoList.chunked(2)) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { fmt ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                FssVideoTile(
                                    fmt = fmt,
                                    isSelected = selectedVideo?.formatId == fmt.formatId,
                                    onClick = { viewModel.selectVideoFormat(fmt) }
                                )
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            //  Mixed (Video + Audio) Section 
            if (groups.mixed.isNotEmpty()) {
                item { FssSectionTitle("Video") }
                items(groups.mixed.chunked(2)) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { fmt ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                FssMixedTile(
                                    fmt = fmt,
                                    isSelected = selectedVideo?.formatId == fmt.formatId,
                                    onClick = { viewModel.selectVideoFormat(fmt) }
                                )
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            //  Info Disclaimer 
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    ),
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
                            text = "Most video streaming platforms deliver audio and video separately. " +
                                    "Select and merge an audio-only format with a video-only format into a single video.",
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

//  Private helper composables 

@Composable
private fun FssSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun FssFormatCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    line1: String,
    line2: String,
    line3: String,
    modifier: Modifier = Modifier,
    icons: @Composable () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
            Box(modifier = Modifier.align(Alignment.BottomEnd)) { icons() }
        }
    }
}

@Composable
private fun FssSelectedFormatsTile(video: VideoFormat?, audio: VideoFormat?) {
    val title = buildString {
        if (video != null) append(fssGetVideoLabel(video))
        if (audio != null) {
            if (isNotEmpty()) append(" + ")
            append("${audio.formatId ?: ""} - audio only${fssGetAudioLabel(audio)}")
        }
    }
    val sizeStr = fssHumanSize((video?.fileSize ?: 0L) + (audio?.fileSize ?: 0L))
    val vTbr = video?.tbr ?: video?.abr ?: 0.0
    val aAbr = audio?.abr ?: audio?.tbr ?: 0.0
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        val br = fssFormatBitrate(vTbr.toDouble() + aAbr.toDouble())
        if (br.isNotBlank()) { if (isNotEmpty()) append(" "); append(br) }
    }
    val line3 = buildString {
        val ext = video?.ext ?: audio?.ext ?: ""
        val cleanV = video?.vcodec?.uppercase()?.replace(Regex(".*\\."), "")
            ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
        val cleanA = audio?.acodec?.uppercase()?.replace(Regex(".*\\."), "")
            ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
        append(ext.uppercase())
        val codecs = listOfNotNull(cleanV.takeIf { it.isNotBlank() }, cleanA.takeIf { it.isNotBlank() })
        if (codecs.isNotEmpty()) append(" (${codecs.joinToString(" ")})")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 36.dp)) {
                Text(
                    text = title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (line2.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line2, style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (line3.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = line3, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (video != null) Icon(
                        Icons.Default.Videocam, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                    )
                    if (audio != null) Icon(
                        Icons.Default.MusicNote, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FssSuggestedTile(pair: SuggestedPair, isSelected: Boolean, onClick: () -> Unit) {
    val title = "${fssGetVideoLabel(pair.video)} + ${pair.audio.formatId ?: ""} - audio only${fssGetAudioLabel(pair.audio)}"
    val sizeStr = fssHumanSize(pair.estSizeBytes)
    val brStr = fssFormatBitrate(pair.video.tbr ?: pair.video.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) { if (isNotEmpty()) append(" "); append(brStr) }
    }
    val cleanV = pair.video.vcodec?.uppercase()?.replace(Regex(".*\\."), "")
        ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
    val cleanA = pair.audio.acodec?.uppercase()?.replace(Regex(".*\\."), "")
        ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
    val codecs = listOfNotNull(cleanV.takeIf { it.isNotBlank() }, cleanA.takeIf { it.isNotBlank() })
    val line3 = "MKV${if (codecs.isNotEmpty()) " (${codecs.joinToString(" ")})" else ""}"
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    FssFormatCard(isSelected = isSelected, onClick = onClick, line1 = title, line2 = line2, line3 = line3) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Videocam, null, tint = tint, modifier = Modifier.size(16.dp))
            Icon(Icons.Default.MusicNote, null, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FssAudioTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val title = "${fmt.formatId ?: ""} - audio only${fssGetAudioLabel(fmt)}"
    val sizeStr = fssHumanSize(fmt.fileSize)
    val brStr = fssFormatBitrate(fmt.abr ?: fmt.tbr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) { if (isNotEmpty()) append(" "); append(brStr) }
    }
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    FssFormatCard(isSelected, onClick, title, line2, fssFormatContainerCodec(fmt)) {
        Icon(Icons.Default.MusicNote, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FssVideoTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val sizeStr = fssHumanSize(fmt.fileSize)
    val brStr = fssFormatBitrate(fmt.tbr ?: fmt.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) { if (isNotEmpty()) append(" "); append(brStr) }
    }
    FssFormatCard(isSelected, onClick, fssGetVideoLabel(fmt), line2, fssFormatContainerCodec(fmt)) {
        Icon(Icons.Default.Videocam, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FssMixedTile(fmt: VideoFormat, isSelected: Boolean, onClick: () -> Unit) {
    val sizeStr = fssHumanSize(fmt.fileSize)
    val brStr = fssFormatBitrate(fmt.tbr ?: fmt.abr)
    val line2 = buildString {
        if (sizeStr.isNotBlank()) append(sizeStr)
        if (brStr.isNotBlank()) { if (isNotEmpty()) append(" "); append(brStr) }
    }
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    FssFormatCard(isSelected, onClick, fssGetVideoLabel(fmt), line2, fssFormatContainerCodec(fmt)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Videocam, null, tint = tint, modifier = Modifier.size(16.dp))
            Icon(Icons.Default.MusicNote, null, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

//  Private pure helper functions 

private fun fssGetAudioLabel(fmt: VideoFormat): String {
    val abr = fmt.abr ?: fmt.tbr ?: return ""
    return if (abr.toDouble() >= 100.0) " (medium)" else " [low]"
}

private fun fssGetVideoLabel(fmt: VideoFormat): String {
    val w = fmt.width ?: 0; val h = fmt.height ?: 0; val id = fmt.formatId ?: ""
    return if (w > 0 && h > 0) "$id - ${w}x${h} (${h}p)" else if (h > 0) "$id - ${h}p" else id
}

private fun fssFormatBitrate(kbps: Number?): String {
    if (kbps == null) return ""
    val d = kbps.toDouble()
    if (d <= 0.0) return ""
    return if (d >= 1000.0) String.format("%.2f Mbps", d / 1000.0)
    else if (d == d.toInt().toDouble()) "${d.toInt()} Kbps"
    else String.format("%.1f Kbps", d)
}

private fun fssHumanSize(bytes: Long?): String {
    val b = bytes ?: return ""
    if (b <= 0L) return ""
    return when {
        b >= 1_000_000_000L -> String.format("%.2f GB", b / 1_000_000_000.0)
        b >= 1_000_000L -> String.format("%.2f MB", b / 1_000_000.0)
        b >= 1_000L -> String.format("%.1f KB", b / 1_000.0)
        else -> "$b B"
    }
}

private fun fssFormatContainerCodec(fmt: VideoFormat): String {
    val ext = fmt.ext?.uppercase() ?: ""
    val cleanV = fmt.vcodec?.uppercase()?.replace(Regex(".*\\."), "")
        ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
    val cleanA = fmt.acodec?.uppercase()?.replace(Regex(".*\\."), "")
        ?.takeIf { it.isNotBlank() && it != "NONE" } ?: ""
    val codecs = listOfNotNull(cleanV.takeIf { it.isNotBlank() }, cleanA.takeIf { it.isNotBlank() })
    return "$ext${if (codecs.isNotEmpty()) " (${codecs.joinToString(" ")})" else ""}"
}

private fun formatScreenDuration(seconds: Int?): String {
    if (seconds == null) return ""
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
