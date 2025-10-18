package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.devson.nosved.MainViewModel
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
    val runningDownloads by viewModel.runningDownloads.collectAsState(initial = emptyList())
    val completedDownloads by viewModel.completedDownloads.collectAsState(initial = emptyList())
    val failedDownloads by viewModel.failedDownloads.collectAsState(initial = emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { viewModel.clearCompletedDownloads() }
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Completed")
            }
        }

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All (${allDownloads.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Running (${runningDownloads.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Completed (${completedDownloads.size})") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Failed (${failedDownloads.size})") }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> DownloadList(
                downloads = allDownloads,
                downloadProgress = downloadProgress,
                onCancelDownload = { viewModel.cancelDownload(it) },
                onRetryDownload = { viewModel.retryDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onPlayVideo = { playVideo(context, it) },
                onShareVideo = { shareVideo(context, it) }
            )
            1 -> DownloadList(
                downloads = runningDownloads,
                downloadProgress = downloadProgress,
                onCancelDownload = { viewModel.cancelDownload(it) },
                onRetryDownload = { viewModel.retryDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onPlayVideo = { playVideo(context, it) },
                onShareVideo = { shareVideo(context, it) }
            )
            2 -> DownloadList(
                downloads = completedDownloads,
                downloadProgress = downloadProgress,
                onCancelDownload = { viewModel.cancelDownload(it) },
                onRetryDownload = { viewModel.retryDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onPlayVideo = { playVideo(context, it) },
                onShareVideo = { shareVideo(context, it) }
            )
            3 -> DownloadList(
                downloads = failedDownloads,
                downloadProgress = downloadProgress,
                onCancelDownload = { viewModel.cancelDownload(it) },
                onRetryDownload = { viewModel.retryDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onPlayVideo = { playVideo(context, it) },
                onShareVideo = { shareVideo(context, it) }
            )
        }
    }
}

@Composable
fun DownloadList(
    downloads: List<DownloadEntity>,
    downloadProgress: Map<String, com.devson.nosved.data.DownloadProgress>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onShareVideo: (String) -> Unit
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No downloads found",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(downloads) { download ->
                DownloadItem(
                    download = download,
                    progress = downloadProgress[download.id],
                    onCancelDownload = onCancelDownload,
                    onRetryDownload = onRetryDownload,
                    onDeleteDownload = onDeleteDownload,
                    onPlayVideo = onPlayVideo,
                    onShareVideo = onShareVideo
                )
            }
        }
    }
}

@Composable
fun DownloadItem(
    download: DownloadEntity,
    progress: com.devson.nosved.data.DownloadProgress?,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onShareVideo: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail
                AsyncImage(
                    model = download.thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row {
                        download.videoFormat?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        download.audioFormat?.let {
                            Text(
                                text = " • $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    download.uploader?.let {
                        Text(
                            text = "by $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status indicator
                StatusIndicator(download.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar (only for downloading)
            if (download.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { (progress?.progress ?: download.progress) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progress?.progress ?: download.progress}%",
                        style = MaterialTheme.typography.bodySmall
                    )

                    progress?.let {
                        Text(
                            text = "${it.speed} • ETA ${it.eta}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error message (only for failed)
            if (download.status == DownloadStatus.FAILED && !download.error.isNullOrEmpty()) {
                Text(
                    text = "Error: ${download.error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = { onCancelDownload(download.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        if (!download.filePath.isNullOrEmpty() && File(download.filePath).exists()) {
                            OutlinedButton(
                                onClick = { onPlayVideo(download.filePath) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play")
                            }

                            OutlinedButton(
                                onClick = { onShareVideo(download.filePath) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }

                    DownloadStatus.FAILED -> {
                        OutlinedButton(
                            onClick = { onRetryDownload(download.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }

                    else -> {}
                }

                // Delete button (always available)
                OutlinedButton(
                    onClick = { onDeleteDownload(download.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: DownloadStatus) {
    val (color, icon) = when (status) {
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Schedule
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary to Icons.Default.Download
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Pause
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error to Icons.Default.Error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Cancel
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

private fun playVideo(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Play Video"))
        }
    } catch (e: Exception) {
        // Handle error - maybe show a toast
    }
}

private fun shareVideo(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Video"))
        }
    } catch (e: Exception) {
        // Handle error - maybe show a toast
    }
}
