package com.devson.nosved.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
                top = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Section
            item {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Save as Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Save as audio",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Download and save audio, instead of video",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAudioOnly,
                                onCheckedChange = {
                                    val mode = if (it) DownloadMode.AUDIO_ONLY else DownloadMode.VIDEO_AUDIO
                                    viewModel.setDownloadMode(mode)
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Preferred Audio Format (Always enabled)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAudioFormatSheet = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Preferred audio format",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = audioContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Audio Quality (Always enabled)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAudioQualitySheet = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Audio quality",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = audioQuality,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Convert Audio Format
                        val convertAudioEnabled = isAudioOnly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (convertAudioEnabled) Modifier.clickable { showConvertAudioSheet = true } else Modifier)
                                .alpha(if (convertAudioEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Convert audio format",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Convert to $convertAudioFormat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (convertAudioFormatEnabled && convertAudioEnabled) {
                                    TextButton(onClick = { showConvertAudioSheet = true }) {
                                        Text("Format")
                                    }
                                }
                                Switch(
                                    checked = convertAudioFormatEnabled,
                                    onCheckedChange = { viewModel.setConvertAudioFormatEnabled(it) },
                                    enabled = convertAudioEnabled
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Embed Metadata
                        val embedMetadataEnabled = isAudioOnly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (embedMetadataEnabled) Modifier.clickable { viewModel.setEmbedMetadata(!embedMetadata) } else Modifier)
                                .alpha(if (embedMetadataEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Embed metadata",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Embed metadata and video thumbnail into the audio file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = embedMetadata,
                                onCheckedChange = { viewModel.setEmbedMetadata(it) },
                                enabled = embedMetadataEnabled
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Crop Artwork
                        val cropArtworkEnabled = isAudioOnly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (cropArtworkEnabled) Modifier.clickable { viewModel.setCropArtwork(!cropArtwork) } else Modifier)
                                .alpha(if (cropArtworkEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Crop artwork",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Crop embedded image into square",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cropArtwork,
                                onCheckedChange = { viewModel.setCropArtwork(it) },
                                enabled = cropArtworkEnabled
                            )
                        }
                    }
                }
            }

            // Video Section
            item {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                val videoCardEnabled = !isAudioOnly
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Preferred Video Format
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (videoCardEnabled) Modifier.clickable { showVideoFormatSheet = true } else Modifier)
                                .alpha(if (videoCardEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Preferred video format",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (preferredVideoFormat == "legacy") "Legacy-MP4 (H.264)" else "Quality (AV1, VP9, H.265)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Video Quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (videoCardEnabled) Modifier.clickable { showVideoQualitySheet = true } else Modifier)
                                .alpha(if (videoCardEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Video quality",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = videoQuality,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Remux Video Container
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (videoCardEnabled) Modifier.clickable { viewModel.setRemuxVideoContainer(!remuxVideoContainer) } else Modifier)
                                .alpha(if (videoCardEnabled) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Remux video container",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Remux videos into MKV container for better compatibility",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = remuxVideoContainer,
                                onCheckedChange = { viewModel.setRemuxVideoContainer(it) },
                                enabled = videoCardEnabled
                            )
                        }
                    }
                }
            }

            // Advanced Section
            item {
                Text(
                    text = "Advanced",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Subtitle settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSubtitleSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Subtitle",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Languages, embed subtitles, auto captions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Format Sorting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setFormatSorting(!formatSorting) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Format sorting",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Sorting formats with the -S option of yt-dlp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (formatSorting) {
                                    TextButton(
                                        onClick = {
                                            tempSortingInput = sortingFields
                                            showSortingDialog = true
                                        }
                                    ) {
                                        Text("Configure")
                                    }
                                }
                                Switch(
                                    checked = formatSorting,
                                    onCheckedChange = { viewModel.setFormatSorting(it) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Format Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setFormatSelection(!formatSelection) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Format selection",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Select the format to download before starting the download",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = formatSelection,
                                onCheckedChange = { viewModel.setFormatSelection(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Clip Video
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (formatSelection) Modifier.clickable { viewModel.setClipVideo(!clipVideo) } else Modifier)
                                .alpha(if (formatSelection) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Clip video",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Make video clips in the format selection page",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = clipVideo,
                                onCheckedChange = { viewModel.setClipVideo(it) },
                                enabled = formatSelection
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Merger Multiple Audio
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (formatSelection) Modifier.clickable { viewModel.setMergeMultipleAudio(!mergeMultipleAudio) } else Modifier)
                                .alpha(if (formatSelection) 1f else 0.38f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Merger Multiple audio",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Merge multiple audio streams if available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = mergeMultipleAudio,
                                onCheckedChange = { viewModel.setMergeMultipleAudio(it) },
                                enabled = formatSelection
                            )
                        }
                    }
                }
            }

            // Additional Section
            item {
                Text(
                    text = "Additional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Download Playlist
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDownloadPlaylist(!downloadPlaylist) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Download playlist",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Download full playlist if URL contains one",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = downloadPlaylist,
                                onCheckedChange = { viewModel.setDownloadPlaylist(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Download Subtitles
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDownloadSubtitles(!downloadSubtitles) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Download subtitles",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Download subtitles for videos if available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = downloadSubtitles,
                                onCheckedChange = { viewModel.setDownloadSubtitles(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        // Save Thumbnail
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSaveThumbnail(!saveThumbnail) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Save thumbnail",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Download video thumbnail file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = saveThumbnail,
                                onCheckedChange = { viewModel.setSaveThumbnail(it) }
                            )
                        }
                    }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = item.first }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == item.first,
                                onClick = { selectedOption = item.first }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(item.second, style = MaterialTheme.typography.bodyLarge)
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(qualities.size) { index ->
                            val quality = qualities[index]
                            val displayLabel = when (quality) {
                                "unlimited" -> "Best quality"
                                else -> quality
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = quality }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOption == quality,
                                    onClick = { selectedOption = quality }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(displayLabel, style = MaterialTheme.typography.bodyLarge)
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

    // Convert Audio Format Sheet
    if (showConvertAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConvertAudioSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Convert Audio Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                listOf("mp3", "m4a", "opus", "flac", "wav").forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setConvertAudioFormat(format)
                                showConvertAudioSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = convertAudioFormat == format,
                            onClick = {
                                viewModel.setConvertAudioFormat(format)
                                showConvertAudioSheet = false
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(format.uppercase(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf(
                        "legacy" to ("Legacy" to "Prefer MP4(H.264) formats for sharing to other apps"),
                        "quality" to ("Quality" to "Prefer AV1, VP9 or H.265 formats for watching in compatible apps")
                    ).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = item.first }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == item.first,
                                onClick = { selectedOption = item.first }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.second.first,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.second.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(qualities.size) { index ->
                            val quality = qualities[index]
                            val displayLabel = when (quality) {
                                "best" -> "Best quality"
                                "worst" -> "Lowest quality"
                                else -> quality
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = quality }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOption == quality,
                                    onClick = { selectedOption = quality }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(displayLabel, style = MaterialTheme.typography.bodyLarge)
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
            title = { Text("Format Sorting Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Input custom sorting fields (comma-separated). Prefix with '+' or '-' to specify sort order.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempSortingInput,
                        onValueChange = { tempSortingInput = it },
                        label = { Text("Sorting Fields (e.g. res,vcodec,acodec)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
            }
        )
    }
}