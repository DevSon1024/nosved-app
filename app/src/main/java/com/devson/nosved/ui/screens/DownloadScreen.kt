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
        // Tab Row with improved spacing and better layout
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "All (${allDownloads.size})",
                        maxLines = 1
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Active (${runningDownloads.size})",
                        maxLines = 1
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "Completed (${completedDownloads.size})",
                        maxLines = 1
                    )
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Text(
                        text = "Failed (${failedDownloads.size})",
                        maxLines = 1
                    )
                }
            )
        }

        // Content based on selected tab with consistent padding
        Box(modifier = Modifier.fillMaxSize()) {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No downloads found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Your download history will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail with better styling
                Card(
                    modifier = Modifier.size(64.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    AsyncImage(
                        model = download.thumbnail,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and details with improved layout
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Format info with better spacing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        download.videoFormat?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(0.dp)
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        download.audioFormat?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    download.uploader?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "by $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status indicator with better positioning
                StatusIndicator(download.status)
            }

            // Progress section with improved styling
            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { (progress?.progress ?: download.progress) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progress?.progress ?: download.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    progress?.let {
                        Text(
                            text = "${it.speed}${if (it.eta.isNotEmpty()) " • ETA ${it.eta}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error message with better styling
            if (download.status == DownloadStatus.FAILED && !download.error.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: ${download.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons with improved layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = { onCancelDownload(download.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play")
                            }

                            OutlinedButton(
                                onClick = { onShareVideo(download.filePath) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
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
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }

                    DownloadStatus.QUEUED -> {
                        OutlinedButton(
                            onClick = { onCancelDownload(download.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Queued")
                        }
                    }

                    else -> {
                        // For cancelled or other states, just show spacer
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Delete button (always available) with consistent sizing
                OutlinedButton(
                    onClick = { onDeleteDownload(download.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(width = 56.dp, height = 40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
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

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.name,
            tint = color,
            modifier = Modifier
                .size(28.dp)
                .padding(4.dp)
        )
    }
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
