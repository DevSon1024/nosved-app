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

                // Enhanced Thumbnail with overlay
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AsyncImage(
                            model = info.thumbnail,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Video Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Title
                            Text(
                                text = info.title ?: "Unknown Title",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )

                            // Video Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📺 ${info.uploader ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "⏱️ ${formatDuration(info.duration)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                info.viewCount?.let { views ->
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Views",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatViewCount(views),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quality Selection Card (New Seal-like interface)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Download Quality",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Download Mode Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Video + Audio Button
                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.VIDEO_AUDIO,
                                    onClick = { selectedDownloadMode = DownloadMode.VIDEO_AUDIO },
                                    label = { Text("Video + Audio") },
                                    modifier = Modifier.weight(1f)
                                )

                                // Audio Only Button
                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.AUDIO_ONLY,
                                    onClick = { selectedDownloadMode = DownloadMode.AUDIO_ONLY },
                                    label = { Text("Audio Only") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Show current selection preview
                            if (selectedDownloadMode == DownloadMode.VIDEO_AUDIO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Video Quality Preview
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📹 Video",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = defaultVideoQuality,
                                                style = MaterialTheme.typography.titleMedium,
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
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🎵 Audio",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = defaultAudioQuality,
                                                style = MaterialTheme.typography.titleMedium,
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
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🎵 Audio Only",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = defaultAudioQuality,
                                            style = MaterialTheme.typography.titleMedium,
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

            // Bottom Action Buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quality Settings Button
                    OutlinedButton(
                        onClick = { showQualityDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Quality Settings")
                    }

                    // Download Button
                    Button(
                        onClick = {
                            // Use smart format selection based on mode and preferences
                            viewModel.downloadVideoWithQuality(
                                videoInfo = info,
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

        // Advanced Format Selection Sheet (Original)
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
                            viewModel.downloadVideo(info, videoFormat, audioFormat)
                            showAdvancedSheet = false
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

// Helper functions remain the same...
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
