package com.devson.nosved.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadProgress
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.model.DownloadAction

@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    progress: DownloadProgress?,
    onAction: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                if (download.status == DownloadStatus.COMPLETED && !download.filePath.isNullOrEmpty()) {
                    onAction(DownloadAction.Play(download.filePath!!))
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Thumbnail section
            DownloadThumbnail(
                thumbnail = download.thumbnail,
                duration = download.duration,
                status = download.status,
                progress = (progress?.progress ?: download.progress).toFloat(),
                onThumbnailClick = {
                    if (!download.filePath.isNullOrEmpty()) {
                        onAction(DownloadAction.Play(download.filePath!!))
                    }
                }
            )

            // Content section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Main content
                DownloadItemContent(
                    download = download,
                    progress = progress,
                    modifier = Modifier.weight(1f)
                )

                // Actions menu
                DownloadActions(
                    download = download,
                    onAction = onAction
                )
            }
        }
    }
}
