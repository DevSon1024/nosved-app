package com.devson.nosved.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.data.DownloadMode
import com.devson.nosved.data.QualityOption
import com.devson.nosved.viewmodel.FormatSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubtitleSettings: () -> Unit,
    viewModel: FormatSettingsViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current

    // Collect flows
    val downloadMode by viewModel.downloadMode.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val audioContainer by viewModel.audioContainer.collectAsState()
    val convertAudioFormatEnabled by viewModel.convertAudioFormatEnabled.collectAsState()
    val convertAudioFormat by viewModel.convertAudioFormat.collectAsState()
    val embedMetadata by viewModel.embedMetadata.collectAsState()
    val cropArtwork by viewModel.cropArtwork.collectAsState()

    val preferredVideoFormat by viewModel.preferredVideoFormat.collectAsState()
    val remuxVideoContainer by viewModel.remuxVideoContainer.collectAsState()

    val formatSorting by viewModel.formatSorting.collectAsState()
    val sortingFields by viewModel.sortingFields.collectAsState()
    val formatSelection by viewModel.formatSelection.collectAsState()
    val clipVideo by viewModel.clipVideo.collectAsState()
    val mergeMultipleAudio by viewModel.mergeMultipleAudio.collectAsState()

    val downloadPlaylist by viewModel.downloadPlaylist.collectAsState()
    val saveThumbnail by viewModel.saveThumbnail.collectAsState()
    val downloadSubtitles by viewModel.downloadSubtitles.collectAsState()

    // Dialog & Sheet States
    var showAudioFormatSheet by remember { mutableStateOf(false) }
    var showAudioQualitySheet by remember { mutableStateOf(false) }
    var showConvertAudioSheet by remember { mutableStateOf(false) }
    var showVideoFormatSheet by remember { mutableStateOf(false) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    var showSortingDialog by remember { mutableStateOf(false) }
    var tempSortingInput by remember { mutableStateOf("") }

    val isAudioOnly = downloadMode == DownloadMode.AUDIO_ONLY

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    FormatSettingCard(
                        title = "Save as audio",
                        subtitle = "Download and save audio, instead of video",
                        checked = isAudioOnly,
                        onCheckedChange = {
                            val mode = if (it) DownloadMode.AUDIO_ONLY else DownloadMode.VIDEO_AUDIO
                            viewModel.setDownloadMode(mode)
                        }
                    )
                    FormatSettingCard(
                        title = "Preferred audio format",
                        subtitle = audioContainer,
                        onClick = { showAudioFormatSheet = true }
                    )
                    FormatSettingCard(
                        title = "Audio quality",
                        subtitle = audioQuality,
                        onClick = { showAudioQualitySheet = true }
                    )
                    FormatSettingCard(
                        title = "Convert audio format",
                        subtitle = "Convert to ${convertAudioFormat.uppercase()}",
                        enabled = isAudioOnly,
                        onClick = { if (convertAudioFormatEnabled) showConvertAudioSheet = true },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (convertAudioFormatEnabled && isAudioOnly) {
                                    TextButton(onClick = { showConvertAudioSheet = true }) {
                                        Text("Format")
                                    }
                                }
                                Switch(
                                    checked = convertAudioFormatEnabled,
                                    onCheckedChange = { viewModel.setConvertAudioFormatEnabled(it) },
                                    enabled = isAudioOnly
                                )
                            }
                        }
                    )
                    FormatSettingCard(
                        title = "Embed metadata",
                        subtitle = "Embed metadata and video thumbnail into the audio file",
                        checked = embedMetadata,
                        enabled = isAudioOnly,
                        onCheckedChange = { viewModel.setEmbedMetadata(it) }
                    )
                    FormatSettingCard(
                        title = "Crop artwork",
                        subtitle = "Crop embedded image into square",
                        checked = cropArtwork,
                        enabled = isAudioOnly,
                        onCheckedChange = { viewModel.setCropArtwork(it) }
                    )
                }
            }

            // Video Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    FormatSettingCard(
                        title = "Preferred video format",
                        subtitle = if (preferredVideoFormat == "legacy") "Legacy-MP4 (H.264)" else "Quality (AV1, VP9, H.265)",
                        enabled = !isAudioOnly,
                        onClick = { showVideoFormatSheet = true }
                    )
                    FormatSettingCard(
                        title = "Video quality",
                        subtitle = videoQuality,
                        enabled = !isAudioOnly,
                        onClick = { showVideoQualitySheet = true }
                    )
                    FormatSettingCard(
                        title = "Remux video container",
                        subtitle = "Remux videos into MKV container for better compatibility",
                        checked = remuxVideoContainer,
                        enabled = !isAudioOnly,
                        onCheckedChange = { viewModel.setRemuxVideoContainer(it) }
                    )
                }
            }

            // Advanced Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    FormatSettingCard(
                        title = "Subtitle",
                        subtitle = "Languages, embed subtitles, auto captions",
                        onClick = { onNavigateToSubtitleSettings() }
                    )
                    ClickableSwitchFormatSettingCard(
                        title = "Format sorting",
                        subtitle = "Sorting formats with the -S option of yt-dlp",
                        checked = formatSorting,
                        onClick = {
                            if (formatSorting) {
                                tempSortingInput = sortingFields
                                showSortingDialog = true
                            }
                        },
                        onToggle = { viewModel.setFormatSorting(it) }
                    )
                    FormatSettingCard(
                        title = "Format selection",
                        subtitle = "Select the format to download before starting the download",
                        checked = formatSelection,
                        onCheckedChange = { viewModel.setFormatSelection(it) }
                    )
                    FormatSettingCard(
                        title = "Clip video",
                        subtitle = "Make video clips in the format selection page",
                        checked = clipVideo,
                        enabled = formatSelection,
                        onCheckedChange = { viewModel.setClipVideo(it) }
                    )
                    FormatSettingCard(
                        title = "Merger Multiple audio",
                        subtitle = "Merge multiple audio streams if available",
                        checked = mergeMultipleAudio,
                        enabled = formatSelection,
                        onCheckedChange = { viewModel.setMergeMultipleAudio(it) }
                    )
                }
            }

            // Additional Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Additional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    FormatSettingCard(
                        title = "Download playlist",
                        subtitle = "Download full playlist if URL contains one",
                        checked = downloadPlaylist,
                        onCheckedChange = { viewModel.setDownloadPlaylist(it) }
                    )
                    FormatSettingCard(
                        title = "Download subtitles",
                        subtitle = "Download subtitles for videos if available",
                        checked = downloadSubtitles,
                        onCheckedChange = { viewModel.setDownloadSubtitles(it) }
                    )
                    FormatSettingCard(
                        title = "Save thumbnail",
                        subtitle = "Download video thumbnail file",
                        checked = saveThumbnail,
                        onCheckedChange = { viewModel.setSaveThumbnail(it) }
                    )
                }
            }
        }
    }

    // Preferred Audio Format Dialog
    if (showAudioFormatSheet) {
        var selectedOption by remember { mutableStateOf(audioContainer.lowercase()) }
        AlertDialog(
            onDismissRequest = { showAudioFormatSheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Preferred audio format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preferred format when multiple are provided",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    listOf(
                        "default" to "Not specified (default)",
                        "opus" to "OPUS",
                        "m4a" to "M4A"
                    ).forEach { item ->
                        val isSelected = selectedOption == item.first
                        Surface(
                            onClick = { selectedOption = item.first },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.second,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOption = item.first }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAudioContainer(selectedOption)
                        showAudioFormatSheet = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioFormatSheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Audio Quality Dialog
    if (showAudioQualitySheet) {
        var selectedOption by remember { mutableStateOf(audioQuality) }
        AlertDialog(
            onDismissRequest = { showAudioQualitySheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Audio quality",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Limit the audio quality when multiple are present",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    val qualities = listOf("unlimited", "320kbps", "256kbps", "192kbps", "128kbps", "96kbps", "64kbps", "48kbps")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        qualities.forEach { quality ->
                            val isSelected = selectedOption == quality
                            val displayLabel = when (quality) {
                                "unlimited" -> "Best quality"
                                else -> quality
                            }
                            Surface(
                                onClick = { selectedOption = quality },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedOption = quality }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAudioQuality(selectedOption)
                        showAudioQualitySheet = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioQualitySheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Convert Audio Format Dialog
    if (showConvertAudioSheet) {
        var selectedOption by remember { mutableStateOf(convertAudioFormat) }
        AlertDialog(
            onDismissRequest = { showConvertAudioSheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Convert audio format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select output audio conversion format",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    listOf("mp3", "m4a", "opus", "flac", "wav").forEach { format ->
                        val isSelected = selectedOption == format
                        Surface(
                            onClick = { selectedOption = format },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = format.uppercase(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOption = format }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setConvertAudioFormat(selectedOption)
                        showConvertAudioSheet = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertAudioSheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Preferred Video Format Dialog
    if (showVideoFormatSheet) {
        var selectedOption by remember { mutableStateOf(preferredVideoFormat) }
        AlertDialog(
            onDismissRequest = { showVideoFormatSheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Preferred video format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preferred format when downloading videos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                    listOf(
                        "legacy" to ("Legacy" to "Prefer MP4(H.264) formats for sharing to other apps"),
                        "quality" to ("Quality" to "Prefer AV1, VP9 or H.265 formats for watching in compatible apps")
                    ).forEach { item ->
                        val isSelected = selectedOption == item.first
                        Surface(
                            onClick = { selectedOption = item.first },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        text = item.second.first,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.second.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOption = item.first }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setPreferredVideoFormat(selectedOption)
                        showVideoFormatSheet = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoFormatSheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Video Quality Dialog
    if (showVideoQualitySheet) {
        var selectedOption by remember { mutableStateOf(videoQuality) }
        AlertDialog(
            onDismissRequest = { showVideoQualitySheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Video quality",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Limit the video quality when multiple are present",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    val qualities = listOf("best", "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p", "worst")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        qualities.forEach { quality ->
                            val isSelected = selectedOption == quality
                            val displayLabel = when (quality) {
                                "best" -> "Best quality"
                                "worst" -> "Lowest quality"
                                else -> quality
                            }
                            Surface(
                                onClick = { selectedOption = quality },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedOption = quality }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setVideoQuality(selectedOption)
                        showVideoQualitySheet = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoQualitySheet = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Format Sorting Dialog
    if (showSortingDialog) {
        AlertDialog(
            onDismissRequest = { showSortingDialog = false },
            title = { Text("Format Sorting Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Input custom sorting fields (comma-separated). Prefix with '+' or '-' to specify sort order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempSortingInput,
                        onValueChange = { tempSortingInput = it },
                        label = { Text("Sorting Fields (e.g. res,vcodec,acodec)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                tempSortingInput = ""
                            }
                        ) {
                            Text("Reset")
                        }
                        TextButton(
                            onClick = {
                                uriHandler.openUri("https://github.com/yt-dlp/yt-dlp#sorting-formats")
                            }
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("yt-dlp Docs")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setSortingFields(tempSortingInput)
                        showSortingDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSortingDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun FormatSettingCard(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    checked: Boolean? = null,
    onClick: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isToggled = checked ?: false
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled && (onClick != null || onCheckedChange != null)) {
                    Modifier.clickable {
                        if (onCheckedChange != null) onCheckedChange(!isToggled) else onClick?.invoke()
                    }
                } else Modifier
            )
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (isToggled && enabled && onCheckedChange != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToggled && enabled && onCheckedChange != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (onCheckedChange != null) {
                Switch(
                    checked = isToggled,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClickableSwitchFormatSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                    .clickable(enabled = enabled) { onClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "TAP TO SETUP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}