package com.devson.nosved.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionDialog(
    currentMode: DownloadMode,
    onModeChange: (DownloadMode) -> Unit,
    onAdvancedClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    // Collect states
    val currentVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val currentAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")
    val preferredVideoFormat by qualityPrefs.preferredVideoFormat.collectAsState(initial = "quality")
    val audioContainer by qualityPrefs.audioContainer.collectAsState(initial = "default")
    val convertAudioFormatEnabled by qualityPrefs.convertAudioFormatEnabled.collectAsState(initial = false)
    val convertAudioFormat by qualityPrefs.convertAudioFormat.collectAsState(initial = "mp3")
    val downloadSubtitles by qualityPrefs.downloadSubtitles.collectAsState(initial = false)

    val sharedPrefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    var downloadPlaylist by remember { mutableStateOf(sharedPrefs.getBoolean("download_playlist", true)) }
    var saveThumbnail by remember { mutableStateOf(sharedPrefs.getBoolean("save_thumbnail", true)) }

    // Dialog states
    var showVideoConfigDialog by remember { mutableStateOf(false) }
    var showAudioConfigDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }

    // Video Config states
    var tempVideoFormat by remember { mutableStateOf("quality") }
    var tempVideoQuality by remember { mutableStateOf("720p") }

    LaunchedEffect(showVideoConfigDialog) {
        if (showVideoConfigDialog) {
            tempVideoFormat = preferredVideoFormat
            tempVideoQuality = currentVideoQuality
        }
    }

    // Audio Config states
    var tempAudioFormat by remember { mutableStateOf("default") }
    var tempAudioQuality by remember { mutableStateOf("128kbps") }

    LaunchedEffect(showAudioConfigDialog) {
        if (showAudioConfigDialog) {
            tempAudioFormat = audioContainer
            tempAudioQuality = currentAudioQuality
        }
    }

    // Convert states
    var tempConvertMode by remember { mutableStateOf("unconverted") }

    LaunchedEffect(showConvertDialog) {
        if (showConvertDialog) {
            tempConvertMode = when {
                !convertAudioFormatEnabled -> "unconverted"
                convertAudioFormat == "m4a" -> "m4a"
                else -> "mp3"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // DoneAll Header Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Title and Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Configure before download",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust this download",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Download Type Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Download type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAudioSelected = currentMode == DownloadMode.AUDIO_ONLY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isAudioSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onModeChange(DownloadMode.AUDIO_ONLY) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isAudioSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Audio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isAudioSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )

                    val isVideoSelected = currentMode == DownloadMode.VIDEO_AUDIO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isVideoSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onModeChange(DownloadMode.VIDEO_AUDIO) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isVideoSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Video",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isVideoSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Format Selection Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Format selection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Format Preference Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Format preference",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentMode == DownloadMode.VIDEO_AUDIO) {
                        // VIDEO MODE: Video Format and Audio Format cards
                        val videoFormatText = if (preferredVideoFormat == "legacy") "MP4" else "WEBM"
                        val videoQualityText = when (currentVideoQuality) {
                            "best" -> "Best"
                            "worst" -> "Lowest"
                            else -> currentVideoQuality
                        }

                        val audioFormatText = when (audioContainer) {
                            "opus" -> "OPUS"
                            "m4a" -> "M4A"
                            else -> "Default"
                        }
                        val audioQualityText = when (currentAudioQuality) {
                            "unlimited" -> "Unlimited"
                            else -> currentAudioQuality
                        }
                        FormatPreferenceCard(
                            icon = Icons.Default.Audiotrack,
                            title = "Audio Format",
                            subtitle = "$audioFormatText ($audioQualityText)",
                            onClick = { showAudioConfigDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        FormatPreferenceCard(
                            icon = Icons.Default.HighQuality,
                            title = "Video Format",
                            subtitle = "$videoFormatText ($videoQualityText)",
                            onClick = { showVideoConfigDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // AUDIO MODE: Audio Format and Convert cards
                        val audioFormatText = when (audioContainer) {
                            "opus" -> "OPUS"
                            "m4a" -> "M4A"
                            else -> "Default"
                        }
                        val audioQualityText = when (currentAudioQuality) {
                            "unlimited" -> "Unlimited"
                            else -> currentAudioQuality
                        }

                        val convertText = when {
                            !convertAudioFormatEnabled -> "Unconverted"
                            convertAudioFormat == "m4a" -> "To M4A"
                            else -> "To MP3"
                        }

                        FormatPreferenceCard(
                            icon = Icons.Default.Audiotrack,
                            title = "Audio Format",
                            subtitle = "$audioFormatText ($audioQualityText)",
                            onClick = { showAudioConfigDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        FormatPreferenceCard(
                            icon = Icons.Default.Settings,
                            title = "Convert Audio",
                            subtitle = convertText,
                            onClick = { showConvertDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Additional settings Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Additional settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdditionalSettingChip(
                        label = "Download playlist",
                        selected = downloadPlaylist,
                        onClick = {
                            downloadPlaylist = !downloadPlaylist
                            sharedPrefs.edit().putBoolean("download_playlist", downloadPlaylist).apply()
                        }
                    )

                    AdditionalSettingChip(
                        label = "Download subtitles",
                        selected = downloadSubtitles,
                        onClick = {
                            scope.launch {
                                qualityPrefs.setDownloadSubtitles(!downloadSubtitles)
                            }
                        }
                    )

                    AdditionalSettingChip(
                        label = "Save thumbnail",
                        selected = saveThumbnail,
                        onClick = {
                            saveThumbnail = !saveThumbnail
                            sharedPrefs.edit().putBoolean("save_thumbnail", saveThumbnail).apply()
                        }
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(
                        enabled = true
                    ),
                    modifier = Modifier
                        .weight(1.0f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            qualityPrefs.setDownloadMode(currentMode)
                            if (onDownloadClick != null) {
                                onDownloadClick()
                            } else {
                                onDismiss()
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // --- Sub-Dialogs ---

    // Video Settings Dialog
    if (showVideoConfigDialog) {
        AlertDialog(
            onDismissRequest = { showVideoConfigDialog = false },
            title = { Text("Video Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Dropdown 1: Video Format
                    var formatMenuExpanded by remember { mutableStateOf(false) }
                    Column {
                        Text(
                            text = "Video Format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { formatMenuExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val label = if (tempVideoFormat == "legacy") "Legacy (MP4)" else "Quality (WEBM)"
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = formatMenuExpanded,
                                onDismissRequest = { formatMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Legacy (MP4)") },
                                    onClick = {
                                        tempVideoFormat = "legacy"
                                        formatMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Quality (WEBM)") },
                                    onClick = {
                                        tempVideoFormat = "quality"
                                        formatMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Dropdown 2: Video Quality
                    var qualityMenuExpanded by remember { mutableStateOf(false) }
                    val qualities = listOf(
                        "best" to "Best Quality",
                        "2160p" to "2160p (4K)",
                        "1440p" to "1440p (2K)",
                        "1080p" to "1080p (FHD)",
                        "720p" to "720p (HD)",
                        "480p" to "480p",
                        "360p" to "360p",
                        "240p" to "240p",
                        "144p" to "144p",
                        "worst" to "Lowest Quality"
                    )
                    Column {
                        Text(
                            text = "Video Quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { qualityMenuExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val qualityLabel = qualities.find { it.first == tempVideoQuality }?.second ?: tempVideoQuality
                                Text(qualityLabel, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = qualityMenuExpanded,
                                onDismissRequest = { qualityMenuExpanded = false }
                            ) {
                                qualities.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            tempVideoQuality = value
                                            qualityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            qualityPrefs.setPreferredVideoFormat(tempVideoFormat)
                            qualityPrefs.setVideoQuality(tempVideoQuality)
                        }
                        showVideoConfigDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Settings Dialog
    if (showAudioConfigDialog) {
        AlertDialog(
            onDismissRequest = { showAudioConfigDialog = false },
            title = { Text("Audio Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Dropdown 1: Audio Format
                    var formatMenuExpanded by remember { mutableStateOf(false) }
                    Column {
                        Text(
                            text = "Audio Format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { formatMenuExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val label = when (tempAudioFormat) {
                                    "opus" -> "OPUS"
                                    "m4a" -> "M4A"
                                    else -> "not specified (default)"
                                }
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = formatMenuExpanded,
                                onDismissRequest = { formatMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("not specified (default)") },
                                    onClick = {
                                        tempAudioFormat = "default"
                                        formatMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("OPUS") },
                                    onClick = {
                                        tempAudioFormat = "opus"
                                        formatMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("M4A") },
                                    onClick = {
                                        tempAudioFormat = "m4a"
                                        formatMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Dropdown 2: Audio Quality
                    var qualityMenuExpanded by remember { mutableStateOf(false) }
                    val audioQualities = listOf(
                        "unlimited" to "Unlimited",
                        "192kbps" to "192kbps",
                        "128kbps" to "128kbps",
                        "64kbps" to "64kbps",
                        "32kbps" to "32kbps"
                    )
                    Column {
                        Text(
                            text = "Audio Quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { qualityMenuExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val qualityLabel = audioQualities.find { it.first == tempAudioQuality }?.second ?: tempAudioQuality
                                Text(qualityLabel, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = qualityMenuExpanded,
                                onDismissRequest = { qualityMenuExpanded = false }
                            ) {
                                audioQualities.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            tempAudioQuality = value
                                            qualityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            qualityPrefs.setAudioContainer(tempAudioFormat)
                            qualityPrefs.setAudioQuality(tempAudioQuality)
                        }
                        showAudioConfigDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Convert Audio Dialog
    if (showConvertDialog) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = { Text("Audio Conversion", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    listOf(
                        "unconverted" to "Unconverted",
                        "mp3" to "Convert to MP3",
                        "m4a" to "Convert to M4A"
                    ).forEach { (mode, label) ->
                        val isSelected = tempConvertMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { tempConvertMode = mode }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { tempConvertMode = mode }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            when (tempConvertMode) {
                                "unconverted" -> {
                                    qualityPrefs.setConvertAudioFormatEnabled(false)
                                }
                                "mp3" -> {
                                    qualityPrefs.setConvertAudioFormatEnabled(true)
                                    qualityPrefs.setConvertAudioFormat("mp3")
                                }
                                "m4a" -> {
                                    qualityPrefs.setConvertAudioFormatEnabled(true)
                                    qualityPrefs.setConvertAudioFormat("m4a")
                                }
                            }
                        }
                        showConvertDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FormatPreferenceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdditionalSettingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}