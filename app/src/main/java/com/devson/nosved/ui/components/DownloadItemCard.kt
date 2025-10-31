// app/src/main/java/com/devson/nosved/ui/components/DownloadItemCard.kt
package com.devson.nosved.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.model.DownloadAction
import java.io.File

@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    progress: DownloadProgress?,
    onAction: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFileNotFoundDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (download.status == DownloadStatus.COMPLETED && !download.filePath.isNullOrEmpty()) {
                    val file = File(download.filePath!!)
                    if (file.exists()) {
                        onAction(DownloadAction.Play(download.filePath!!))
                    } else {
                        showFileNotFoundDialog = true
                    }
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail section
            DownloadThumbnail(
                thumbnail = download.thumbnail,
                duration = download.duration,
                status = download.status,
                progress = (progress?.progress ?: download.progress).toFloat(),
                onThumbnailClick = {
                    if (!download.filePath.isNullOrEmpty()) {
                        val file = File(download.filePath!!)
                        if (file.exists()) {
                            onAction(DownloadAction.Play(download.filePath!!))
                        } else {
                            showFileNotFoundDialog = true
                        }
                    }
                }
            )

            // Content section with improved spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main content
                DownloadItemContent(
                    download = download,
                    progress = progress,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Actions menu
                DownloadActions(
                    download = download,
                    onAction = onAction
                )
            }
        }
    }

    // Modern file not found dialog
    if (showFileNotFoundDialog) {
        FileNotFoundDialog(
            downloadTitle = download.title,
            onDismiss = { showFileNotFoundDialog = false },
            onDelete = {
                onAction(DownloadAction.RemoveFromApp(download.id))
                showFileNotFoundDialog = false
            },
            onRedownloadSame = {
                onAction(DownloadAction.Redownload(download.id, sameQuality = true))
                showFileNotFoundDialog = false
            },
            onRedownloadDifferent = {
                onAction(DownloadAction.Redownload(download.id, sameQuality = false))
                showFileNotFoundDialog = false
            }
        )
    }
}

@Composable
private fun FileNotFoundDialog(
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
                text = "File Not Found",
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
                    text = "The downloaded file for:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\"$downloadTitle\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "has been deleted from your device storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                // Primary action - Redownload with same quality
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

                // Secondary action - Redownload with different quality
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

                // Tertiary action - Remove from app
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