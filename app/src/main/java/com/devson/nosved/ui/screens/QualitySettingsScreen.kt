package com.devson.nosved.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    // Collect current preferences
    val currentMode by qualityPrefs.downloadMode.collectAsState(initial = DownloadMode.VIDEO_AUDIO)
    val currentVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val currentAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")
    val currentVideoContainer by qualityPrefs.videoContainer.collectAsState(initial = "MP4")
    val currentAudioContainer by qualityPrefs.audioContainer.collectAsState(initial = "M4A")

    // New preferences
    val embedMetadata by qualityPrefs.embedMetadata.collectAsState(initial = true)
    val convertToMp3 by qualityPrefs.convertToMp3.collectAsState(initial = false)

    var showAllVideoQualities by remember { mutableStateOf(false) }
    var showAllAudioQualities by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format Settings") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Download Mode Selection
            item {
                Column {
                    Text(
                        text = "Default Download Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = currentMode == DownloadMode.VIDEO_AUDIO,
                            onClick = { scope.launch { qualityPrefs.setDownloadMode(DownloadMode.VIDEO_AUDIO) } },
                            label = { Text("Video + Audio") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentMode == DownloadMode.AUDIO_ONLY,
                            onClick = { scope.launch { qualityPrefs.setDownloadMode(DownloadMode.AUDIO_ONLY) } },
                            label = { Text("Audio Only") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Audio Only Section
            item {
                AnimatedVisibility(
                    visible = currentMode == DownloadMode.AUDIO_ONLY,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎵 Audio Only Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            // Audio Quality Selection
                            Column {
                                Text("Audio Quality", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

                                val audioQualities = if (currentAudioContainer == "M4A")
                                    QualityConstants.ENHANCED_AUDIO_M4A_QUALITIES
                                else
                                    QualityConstants.ENHANCED_AUDIO_WEBM_OPUS_QUALITIES

                                val qualitiesToShow = if (showAllAudioQualities) audioQualities else audioQualities.take(4)

                                qualitiesToShow.forEach { quality ->
                                    QualitySelectorRow(
                                        text = quality.label,
                                        selected = currentAudioQuality == quality.value,
                                        onClick = {
                                            scope.launch { qualityPrefs.setAudioQuality(quality.value) }
                                        }
                                    )
                                }

                                if (audioQualities.size > 4) {
                                    TextButton(
                                        onClick = { showAllAudioQualities = !showAllAudioQualities }
                                    ) {
                                        Text(if (showAllAudioQualities) "Show Less" else "Show All Qualities")
                                        Icon(Icons.Default.ExpandMore, contentDescription = null,
                                            modifier = Modifier.graphicsLayer(rotationZ = if(showAllAudioQualities) 180f else 0f))
                                    }
                                }
                            }

                            Divider()

                            // Embed Metadata Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Embed Metadata",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Add video info to audio file",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = embedMetadata,
                                    onCheckedChange = { scope.launch { qualityPrefs.setEmbedMetadata(it) } }
                                )
                            }

                            // Convert to MP3 Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Convert to MP3",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Convert M4A/WEBM to MP3 format",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = convertToMp3,
                                    onCheckedChange = { scope.launch { qualityPrefs.setConvertToMp3(it) } }
                                )
                            }
                        }
                    }
                }
            }

            // Video + Audio Section
            item {
                AnimatedVisibility(
                    visible = currentMode == DownloadMode.VIDEO_AUDIO,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎬 Video + Audio Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            // Video Format Mode Selection
                            Text("Video Format Mode", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = currentVideoContainer == "MP4",
                                    onClick = { scope.launch { qualityPrefs.setVideoContainer("MP4") } },
                                    label = { Text("MP4") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = currentVideoContainer == "WEBM",
                                    onClick = { scope.launch { qualityPrefs.setVideoContainer("WEBM") } },
                                    label = { Text("WEBM") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Video Quality Selection
                            Column {
                                Text("Video Quality", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

                                val videoQualities = if (currentVideoContainer == "MP4")
                                    QualityConstants.ENHANCED_VIDEO_MP4_QUALITIES
                                else
                                    QualityConstants.ENHANCED_VIDEO_WEBM_QUALITIES

                                val qualitiesToShow = if (showAllVideoQualities) videoQualities else videoQualities.take(4)

                                qualitiesToShow.forEach { quality ->
                                    QualitySelectorRow(
                                        text = quality.label,
                                        selected = currentVideoQuality == quality.value,
                                        onClick = {
                                            scope.launch { qualityPrefs.setVideoQuality(quality.value) }
                                        }
                                    )
                                }

                                if (videoQualities.size > 4) {
                                    TextButton(
                                        onClick = { showAllVideoQualities = !showAllVideoQualities }
                                    ) {
                                        Text(if (showAllVideoQualities) "Show Less" else "Show All Qualities")
                                        Icon(Icons.Default.ExpandMore, contentDescription = null,
                                            modifier = Modifier.graphicsLayer(rotationZ = if(showAllVideoQualities) 180f else 0f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualitySelectorRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}