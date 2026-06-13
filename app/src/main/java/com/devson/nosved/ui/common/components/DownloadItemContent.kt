package com.devson.nosved.ui.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.model.humanSize

@Composable
fun DownloadItemContent(
    download: DownloadEntity,
    progress: DownloadProgress?,
    onAction: (com.devson.nosved.ui.model.DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Title
        Text(
            text = download.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Uploader and file info row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Uploader
            download.uploader?.let { uploader ->
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // File size
            if (download.fileSize > 0) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = humanSize(download.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Format badges
        FormatBadges(
            videoFormat = download.videoFormat,
            audioFormat = download.audioFormat
        )

        // Progress info for active/paused downloads
        if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
            Spacer(modifier = Modifier.height(8.dp))
            val currentProgress = progress?.progress ?: download.progress
            val speed = progress?.speed ?: ""
            val eta = progress?.eta ?: ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentProgress}%" + if (download.status == DownloadStatus.PAUSED) " (Paused)" else "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (download.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )

                if (download.status == DownloadStatus.DOWNLOADING) {
                    Text(
                        text = buildString {
                            append(speed)
                            if (eta.isNotEmpty()) {
                                append(" • ETA $eta")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action buttons for active, paused, or queued downloads
        if (download.status == DownloadStatus.DOWNLOADING ||
            download.status == DownloadStatus.PAUSED ||
            download.status == DownloadStatus.QUEUED) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.QUEUED) {
                    FilledTonalButton(
                        onClick = { onAction(com.devson.nosved.ui.model.DownloadAction.Pause(download.id)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pause", fontSize = 12.sp)
                    }
                } else if (download.status == DownloadStatus.PAUSED) {
                    Button(
                        onClick = { onAction(com.devson.nosved.ui.model.DownloadAction.Resume(download.id)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Resume", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onAction(com.devson.nosved.ui.model.DownloadAction.Cancel(download.id)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        }

        // Error message for failed downloads
        if (download.status == DownloadStatus.FAILED && !download.error.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorMessage(error = download.error)
        }
    }
}

@Composable
private fun FormatBadges(
    videoFormat: String?,
    audioFormat: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        videoFormat?.let { format ->
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        audioFormat?.let { format ->
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressInfo(
    progress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${progress.progress}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = buildString {
                append(progress.speed)
                if (progress.eta.isNotEmpty()) {
                    append(" • ETA ${progress.eta}")
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
