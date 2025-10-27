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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.devson.nosved.MainViewModel
import com.devson.nosved.ui.FormatSelectionSheet
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoInfo by viewModel.videoInfo.collectAsState()
    val selectedVideoFormat by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudioFormat by viewModel.selectedAudioFormat.collectAsState()
    var showFormatSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

                // Current Selection Card
                item {
                    // Fix: Store format values in local variables to avoid smart cast issues
                    val currentVideoFormat = selectedVideoFormat
                    val currentAudioFormat = selectedAudioFormat

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
                                    text = "Quality Selection",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (currentVideoFormat != null && currentAudioFormat != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Video Quality
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📹 Video",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "${currentVideoFormat.height ?: "?"}p",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = currentVideoFormat.ext?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    // Audio Quality
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🎵 Audio",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "${currentAudioFormat.abr ?: "?"}kbps",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = currentAudioFormat.ext?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "No formats selected - Please choose quality settings",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // Bottom Action Buttons (Fixed at bottom)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Format Selection Button
                    OutlinedButton(
                        onClick = { showFormatSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Quality Settings",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Download Button - Fix: Use local variables
                    val currentVideoFormat = selectedVideoFormat
                    val currentAudioFormat = selectedAudioFormat
                    val isDownloadEnabled = currentVideoFormat != null && currentAudioFormat != null

                    Button(
                        onClick = {
                            if (isDownloadEnabled) {
                                viewModel.downloadVideo(info, currentVideoFormat!!, currentAudioFormat!!)
                                onBack() // Go back after starting download
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isDownloadEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isDownloadEnabled)
                                "Download Video" else "Select Quality First",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Format Selection Sheet
        if (showFormatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFormatSheet = false },
                sheetState = sheetState,
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                val formats: List<VideoFormat> = info.formats ?: emptyList()

                // Fix: Use local variables for the sheet as well
                val currentVideoFormat = selectedVideoFormat
                val currentAudioFormat = selectedAudioFormat

                FormatSelectionSheet(
                    title = info.title ?: "Unknown Title",
                    thumbnailUrl = info.thumbnail,
                    formats = formats,
                    selectedVideo = currentVideoFormat,
                    selectedAudio = currentAudioFormat,
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
                            showFormatSheet = false
                            onBack() // Go back after starting download
                        }
                    }
                )
            }
        }
    }
}

// Helper functions
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