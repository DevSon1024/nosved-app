package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    // State for showing pickers
    var showVideoPopup by remember { mutableStateOf(false) }
    var showAudioPopup by remember { mutableStateOf(false) }

    val defaultVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val defaultAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")
    val downloadMode by qualityPrefs.downloadMode.collectAsState(initial = DownloadMode.VIDEO_AUDIO)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingsCategory(title = "Download Settings") }

            item {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Download Location",
                    subtitle = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/nosved",
                    onClick = { openDownloadFolder(context) }
                )
            }

            // Default Video Quality
            item {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Default Video Quality",
                    subtitle = defaultVideoQuality,
                    onClick = { showVideoPopup = true }
                )
            }

            // Default Audio Quality
            item {
                SettingsItem(
                    icon = Icons.Default.AudioFile,
                    title = "Default Audio Quality",
                    subtitle = defaultAudioQuality,
                    onClick = { showAudioPopup = true }
                )
            }

            // Download mode (video+audio or audio only)
            item {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Default Download Mode",
                    subtitle = if (downloadMode == DownloadMode.AUDIO_ONLY) "Audio Only" else "Video + Audio",
                    onClick = {
                        scope.launch {
                            val nextMode = if (downloadMode == DownloadMode.AUDIO_ONLY) DownloadMode.VIDEO_AUDIO else DownloadMode.AUDIO_ONLY
                            qualityPrefs.setDownloadMode(nextMode)
                        }
                    }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsCategory(title = "Storage") }

            item {
                val storageInfo = getStorageInfo(context)
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = "Used: ${storageInfo.first} • Available: ${storageInfo.second}",
                    onClick = { /* TODO */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Cache",
                    subtitle = "Remove temporary files",
                    onClick = { clearAppCache(context) }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsCategory(title = "About") }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Source Code",
                    subtitle = "View on GitHub",
                    onClick = { openGitHub(context) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Report Issue",
                    subtitle = "Found a bug? Let us know",
                    onClick = { openIssueTracker(context) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    subtitle = "Tell your friends about Nosved",
                    onClick = { shareApp(context) }
                )
            }
        }
    }

    // Video Quality Picker Dialog
    if (showVideoPopup) {
        AlertDialog(
            onDismissRequest = { showVideoPopup = false },
            title = { Text("Choose Default Video Quality") },
            text = {
                Column {
                    QualityConstants.VIDEO_MP4_QUALITIES.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    scope.launch { qualityPrefs.setVideoQuality(option.value) }
                                    showVideoPopup = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultVideoQuality == option.value,
                                onClick = {
                                    scope.launch { qualityPrefs.setVideoQuality(option.value) }
                                    showVideoPopup = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVideoPopup = false }) { Text("Cancel") }
            }
        )
    }

    // Audio Quality Picker Dialog
    if (showAudioPopup) {
        AlertDialog(
            onDismissRequest = { showAudioPopup = false },
            title = { Text("Choose Default Audio Quality") },
            text = {
                Column {
                    QualityConstants.AUDIO_M4A_QUALITIES.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    scope.launch { qualityPrefs.setAudioQuality(option.value) }
                                    showAudioPopup = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultAudioQuality == option.value,
                                onClick = {
                                    scope.launch { qualityPrefs.setAudioQuality(option.value) }
                                    showAudioPopup = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAudioPopup = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openDownloadFolder(context: Context) {
    try {
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "nosved")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(downloadDir), "resource/folder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback - open Downloads folder
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)),
                "resource/folder"
            )
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Could not open folder
        }
    }
}

private fun getStorageInfo(context: Context): Pair<String, String> {
    return try {
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "nosved")
        val usedSpace = if (downloadDir.exists()) {
            downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else {
            0L
        }

        val availableSpace = Environment.getExternalStorageDirectory().freeSpace

        val usedMB = usedSpace / (1024 * 1024)
        val availableGB = availableSpace / (1024 * 1024 * 1024)

        "${usedMB}MB" to "${availableGB}GB"
    } catch (e: Exception) {
        "Unknown" to "Unknown"
    }
}

private fun clearAppCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        cacheDir.deleteRecursively()
    } catch (e: Exception) {
        // Handle error
    }
}

private fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app"))
    context.startActivity(intent)
}

private fun openIssueTracker(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app/issues"))
    context.startActivity(intent)
}

private fun shareApp(context: Context) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out Nosved - Video Downloader")
        putExtra(
            Intent.EXTRA_TEXT,
            "Download videos easily with Nosved! Get it from: [Your Play Store Link]"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Nosved"))
}