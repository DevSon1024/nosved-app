package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onNavigateToCredits: () -> Unit,
    onNavigateToAppearanceSettings: () -> Unit,
    onNavigateToDirectorySettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var storageInfo by remember { mutableStateOf("Calculating..." to "...") }

    LaunchedEffect(Unit) {
        storageInfo = withContext(Dispatchers.IO) {
            getStorageInfoOptimized(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            // General Section
            settingsSection("General") {
                item {
                    SettingsGroupCard {
                        SettingsItemRow(
                            icon = Icons.Default.Palette,
                            title = "Appearance",
                            subtitle = "Theme mode, colors, and navigation style",
                            onClick = onNavigateToAppearanceSettings
                        )
                    }
                }
            }

            // Download Settings Section
            settingsSection("Download Settings") {
                item {
                    SettingsGroupCard {
                        SettingsItemRow(
                            icon = Icons.Default.Folder,
                            title = "Directory Setup",
                            subtitle = "Configure download folders and output templates",
                            onClick = onNavigateToDirectorySettings
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
                            icon = Icons.Default.Tune,
                            title = "Format Settings",
                            subtitle = "Set default mode, formats, and quality",
                            onClick = onNavigateToQualitySettings
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
                            icon = Icons.Default.Settings,
                            title = "Advanced Settings",
                            subtitle = "Subtitles, SponsorBlock, and more",
                            onClick = onNavigateToAdvancedSettings
                        )
                    }
                }
            }

            // Storage Section
            settingsSection("Storage") {
                item {
                    SettingsGroupCard {
                        SettingsItemRow(
                            icon = Icons.Default.Storage,
                            title = "Storage Usage",
                            subtitle = "Used: ${storageInfo.first} • Available: ${storageInfo.second}",
                            onClick = { }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
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
            }

            // About Section
            settingsSection("About") {
                item {
                    SettingsGroupCard {
                        SettingsItemRow(
                            icon = Icons.Default.Info,
                            title = "App Version",
                            subtitle = "Version info and updates",
                            onClick = onNavigateToAppVersion
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
                            icon = Icons.Default.Favorite,
                            title = "Credits",
                            subtitle = "Acknowledgements and licenses",
                            onClick = onNavigateToCredits
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
                            icon = Icons.Default.Code,
                            title = "Official Repo",
                            subtitle = "View on GitHub",
                            onClick = { openGitHub(context) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItemRow(
                            icon = Icons.Default.BugReport,
                            title = "Report Issue",
                            subtitle = "Found a bug? Let us know",
                            onClick = { openIssueTracker(context) }
                        )
                    }
                }
            }
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
        )
    }
    content()
    item {
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            content = content
        )
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
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
            modifier = Modifier.size(20.dp)
        )
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
        icon = {
            Icon(
                Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Download Folder",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current location:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = downloadFolder.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Choose Folder")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetFolder,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                    OutlinedButton(
                        onClick = onOpenFolder,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
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