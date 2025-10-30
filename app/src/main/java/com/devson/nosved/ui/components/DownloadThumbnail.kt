package com.devson.nosved.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.nosved.data.DownloadStatus

@Composable
fun DownloadThumbnail(
    thumbnail: String?,
    duration: String?,
    status: DownloadStatus,
    progress: Float,
    modifier: Modifier = Modifier,
    onThumbnailClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .clickable(enabled = status == DownloadStatus.COMPLETED) {
                onThumbnailClick()
            }
    ) {
        // Optimized thumbnail with caching
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnail)
                .memoryCacheKey(thumbnail)
                .diskCacheKey(thumbnail)
                .crossfade(300)
                .build(),
            contentDescription = "Video Thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Duration badge
        duration?.let {
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomEnd),
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = formatDuration(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Status and progress overlays
        when (status) {
            DownloadStatus.DOWNLOADING -> {
                DownloadProgressOverlay(
                    progress = progress,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Progress bar at bottom
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }

            DownloadStatus.FAILED -> {
                StatusOverlay(
                    icon = Icons.Default.Error,
                    iconColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            DownloadStatus.COMPLETED -> {
                // Play button overlay on hover/focus
                StatusOverlay(
                    icon = Icons.Default.PlayArrow,
                    iconColor = Color.White,
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun DownloadProgressOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.size(44.dp),
            color = Color.White,
            strokeWidth = 3.dp,
            trackColor = Color.White.copy(alpha = 0.3f)
        )

        Text(
            text = "${progress.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun StatusOverlay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.7f)
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Utility function for formatting duration - moved here to avoid import issues
private fun formatDuration(durationString: String?): String {
    val duration = durationString?.toIntOrNull()
    if (duration == null) return "Unknown"

    val hours = duration / 3600
    val minutes = (duration % 3600) / 60
    val seconds = duration % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}
