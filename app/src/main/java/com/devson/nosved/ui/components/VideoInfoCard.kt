package com.devson.nosved.ui.components

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
import com.devson.nosved.ui.FormatSelectionSheet
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoCard(
    videoInfo: VideoInfo,
    selectedVideoFormat: VideoFormat?,
    selectedAudioFormat: VideoFormat?,
    onVideoFormatSelected: (VideoFormat) -> Unit,
    onAudioFormatSelected: (VideoFormat) -> Unit,
    onDownloadClicked: (VideoFormat, VideoFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFormatSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Card(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Thumbnail with gradient overlay
            item {
                AsyncImage(
                    model = videoInfo.thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Video Information with Material Design 3 styling
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = videoInfo.title ?: "Unknown Title",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Video Stats Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "📺 ${videoInfo.uploader ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "⏱️ ${formatDuration(videoInfo.duration)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                videoInfo.viewCount?.let { views ->
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "👁️ Views",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatViewCount(views),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Enhanced Current Selection Display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Selection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (selectedVideoFormat != null && selectedAudioFormat != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "📹 Video Quality",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${selectedVideoFormat.height ?: "?"}p • ${selectedVideoFormat.ext?.uppercase() ?: "?"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "🎵 Audio Quality",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${selectedAudioFormat.abr ?: "?"}kbps • ${selectedAudioFormat.ext?.uppercase() ?: "?"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "⚠️ No formats selected - Please choose quality settings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Enhanced Action Buttons
            item {
                Column(
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
                            text = "Format Selection",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Enhanced Download Button
                    Button(
                        onClick = {
                            if (selectedVideoFormat != null && selectedAudioFormat != null) {
                                onDownloadClicked(selectedVideoFormat, selectedAudioFormat)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedVideoFormat != null && selectedAudioFormat != null,
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
                            text = if (selectedVideoFormat != null && selectedAudioFormat != null)
                                "Download Video" else "Select Format First",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
            val formats: List<VideoFormat> = videoInfo.formats ?: emptyList()
            FormatSelectionSheet(
                title = videoInfo.title ?: "Unknown Title",
                thumbnailUrl = videoInfo.thumbnail,
                formats = formats,
                selectedVideo = selectedVideoFormat,
                selectedAudio = selectedAudioFormat,
                onSelectVideo = onVideoFormatSelected,
                onSelectAudio = onAudioFormatSelected,
                onSelectSuggested = { video, audio ->
                    onVideoFormatSelected(video)
                    onAudioFormatSelected(audio)
                },
                onDownload = {
                    if (selectedVideoFormat != null && selectedAudioFormat != null) {
                        onDownloadClicked(selectedVideoFormat, selectedAudioFormat)
                        showFormatSheet = false
                    }
                }
            )
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
