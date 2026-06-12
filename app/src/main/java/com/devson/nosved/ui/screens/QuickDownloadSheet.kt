package com.devson.nosved.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.common.components.FormatPreferenceCard
import com.devson.nosved.viewmodel.QuickDownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDownloadSheet(
    viewModel: QuickDownloadViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp, top = 4.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            //  Header icon 
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Quick Nosved",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Download without opening the app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            //  Animated state body 
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically { it / 6 })
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "quick_state"
            ) { s ->
                when (s) {
                    is QuickDownloadViewModel.State.Idle,
                    is QuickDownloadViewModel.State.Loading -> LoadingSection(
                        message = if (s is QuickDownloadViewModel.State.Loading) s.message else "Loading..."
                    )

                    is QuickDownloadViewModel.State.Queued -> LoadingSection(
                        message = "Download queued..."
                    )

                    is QuickDownloadViewModel.State.Ready -> ReadySection(
                        state = s,
                        onModeChange = { viewModel.setAudioMode(it) },
                        onVideoQualityChange = { viewModel.setVideoQuality(it) },
                        onAudioQualityChange = { viewModel.setAudioQuality(it) },
                        onVideoContainerChange = { viewModel.setVideoContainer(it) },
                        onAudioContainerChange = { viewModel.setAudioContainer(it) },
                        onDownload = { viewModel.startDownload() },
                        onDismiss = onDismiss
                    )

                    is QuickDownloadViewModel.State.Error -> ErrorSection(
                        message = s.message,
                        onRetry = { viewModel.retry() },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

//  Loading 

@Composable
private fun LoadingSection(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    // Three dots each offset by 120ms
    val offsets = listOf(0, 120, 240).map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at delay using FastOutSlowInEasing
                    -10f at delay + 220 using FastOutSlowInEasing
                    0f at delay + 440
                    0f at 900
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$delay"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            offsets.forEach { offset ->
                Box(
                    modifier = Modifier
                        .offset(y = offset.value.dp)
                        .size(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

//  Ready 

@Composable
private fun ReadySection(
    state: QuickDownloadViewModel.State.Ready,
    onModeChange: (Boolean) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onAudioQualityChange: (String) -> Unit,
    onVideoContainerChange: (String) -> Unit,
    onAudioContainerChange: (String) -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = state.videoInfo
    val isAudio = state.isAudio

    // Sub-dialog visibility
    var showVideoConfigDialog by remember { mutableStateOf(false) }
    var showAudioConfigDialog by remember { mutableStateOf(false) }

    // Temp states for video config dialog
    var tempVideoQuality by remember { mutableStateOf(state.videoQuality) }
    var tempVideoContainer by remember { mutableStateOf(state.videoContainer) }

    // Temp states for audio config dialog
    var tempAudioQuality by remember { mutableStateOf(state.audioQuality) }
    var tempAudioContainer by remember { mutableStateOf(state.audioContainer) }

    // Re-sync temps when dialog opens
    LaunchedEffect(showVideoConfigDialog) {
        if (showVideoConfigDialog) {
            tempVideoQuality = state.videoQuality
            tempVideoContainer = state.videoContainer
        }
    }
    LaunchedEffect(showAudioConfigDialog) {
        if (showAudioConfigDialog) {
            tempAudioQuality = state.audioQuality
            tempAudioContainer = state.audioContainer
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Video title surface
        info.title?.let { title ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        //  Download type section 
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
            SegmentedToggle(
                leftLabel = "Audio",
                rightLabel = "Video",
                leftSelected = isAudio,
                onLeftClick = { onModeChange(true) },
                onRightClick = { onModeChange(false) }
            )
        }

        //  Format preference section 
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
                // Audio card (always shown)
                val audioContainerLabel = when (state.audioContainer.lowercase()) {
                    "opus" -> "OPUS"
                    "m4a" -> "M4A"
                    "mp3" -> "MP3"
                    else -> "Default"
                }
                FormatPreferenceCard(
                    icon = Icons.Default.Audiotrack,
                    title = "Audio Format",
                    subtitle = "$audioContainerLabel (${state.audioQuality})",
                    onClick = { showAudioConfigDialog = true },
                    modifier = Modifier.weight(1f)
                )

                if (!isAudio) {
                    // Video card — only in video mode
                    val videoContainerLabel = when (state.videoContainer.lowercase()) {
                        "mp4" -> "MP4"
                        "webm" -> "WEBM"
                        "mkv" -> "MKV"
                        else -> state.videoContainer.uppercase()
                    }
                    FormatPreferenceCard(
                        icon = Icons.Default.HighQuality,
                        title = "Video Format",
                        subtitle = "$videoContainerLabel (${state.videoQuality})",
                        onClick = { showVideoConfigDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        //  Action buttons 
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
                modifier = Modifier
                    .weight(1f)
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onDownload,
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    //  Video Config AlertDialog 
    if (showVideoConfigDialog) {
        VideoConfigDialog(
            initialQuality = tempVideoQuality,
            initialContainer = tempVideoContainer,
            onApply = { quality, container ->
                onVideoQualityChange(quality)
                onVideoContainerChange(container)
                showVideoConfigDialog = false
            },
            onDismiss = { showVideoConfigDialog = false }
        )
    }

    //  Audio Config AlertDialog 
    if (showAudioConfigDialog) {
        AudioConfigDialog(
            initialQuality = tempAudioQuality,
            initialContainer = tempAudioContainer,
            onApply = { quality, container ->
                onAudioQualityChange(quality)
                onAudioContainerChange(container)
                showAudioConfigDialog = false
            },
            onDismiss = { showAudioConfigDialog = false }
        )
    }
}

//  Video Config Dialog 

@Composable
private fun VideoConfigDialog(
    initialQuality: String,
    initialContainer: String,
    onApply: (quality: String, container: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempQuality by remember { mutableStateOf(initialQuality) }
    var tempContainer by remember { mutableStateOf(initialContainer) }

    val videoQualities = listOf(
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
    val videoContainers = listOf(
        "mp4" to "MP4",
        "webm" to "WEBM",
        "mkv" to "MKV"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Container dropdown
                var containerExpanded by remember { mutableStateOf(false) }
                DropdownField(
                    label = "Video Container",
                    value = videoContainers.find { it.first == tempContainer }?.second ?: tempContainer.uppercase(),
                    expanded = containerExpanded,
                    onExpandChange = { containerExpanded = it },
                    items = videoContainers,
                    onSelect = { tempContainer = it; containerExpanded = false }
                )

                // Quality dropdown
                var qualityExpanded by remember { mutableStateOf(false) }
                DropdownField(
                    label = "Video Quality",
                    value = videoQualities.find { it.first == tempQuality }?.second ?: tempQuality,
                    expanded = qualityExpanded,
                    onExpandChange = { qualityExpanded = it },
                    items = videoQualities,
                    onSelect = { tempQuality = it; qualityExpanded = false }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApply(tempQuality, tempContainer) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

//  Audio Config Dialog 

@Composable
private fun AudioConfigDialog(
    initialQuality: String,
    initialContainer: String,
    onApply: (quality: String, container: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempQuality by remember { mutableStateOf(initialQuality) }
    var tempContainer by remember { mutableStateOf(initialContainer) }

    val audioQualities = listOf(
        "unlimited" to "Unlimited",
        "320kbps" to "320kbps",
        "256kbps" to "256kbps",
        "192kbps" to "192kbps",
        "128kbps" to "128kbps",
        "96kbps" to "96kbps",
        "64kbps" to "64kbps",
        "48kbps" to "48kbps"
    )
    val audioContainers = listOf(
        "m4a" to "M4A",
        "mp3" to "MP3",
        "opus" to "OPUS",
        "default" to "not specified (default)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Container dropdown
                var containerExpanded by remember { mutableStateOf(false) }
                DropdownField(
                    label = "Audio Format",
                    value = audioContainers.find { it.first == tempContainer }?.second
                        ?: tempContainer.uppercase(),
                    expanded = containerExpanded,
                    onExpandChange = { containerExpanded = it },
                    items = audioContainers,
                    onSelect = { tempContainer = it; containerExpanded = false }
                )

                // Quality dropdown
                var qualityExpanded by remember { mutableStateOf(false) }
                DropdownField(
                    label = "Audio Quality",
                    value = audioQualities.find { it.first == tempQuality }?.second ?: tempQuality,
                    expanded = qualityExpanded,
                    onExpandChange = { qualityExpanded = it },
                    items = audioQualities,
                    onSelect = { tempQuality = it; qualityExpanded = false }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApply(tempQuality, tempContainer) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

//  Reusable styled dropdown field (matches QualitySelectionDialog style) 

@Composable
private fun DropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = label,
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
                .clickable { onExpandChange(true) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, style = MaterialTheme.typography.bodyLarge)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) }
            ) {
                items.forEach { (key, displayName) ->
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = { onSelect(key) }
                    )
                }
            }
        }
    }
}



@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) { Text("Close") }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

//  Segmented toggle (same as before) 

@Composable
private fun SegmentedToggle(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (leftSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(onClick = onLeftClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leftSelected) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = leftLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (leftSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        VerticalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (!leftSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(onClick = onRightClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!leftSelected) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (!leftSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
