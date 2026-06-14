package com.devson.nosved.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.devson.nosved.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectorySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    val videoFolder by viewModel.videoDownloadFolder.collectAsState()
    val audioFolder by viewModel.audioDownloadFolder.collectAsState()
    val saveToSubdirWebsite by viewModel.saveToSubdirectoryWebsite.collectAsState()
    val saveToSubdirPlaylist by viewModel.saveToSubdirectoryPlaylist.collectAsState()
    val outputTemplate by viewModel.outputTemplate.collectAsState()
    val restrictFilenames by viewModel.restrictFilenames.collectAsState()

    var showVideoFolderDialog by remember { mutableStateOf(false) }
    var showAudioFolderDialog by remember { mutableStateOf(false) }
    var showSubdirDialog by remember { mutableStateOf(false) }
    var showOutputTemplateDialog by remember { mutableStateOf(false) }
    var showClearTempDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Directory Setup", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
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
                top = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folders Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    DirectorySettingCard(
                        title = "Video Folder",
                        subtitle = videoFolder,
                        onClick = { showVideoFolderDialog = true }
                    )
                    DirectorySettingCard(
                        title = "Audio Folder",
                        subtitle = audioFolder,
                        onClick = { showAudioFolderDialog = true }
                    )
                }
            }

            // Organization Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Organization & Naming",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    val websiteStr = if (saveToSubdirWebsite) "Website" else ""
                    val playlistStr = if (saveToSubdirPlaylist) "Playlist title" else ""
                    val subdirSubtitle = when {
                        saveToSubdirWebsite && saveToSubdirPlaylist -> "Website, Playlist title"
                        saveToSubdirWebsite -> "Website"
                        saveToSubdirPlaylist -> "Playlist title"
                        else -> "None"
                    }
                    DirectorySettingCard(
                        title = "Save to Subdirectory",
                        subtitle = subdirSubtitle,
                        onClick = { showSubdirDialog = true }
                    )
                    DirectorySettingCard(
                        title = "Output Template",
                        subtitle = outputTemplate,
                        onClick = { showOutputTemplateDialog = true }
                    )
                    AdvancedSettingCard(
                        title = "Restrict Filenames",
                        subtitle = "Limit filenames to ASCII characters (removes emojis/spaces)",
                        checked = restrictFilenames,
                        onToggle = { viewModel.setRestrictFilenames(it) }
                    )
                }
            }

            // Maintenance Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Maintenance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    DirectorySettingCard(
                        title = "Clear temporary Files",
                        subtitle = "Remove partial downloads to free up space",
                        onClick = { showClearTempDialog = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showVideoFolderDialog) {
        CustomDirectoryDialog(
            title = "Video Folder",
            description = "Specify the output directory for downloaded videos",
            currentPath = videoFolder,
            onDismiss = { showVideoFolderDialog = false },
            onConfirm = { path ->
                viewModel.setVideoDownloadFolder(path)
                showVideoFolderDialog = false
            }
        )
    }

    if (showAudioFolderDialog) {
        CustomDirectoryDialog(
            title = "Audio Folder",
            description = "Specify the output directory for downloaded audios",
            currentPath = audioFolder,
            onDismiss = { showAudioFolderDialog = false },
            onConfirm = { path ->
                viewModel.setAudioDownloadFolder(path)
                showAudioFolderDialog = false
            }
        )
    }

    if (showSubdirDialog) {
        SubdirectorySelectionDialog(
            initialWebsite = saveToSubdirWebsite,
            initialPlaylist = saveToSubdirPlaylist,
            onDismiss = { showSubdirDialog = false },
            onConfirm = { website, playlist ->
                viewModel.setSaveToSubdirectoryWebsite(website)
                viewModel.setSaveToSubdirectoryPlaylist(playlist)
                showSubdirDialog = false
            }
        )
    }

    if (showOutputTemplateDialog) {
        OutputTemplateSelectionDialog(
            currentTemplate = outputTemplate,
            onDismiss = { showOutputTemplateDialog = false },
            onConfirm = { template ->
                viewModel.setOutputTemplate(template)
                showOutputTemplateDialog = false
            }
        )
    }

    if (showClearTempDialog) {
        AlertDialog(
            onDismissRequest = { showClearTempDialog = false },
            title = {
                Text(
                    text = "Clear Temporary Files",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Temporary Files can be used to resume cancelled/paused downloads. Are You Sure to delete all of these files?\n\nYou can access these files in storage/emulated/0/Download/Nosved/temp/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val tempDir = File("/storage/emulated/0/Download/Nosved/temp")
                            val deleted = if (tempDir.exists()) {
                                tempDir.deleteRecursively()
                            } else {
                                true
                            }
                            val created = File("/storage/emulated/0/Download/Nosved/temp").mkdirs()
                            if (deleted && created) {
                                Toast.makeText(context, "Temporary files cleared successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to clear temporary files", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showClearTempDialog = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTempDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DirectorySettingCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
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
}

@Composable
private fun AdvancedSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onToggle(!checked) } else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CustomDirectoryDialog(
    title: String,
    description: String,
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pathText by remember { mutableStateOf(currentPath) }
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val resolvedPath = getPathFromUri(context, it)
            if (resolvedPath != null) {
                pathText = resolvedPath
            } else {
                pathText = it.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = pathText,
                    onValueChange = { pathText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Directory Path") },
                    leadingIcon = {
                        Text(
                            text = "-P",
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Folder picker", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/yt-dlp/yt-dlp#output-template"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Usage reference", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pathText) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SubdirectorySelectionDialog(
    initialWebsite: Boolean,
    initialPlaylist: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit
) {
    var websiteChecked by remember { mutableStateOf(initialWebsite) }
    var playlistChecked by remember { mutableStateOf(initialPlaylist) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save to subdirectory",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Save files in folders named as respective fields",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Website choice Card
                Surface(
                    onClick = { websiteChecked = !websiteChecked },
                    shape = RoundedCornerShape(12.dp),
                    color = if (websiteChecked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (websiteChecked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Website",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (websiteChecked) FontWeight.Bold else FontWeight.Normal,
                            color = if (websiteChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Checkbox(
                            checked = websiteChecked,
                            onCheckedChange = { websiteChecked = it }
                        )
                    }
                }

                // Playlist title choice Card
                Surface(
                    onClick = { playlistChecked = !playlistChecked },
                    shape = RoundedCornerShape(12.dp),
                    color = if (playlistChecked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (playlistChecked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Playlist title",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (playlistChecked) FontWeight.Bold else FontWeight.Normal,
                            color = if (playlistChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Checkbox(
                            checked = playlistChecked,
                            onCheckedChange = { playlistChecked = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Your downloads will be saved as:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val examplePath = when {
                        websiteChecked && playlistChecked -> ".../website/playlist_title/file_name"
                        websiteChecked -> ".../website/file_name"
                        playlistChecked -> ".../playlist_title/file_name"
                        else -> ".../file_name"
                    }
                    Text(
                        text = examplePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(websiteChecked, playlistChecked) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun OutputTemplateSelectionDialog(
    currentTemplate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val option1 = "%(title).200B.%(ext)s"
    val option2 = "%(title).200B [%(id)s].%(ext)s"

    var selectedOption by remember {
        mutableStateOf(
            when (currentTemplate) {
                option1 -> 1
                option2 -> 2
                else -> 3
            }
        )
    }

    var customText by remember {
        mutableStateOf(
            if (selectedOption == 3) currentTemplate else option2
        )
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Output template",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Specify the template for output file names",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Option 1
                Surface(
                    onClick = { selectedOption = 1 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedOption == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (selectedOption == 1) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option1,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedOption == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedOption == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = selectedOption == 1,
                            onClick = { selectedOption = 1 }
                        )
                    }
                }

                // Option 2
                Surface(
                    onClick = { selectedOption = 2 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedOption == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (selectedOption == 2) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option2,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedOption == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedOption == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = selectedOption == 2,
                            onClick = { selectedOption = 2 }
                        )
                    }
                }

                // Option 3 (Custom)
                Surface(
                    onClick = { selectedOption = 3 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedOption == 3) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (selectedOption == 3) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedOption == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedOption == 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = selectedOption == 3,
                            onClick = { selectedOption = 3 }
                        )
                    }
                }

                if (selectedOption == 3) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(option2) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "Required: %(title).200B, .%(ext)s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/yt-dlp/yt-dlp#output-template"))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Yt-dlp usage references",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTemplate = when (selectedOption) {
                        1 -> option1
                        2 -> option2
                        else -> {
                            if (customText.isBlank()) option2 else customText
                        }
                    }
                    onConfirm(finalTemplate)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun getPathFromUri(context: Context, uri: Uri): String? {
    try {
        if (android.provider.DocumentsContract.isTreeUri(uri)) {
            val treeId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val split = treeId.split(":")
            if (split.isNotEmpty()) {
                val type = split[0]
                val path = if (split.size > 1) split[1] else ""
                return if ("primary".equals(type, ignoreCase = true)) {
                    "${Environment.getExternalStorageDirectory()}/$path"
                } else {
                    "/storage/$type/$path"
                }
            }
        }
    } catch (e: Exception) {
        // Fallback to URI path
    }
    return null
}


