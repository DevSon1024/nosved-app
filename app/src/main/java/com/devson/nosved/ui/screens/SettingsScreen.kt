package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

sealed class DialogState {
    object None : DialogState()
    object DownloadLocation : DialogState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQualitySettings: () -> Unit,
    onNavigateToAdvancedSettings: () -> Unit,
    onNavigateToAppVersion: () -> Unit,
    onNavigateToCredits: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    var storageInfo by remember { mutableStateOf("Calculating..." to "...") }

    LaunchedEffect(Unit) {
        storageInfo = withContext(Dispatchers.IO) {
            getStorageInfoOptimized(context)
        }
    }

    val downloadFolder = remember { getCurrentDownloadFolder(context) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            showToast(context, "Download folder updated!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Download Settings Section
            settingsSection("Download Settings") {
                item {
                    SettingsItem(
                        icon = Icons.Default.Folder,
                        title = "Download Location",
                        subtitle = downloadFolder.substringAfterLast("/"),
                        onClick = { dialogState = DialogState.DownloadLocation }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Tune,
                        title = "Format Settings",
                        subtitle = "Set default mode, formats, and quality",
                        onClick = onNavigateToQualitySettings
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Advanced Settings",
                        subtitle = "Subtitles, SponsorBlock, and more",
                        onClick = onNavigateToAdvancedSettings
                    )
                }
            }

            // Storage Section
            settingsSection("Storage") {
                item {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Storage Usage",
                        subtitle = "Used: ${storageInfo.first} • Available: ${storageInfo.second}",
                        onClick = { }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Cache",
                        subtitle = "Remove temporary files",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    clearAppCache(context)
                                }
                                storageInfo = withContext(Dispatchers.IO) {
                                    getStorageInfoOptimized(context)
                                }
                                showToast(context, "Cache cleared!")
                            }
                        }
                    )
                }
            }

            // About Section - Updated with App Version navigation
            settingsSection("About") {
                item {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "Version info and updates",
                        onClick = onNavigateToAppVersion // Navigate to new App Version screen
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Favorite, // or Icons.Default.Stars
                        title = "Credits",
                        subtitle = "Acknowledgements and licenses",
                        onClick = onNavigateToCredits // Add this new navigation
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Official Repo",
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
            }
        }
    }

    // Handle dialogs
    when (val currentDialogState = dialogState) {
        is DialogState.DownloadLocation -> {
            DownloadFolderDialog(
                downloadFolder = downloadFolder,
                onDismiss = { dialogState = DialogState.None },
                onChooseFolder = {
                    dialogState = DialogState.None
                    folderPickerLauncher.launch(null)
                },
                onResetFolder = {
                    dialogState = DialogState.None
                    showToast(context, "Reset to default folder")
                },
                onOpenFolder = {
                    dialogState = DialogState.None
                    openDownloadFolderInFileManager(context, downloadFolder)
                }
            )
        }
        is DialogState.None -> {
            // No dialog to show
        }
    }
}
fun LazyListScope.settingsSection(
    title: String,
    content: LazyListScope.() -> Unit
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }
    content()
    item {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DownloadFolderDialog(
    downloadFolder: String,
    onDismiss: () -> Unit,
    onChooseFolder: () -> Unit,
    onResetFolder: () -> Unit,
    onOpenFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Folder") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        downloadFolder.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Folder")
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onResetFolder) { Text("Reset") }
                    TextButton(onClick = onOpenFolder) { Text("Open") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// Helper functions remain the same...
fun showToast(context: Context, msg: String) {
    android.widget.Toast.makeText(context.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
}

fun getCurrentDownloadFolder(context: Context): String {
    return "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/nosved"
}

suspend fun getStorageInfoOptimized(context: Context): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(getCurrentDownloadFolder(context))
            val usedSpace = if (downloadDir.exists()) {
                downloadDir.walkTopDown()
                    .filter { it.isFile }
                    .map { it.length() }
                    .sum()
            } else 0L

            val availableSpace = Environment.getExternalStorageDirectory().freeSpace
            val df = DecimalFormat("#.#")

            val usedMB = df.format(usedSpace / (1024.0 * 1024.0))
            val availableGB = df.format(availableSpace / (1024.0 * 1024.0 * 1024.0))

            "${usedMB}MB" to "${availableGB}GB"
        } catch (e: Exception) {
            "Error" to "Error"
        }
    }
}

fun clearAppCache(context: Context) {
    try {
        context.cacheDir.deleteRecursively()
        File(context.cacheDir, "videos").takeIf { it.exists() }?.deleteRecursively()
    } catch (_: Exception) { }
}

fun openDownloadFolderInFileManager(context: Context, folderPath: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(File(folderPath)), "resource/folder")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open folder"))
    } catch (e: Exception) {
        showToast(context, "Unable to open folder")
    }
}

fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app"))
    context.startActivity(intent)
}

fun openIssueTracker(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app/issues"))
    context.startActivity(intent)
}