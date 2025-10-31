// app/src/main/java/com/devson/nosved/ui/screens/DownloadScreen.kt
package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.devson.nosved.MainViewModel
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.components.*
import com.devson.nosved.ui.model.DownloadAction
import com.devson.nosved.ui.model.DownloadCounts
import com.devson.nosved.ui.model.DownloadTabType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(DownloadTabType.ALL) }
    val context = LocalContext.current

    // Collect download states
    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
    val runningDownloads by viewModel.runningDownloads.collectAsState(initial = emptyList())
    val completedDownloads by viewModel.completedDownloads.collectAsState(initial = emptyList())
    val failedDownloads by viewModel.failedDownloads.collectAsState(initial = emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Calculate download counts
    val downloadCounts = remember(allDownloads, runningDownloads, completedDownloads, failedDownloads) {
        DownloadCounts(
            all = allDownloads.size,
            active = runningDownloads.size,
            completed = completedDownloads.size,
            failed = failedDownloads.size
        )
    }

    // Get current downloads based on selected tab
    val currentDownloads = remember(selectedTab, allDownloads, runningDownloads, completedDownloads, failedDownloads) {
        when (selectedTab) {
            DownloadTabType.ALL -> allDownloads
            DownloadTabType.ACTIVE -> runningDownloads
            DownloadTabType.COMPLETED -> completedDownloads
            DownloadTabType.FAILED -> failedDownloads
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        DownloadHeader(
            onSearchClick = { /* TODO: Implement search */ },
            onSortClick = { /* TODO: Implement sort */ }
        )

        // Tab Row
        DownloadTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            downloadCounts = downloadCounts
        )

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentDownloads.isEmpty()) {
                DownloadEmptyState(tabType = selectedTab)
            } else {
                DownloadList(
                    downloads = currentDownloads,
                    downloadProgress = downloadProgress,
                    onAction = { action ->
                        handleDownloadAction(action, viewModel, context)
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadList(
    downloads: List<DownloadEntity>,
    downloadProgress: Map<String, com.devson.nosved.data.DownloadProgress>,
    onAction: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(
            items = downloads,
            key = { it.id }
        ) { download ->
            DownloadItemCard(
                download = download,
                progress = downloadProgress[download.id],
                onAction = onAction
            )
        }
    }
}

private fun handleDownloadAction(
    action: DownloadAction,
    viewModel: MainViewModel,
    context: Context
) {
    when (action) {
        is DownloadAction.Play -> playVideo(context, action.filePath)
        is DownloadAction.Share -> shareVideo(context, action.filePath)
        is DownloadAction.Retry -> viewModel.retryDownload(action.downloadId)
        is DownloadAction.Cancel -> viewModel.cancelDownload(action.downloadId)
        is DownloadAction.Delete -> viewModel.deleteDownload(action.downloadId)
        is DownloadAction.RemoveFromApp -> viewModel.removeFromApp(action.downloadId)
        is DownloadAction.Redownload -> viewModel.redownloadVideo(action.downloadId, action.sameQuality)
    }
}

// Video playback and sharing utilities
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
        // Handle error - consider showing a toast or snackbar
        e.printStackTrace()
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
        // Handle error - consider showing a toast or snackbar
        e.printStackTrace()
    }
}
