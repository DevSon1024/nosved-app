package com.devson.nosved.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devson.nosved.MainViewModel
import com.devson.nosved.data.DownloadMode
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.ui.components.QualitySelectionDialog
import com.devson.nosved.ui.FormatSelectionSheet
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }

    val videoInfo by viewModel.videoInfo.collectAsState()
    val selectedVideoFormat by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudioFormat by viewModel.selectedAudioFormat.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var selectedDownloadMode by remember { mutableStateOf(DownloadMode.VIDEO_AUDIO) }

    // State for the editable title
    var customTitle by remember(videoInfo) { mutableStateOf(videoInfo?.title ?: "") }
    var isEditingTitle by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect quality preferences
    val defaultVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val defaultAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")
    val downloadMode by qualityPrefs.downloadMode.collectAsState(initial = DownloadMode.VIDEO_AUDIO)

    // Update local state when preferences change
    LaunchedEffect(downloadMode) {
        selectedDownloadMode = downloadMode
    }

    // If no video info, go back
    LaunchedEffect(videoInfo) {
        if (videoInfo == null) {
            onBack()
        } else {
            // Update custom title if videoInfo changes and title is not yet modified
            if (customTitle.isEmpty() || customTitle == "Unknown Title") {
                customTitle = videoInfo?.title ?: "Unknown Title"
            }
        }
    }

    videoInfo?.let { info ->
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Enhanced Thumbnail with better positioning
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f), // Better aspect ratio for thumbnails
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = info.thumbnail,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Video Information Card with Editable Title
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Editable Title with smaller font
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEditingTitle) {
                                    OutlinedTextField(
                                        value = customTitle,
                                        onValueChange = { customTitle = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = false,
                                        maxLines = 3,
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp, // Decreased font size
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { isEditingTitle = false }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save Title",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = customTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp, // Decreased font size
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { isEditingTitle = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Title",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            // Video Stats with smaller fonts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📺 ${info.uploader ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp // Decreased font size
                                        ),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "⏱️ ${formatDuration(info.duration)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp // Decreased font size
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                info.viewCount?.let { views ->
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Views",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp // Decreased font size
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatViewCount(views),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 16.sp // Decreased font size
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quality Selection Card with better layout
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Download Quality",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp // Decreased font size
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Download Mode Selection with better spacing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Video + Audio Button
                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.VIDEO_AUDIO,
                                    onClick = { selectedDownloadMode = DownloadMode.VIDEO_AUDIO },
                                    label = {
                                        Text(
                                            "Video + Audio",
                                            fontSize = 13.sp // Decreased font size
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                // Audio Only Button
                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.AUDIO_ONLY,
                                    onClick = { selectedDownloadMode = DownloadMode.AUDIO_ONLY },
                                    label = {
                                        Text(
                                            "Audio Only",
                                            fontSize = 13.sp // Decreased font size
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Show current selection preview with smaller fonts
                            if (selectedDownloadMode == DownloadMode.VIDEO_AUDIO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Video Quality Preview
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📹 Video",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp // Decreased font size
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = defaultVideoQuality,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontSize = 14.sp // Decreased font size
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Audio Quality Preview
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🎵 Audio",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp // Decreased font size
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = defaultAudioQuality,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontSize = 14.sp // Decreased font size
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🎵 Audio Only",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp // Decreased font size
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = defaultAudioQuality,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = 14.sp // Decreased font size
                                            ),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // Bottom Action Buttons - New Layout: Change Quality | Advance Formats on top, Download full width below
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Change Quality and Advance Formats side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Change Quality Settings Button
                        OutlinedButton(
                            onClick = { showQualityDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Change Quality",
                                fontSize = 13.sp // Decreased font size
                            )
                        }

                        // Advance Formats Button (renamed from Advanced Format Selection)
                        OutlinedButton(
                            onClick = { showAdvancedSheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Advance Format",
                                fontSize = 13.sp // Decreased font size
                            )
                        }
                    }

                    // Bottom Row: Full width Download button
                    Button(
                        onClick = {
                            viewModel.downloadVideoWithQuality(
                                videoInfo = info,
                                customTitle = customTitle,
                                downloadMode = selectedDownloadMode,
                                preferredVideoQuality = defaultVideoQuality,
                                preferredAudioQuality = defaultAudioQuality
                            )
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Download ${if (selectedDownloadMode == DownloadMode.AUDIO_ONLY) "Audio" else "Video"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quality Selection Dialog
        if (showQualityDialog) {
            QualitySelectionDialog(
                currentMode = selectedDownloadMode,
                onModeChange = { selectedDownloadMode = it },
                onAdvancedClick = {
                    showQualityDialog = false
                    showAdvancedSheet = true
                },
                onDismiss = { showQualityDialog = false }
            )
        }

        // Advanced Format Selection Sheet
        if (showAdvancedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSheet = false },
                sheetState = sheetState,
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                val formats: List<VideoFormat> = info.formats ?: emptyList()

                FormatSelectionSheet(
                    title = info.title ?: "Unknown Title",
                    thumbnailUrl = info.thumbnail,
                    formats = formats,
                    selectedVideo = selectedVideoFormat,
                    selectedAudio = selectedAudioFormat,
                    onSelectVideo = { format -> viewModel.selectVideoFormat(format) },
                    onSelectAudio = { format -> viewModel.selectAudioFormat(format) },
                    onSelectSuggested = { video, audio ->
                        viewModel.selectVideoFormat(video)
                        viewModel.selectAudioFormat(audio)
                    },
                    onDownload = {
                        val videoFormat = selectedVideoFormat
                        val audioFormat = selectedAudioFormat
                        if (videoFormat != null && audioFormat != null) {
                            viewModel.downloadVideo(info, videoFormat, audioFormat, customTitle)
                            showAdvancedSheet = false
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

// Helper functions with same implementation
private fun formatDuration(duration: Int?): String {
    if (duration == null) return "Unknown"

    val hours = duration / 3600
    val minutes = (duration % 3600) / 60
    val seconds = duration % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatViewCount(count: Any?): String {
    val viewCount = when (count) {
        is Long -> count
        is Int -> count.toLong()
        is String -> count.toLongOrNull() ?: 0L
        else -> 0L
    }

    return when {
        viewCount >= 1_000_000 -> String.format("%.1fM", viewCount / 1_000_000.0)
        viewCount >= 1_000 -> String.format("%.1fK", viewCount / 1_000.0)
        else -> viewCount.toString()
    }
}