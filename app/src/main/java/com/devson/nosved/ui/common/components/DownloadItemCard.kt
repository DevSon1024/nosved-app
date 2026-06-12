package com.devson.nosved.ui.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.model.DownloadAction
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    progress: DownloadProgress?,
    onAction: (DownloadAction) -> Unit,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onShowBottomSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    val isPlayable = download.status == DownloadStatus.COMPLETED && !download.filePath.isNullOrEmpty()
    val isFailedOrCancelled = download.status == DownloadStatus.FAILED || download.status == DownloadStatus.CANCELLED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = {
                        if (isInSelectionMode) {
                            onToggleSelection()
                        } else {
                            if (isPlayable) {
                                val file = File(download.filePath!!)
                                if (file.exists()) {
                                    onAction(DownloadAction.Play(download.filePath!!))
                                } else {
                                    dialogTitle = "File Not Found"
                                    dialogMessage = "The downloaded file has been deleted or moved from your device storage."
                                    showOptionsDialog = true
                                }
                            } else if (isFailedOrCancelled) {
                                dialogTitle = if (download.status == DownloadStatus.FAILED) "Download Failed" else "Download Cancelled"
                                dialogMessage = if (download.status == DownloadStatus.FAILED) {
                                    "The download failed due to an error:\n${download.error ?: "Unknown error"}"
                                } else {
                                    "The download was cancelled before completion."
                                }
                                showOptionsDialog = true
                            }
                        }
                    },
                    onLongClick = {
                        onToggleSelection()
                    }
                )
        ) {
            // Thumbnail container with status badge overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                // Optimized thumbnail with caching
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(download.thumbnail)
                        .memoryCacheKey(download.thumbnail)
                        .diskCacheKey(download.thumbnail)
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
                download.duration?.let {
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
                val computedProgress = (progress?.progress ?: download.progress).toFloat()
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        DownloadProgressOverlay(
                            progress = computedProgress,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Progress bar at bottom
                        LinearProgressIndicator(
                            progress = { computedProgress / 100f },
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

                    else -> {}
                }

                // Status Badge Overlay on Top-Left
                StatusBadge(
                    status = download.status,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                )

                // Checkbox Overlay on Top-Right in Selection Mode
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = Color.White.copy(alpha = 0.8f),
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Content section with 3-dots aligned to top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DownloadItemContent(
                    download = download,
                    progress = progress,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (!isInSelectionMode) {
                    IconButton(
                        onClick = onShowBottomSheet,
                        modifier = Modifier
                            .align(Alignment.Top)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Download options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    if (showOptionsDialog) {
        DownloadActionDialog(
            title = dialogTitle,
            message = dialogMessage,
            downloadTitle = download.title,
            onDismiss = { showOptionsDialog = false },
            onDelete = {
                onAction(DownloadAction.RemoveFromApp(download.id))
                showOptionsDialog = false
            },
            onRedownloadSame = {
                onAction(DownloadAction.Redownload(download.id, sameQuality = true))
                showOptionsDialog = false
            },
            onRedownloadDifferent = {
                onAction(DownloadAction.Redownload(download.id, sameQuality = false))
                showOptionsDialog = false
            }
        )
    }
}

@Composable
private fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (status) {
        DownloadStatus.COMPLETED -> Triple(
            Color(0xFF2E7D32), // Green
            Color.White,
            "Completed"
        )
        DownloadStatus.FAILED -> Triple(
            Color(0xFFC62828), // Red
            Color.White,
            "Failed"
        )
        DownloadStatus.CANCELLED -> Triple(
            Color(0xFFC62828), // Dark Gray
            Color.White,
            "Cancelled"
        )
        DownloadStatus.DOWNLOADING -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "Downloading"
        )
        DownloadStatus.QUEUED -> Triple(
            Color(0xFF1565C0), // Blue
            Color.White,
            "Queued"
        )
        DownloadStatus.PAUSED -> Triple(
            Color(0xFFEF6C00), // Orange
            Color.White,
            "Paused"
        )
    }

    Surface(
        modifier = modifier,
        color = backgroundColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun DownloadActionDialog(
    title: String,
    message: String,
    downloadTitle: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRedownloadSame: () -> Unit,
    onRedownloadDifferent: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\"$downloadTitle\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRedownloadSame,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Redownload (Same Quality)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onRedownloadDifferent,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Choose Quality & Redownload",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Remove from App",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
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
    icon: ImageVector,
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