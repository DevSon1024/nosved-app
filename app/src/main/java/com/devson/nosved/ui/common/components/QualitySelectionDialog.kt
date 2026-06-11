package com.devson.nosved.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        title = { 
            Text(
                text = "Quality Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        shape = RoundedCornerShape(28.dp),
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Download Mode Selection
                item {
                    Column {
                        Text(
                            text = "Download Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                DownloadMode.VIDEO_AUDIO to "Video + Audio",
                                DownloadMode.AUDIO_ONLY to "Audio Only"
                            ).forEach { (mode, label) ->
                                val isSelected = currentMode == mode
                                val containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                val textColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onModeChange(mode) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = containerColor)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Container Selection
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                listOf("MP4", "WEBM").forEach { container ->
                                    val isSelected = selectedVideoContainer == container
                                    val containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                    val textColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedVideoContainer = container },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = containerColor)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = container,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Quality Selection List Container
                            val videoQualities = if (selectedVideoContainer == "MP4")
                                QualityConstants.VIDEO_MP4_QUALITIES
                            else
                                QualityConstants.VIDEO_WEBM_QUALITIES

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    val displayedQualities = if (showVideoQualities) videoQualities else videoQualities.take(3)
                                    displayedQualities.forEach { quality ->
                                        val isSelected = currentVideoQuality == quality.value
                                        val itemBg = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                        val itemTextColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(itemBg)
                                                .clickable {
                                                    scope.launch {
                                                        qualityPrefs.setVideoQuality(quality.value)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = quality.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = itemTextColor
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    TextButton(
                                        onClick = { showVideoQualities = !showVideoQualities },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(if (showVideoQualities) "Show Less" else "See All Qualities")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (showVideoQualities) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Container Selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            listOf("M4A", "WEBM OPUS").forEach { container ->
                                val isSelected = selectedAudioContainer == container
                                val containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                val textColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedAudioContainer = container },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = containerColor)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = container,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }

                        // Quality Selection List Container
                        val audioQualities = if (selectedAudioContainer == "M4A")
                            QualityConstants.AUDIO_M4A_QUALITIES
                        else
                            QualityConstants.AUDIO_WEBM_OPUS_QUALITIES

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                val displayedQualities = if (showAudioQualities) audioQualities else audioQualities.take(3)
                                displayedQualities.forEach { quality ->
                                    val isSelected = currentAudioQuality == quality.value
                                    val itemBg = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                    val itemTextColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(itemBg)
                                            .clickable {
                                                scope.launch {
                                                    qualityPrefs.setAudioQuality(quality.value)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = quality.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = itemTextColor
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                TextButton(
                                    onClick = { showAudioQualities = !showAudioQualities },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(if (showAudioQualities) "Show Less" else "See All Qualities")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (showAudioQualities) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
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
                    },
                    shape = RoundedCornerShape(12.dp)
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