package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import java.text.DecimalFormat
import androidx.compose.material.icons.filled.FolderOpen



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qualityPrefs = remember { QualityPreferences(context) }

    var showVideoPopup by remember { mutableStateOf(false) }
    var showAudioPopup by remember { mutableStateOf(false) }
    var showDownloadLocationMenu by remember { mutableStateOf(false) }

    // In a real app, download folder should be stored in DataStore as a preference
    // For demo, we'll use default folder logic
    var downloadFolder by remember { mutableStateOf(getCurrentDownloadFolder(context)) }

    // File picker for setting download folder (saf/intent emulation)
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            downloadFolder = it.toString()
            // Persist and update your DataStore here
            showToast(context, "Download folder set!")
        }
    }

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

            // Download Location with click action
            item {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Download Location",
                    subtitle = downloadFolder,
                    onClick = { showDownloadLocationMenu = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Default Video Quality",
                    subtitle = defaultVideoQuality,
                    onClick = { showVideoPopup = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.AudioFile,
                    title = "Default Audio Quality",
                    subtitle = defaultAudioQuality,
                    onClick = { showAudioPopup = true }
                )
            }
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

            // Storage Usage with double formatting
            item {
                val storageInfo = getStorageInfoDouble(context)
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = "Used: ${storageInfo.first} • Available: ${storageInfo.second}",
                    onClick = { }
                )
            }

            // Clear cache
            item {
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Cache",
                    subtitle = "Remove temporary files",
                    onClick = {
                        scope.launch {
                            clearAppCache(context)
                            showToast(context, "Cache cleared!")
                        }
                    }
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

    // ======= POPUP MENUS / DIALOGS ========

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
            dismissButton = { TextButton(onClick = { showVideoPopup = false }) { Text("Cancel") } }
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
            dismissButton = { TextButton(onClick = { showAudioPopup = false }) { Text("Cancel") } }
        )
    }

    // Download Location Popup Menu
    if (showDownloadLocationMenu) {
        DownloadFolderDialog(
            showDialog = showDownloadLocationMenu,
            downloadFolder = downloadFolder,
            onDismiss = { showDownloadLocationMenu = false },
            onChooseFolder = {
                showDownloadLocationMenu = false
                folderPickerLauncher.launch(null)
            },
            onResetFolder = {
                downloadFolder = getDefaultDownloadFolderPath()
                // Persist to DataStore if needed
                showDownloadLocationMenu = false
                showToast(context, "Reset to default folder")
            },
            onOpenFolder = {
                showDownloadLocationMenu = false
                openDownloadFolderInFileManager(context, downloadFolder)
            }
        )

    }

}
@Composable
fun DownloadFolderDialog(
    showDialog: Boolean,
    downloadFolder: String,
    onDismiss: () -> Unit,
    onChooseFolder: () -> Unit,
    onResetFolder: () -> Unit,
    onOpenFolder: () -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Download Folder", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                // Present folder path with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Current folder",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        downloadFolder,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onChooseFolder
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Folder")
                }

                Divider(Modifier.padding(vertical = 16.dp))

                // Actions row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onResetFolder) {
                        Text("Reset to Default")
                    }
                    TextButton(onClick = onOpenFolder) {
                        Text("Open Folder")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
fun openDownloadFolderInFileManager(context: Context, folderPath: String) {
    try {
        val file = File(folderPath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(file), "resource/folder")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Use chooser to ensure file manager usage on newer Android
        context.startActivity(Intent.createChooser(intent, "Open folder"))
    } catch (e: Exception) {
        showToast(context, "Unable to open folder: $e")
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
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper to show toast
fun showToast(context: Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

// Get default download folder path
fun getDefaultDownloadFolderPath(): String {
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/nosved"
}

// Return folder from DataStore if set, fallback to default
fun getCurrentDownloadFolder(context: Context): String {
    // In demo, just use default. For DataStore/Prefs use persistent value.
    return getDefaultDownloadFolderPath()
}

// Open Download Folder
fun openDownloadFolder(context: Context, folderPath: String) {
    try {
        val file = File(folderPath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(file), "resource/folder")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        showToast(context, "Unable to open folder")
    }
}

// Storage info with DOUBLE formatting
fun getStorageInfoDouble(context: Context): Pair<String, String> {
    return try {
        val downloadDir = File(getCurrentDownloadFolder(context))
        val usedSpace = if (downloadDir.exists()) {
            downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else {
            0L
        }
        val availableSpace = Environment.getExternalStorageDirectory().freeSpace
        val df = DecimalFormat("#.##")
        val usedMB = df.format(usedSpace / (1024.0 * 1024.0))
        val availableGB = df.format(availableSpace / (1024.0 * 1024.0 * 1024.0))
        "${usedMB}MB" to "${availableGB}GB"
    } catch (e: Exception) {
        "Unknown" to "Unknown"
    }
}

// Clear cache function
fun clearAppCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        cacheDir.deleteRecursively()
        val videoCacheDir = File(context.cacheDir, "videos") // If you have video cache in cacheDir/videos
        if (videoCacheDir.exists()) {
            videoCacheDir.deleteRecursively()
        }
    } catch (_: Exception) { }
}

fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app"))
    context.startActivity(intent)
}
fun openIssueTracker(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app/issues"))
    context.startActivity(intent)
}
fun shareApp(context: Context) {
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