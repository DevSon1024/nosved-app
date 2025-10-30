package com.devson.nosved.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionDialog(
    currentMode: DownloadMode,
    onModeChange: (DownloadMode) -> Unit,
    onAdvancedClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    var selectedVideoContainer by remember { mutableStateOf("MP4") }
    var selectedAudioContainer by remember { mutableStateOf("M4A") }
    var showVideoQualities by remember { mutableStateOf(false) }
    var showAudioQualities by remember { mutableStateOf(false) }

    // Collect current preferences
    val currentVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val currentAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quality Settings") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Download Mode Selection
                item {
                    Column {
                        Text(
                            text = "Download Mode",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currentMode == DownloadMode.VIDEO_AUDIO,
                                onClick = { onModeChange(DownloadMode.VIDEO_AUDIO) },
                                label = { Text("Video + Audio") },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = currentMode == DownloadMode.AUDIO_ONLY,
                                onClick = { onModeChange(DownloadMode.AUDIO_ONLY) },
                                label = { Text("Audio Only") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Video Quality (only show if video mode is selected)
                if (currentMode == DownloadMode.VIDEO_AUDIO) {
                    item {
                        Column {
                            Text(
                                text = "Video Quality",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Container Selection
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedVideoContainer == "MP4",
                                    onClick = { selectedVideoContainer = "MP4" },
                                    label = { Text("MP4") },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedVideoContainer == "WEBM",
                                    onClick = { selectedVideoContainer = "WEBM" },
                                    label = { Text("WEBM") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Quality Selection
                            val videoQualities = if (selectedVideoContainer == "MP4")
                                QualityConstants.VIDEO_MP4_QUALITIES
                            else
                                QualityConstants.VIDEO_WEBM_QUALITIES

                            if (showVideoQualities) {
                                videoQualities.forEach { quality ->
                                    FilterChip(
                                        selected = currentVideoQuality == quality.value,
                                        onClick = {
                                            scope.launch {
                                                qualityPrefs.setVideoQuality(quality.value)
                                            }
                                        },
                                        label = { Text(quality.label) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { showVideoQualities = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Show Less")
                                }
                            } else {
                                // Show only current + 2-3 common options
                                val commonQualities = videoQualities.take(3)
                                commonQualities.forEach { quality ->
                                    FilterChip(
                                        selected = currentVideoQuality == quality.value,
                                        onClick = {
                                            scope.launch {
                                                qualityPrefs.setVideoQuality(quality.value)
                                            }
                                        },
                                        label = { Text(quality.label) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { showVideoQualities = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("See All Qualities")
                                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                // Audio Quality
                item {
                    Column {
                        Text(
                            text = "Audio Quality",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Container Selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            FilterChip(
                                selected = selectedAudioContainer == "M4A",
                                onClick = { selectedAudioContainer = "M4A" },
                                label = { Text("M4A") },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = selectedAudioContainer == "WEBM OPUS",
                                onClick = { selectedAudioContainer = "WEBM OPUS" },
                                label = { Text("WEBM OPUS") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Quality Selection
                        val audioQualities = if (selectedAudioContainer == "M4A")
                            QualityConstants.AUDIO_M4A_QUALITIES
                        else
                            QualityConstants.AUDIO_WEBM_OPUS_QUALITIES

                        if (showAudioQualities) {
                            audioQualities.forEach { quality ->
                                FilterChip(
                                    selected = currentAudioQuality == quality.value,
                                    onClick = {
                                        scope.launch {
                                            qualityPrefs.setAudioQuality(quality.value)
                                        }
                                    },
                                    label = { Text(quality.label) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                )
                            }

                            TextButton(
                                onClick = { showAudioQualities = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show Less")
                            }
                        } else {
                            // Show only current + 2-3 common options
                            val commonQualities = audioQualities.take(3)
                            commonQualities.forEach { quality ->
                                FilterChip(
                                    selected = currentAudioQuality == quality.value,
                                    onClick = {
                                        scope.launch {
                                            qualityPrefs.setAudioQuality(quality.value)
                                        }
                                    },
                                    label = { Text(quality.label) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                )
                            }

                            TextButton(
                                onClick = { showAudioQualities = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("See All Qualities")
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAdvancedClick) {
                    Text("Advanced")
                }

                Button(
                    onClick = {
                        scope.launch {
                            qualityPrefs.setDownloadMode(currentMode)
                        }
                        onDismiss()
                    }
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}