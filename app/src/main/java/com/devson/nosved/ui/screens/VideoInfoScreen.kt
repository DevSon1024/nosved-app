package com.devson.nosved.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.devson.nosved.MainViewModel
import com.devson.nosved.data.DownloadMode
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.ui.components.QualitySelectionDialog
import com.devson.nosved.ui.FormatSelectionSheet
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }

    val videoInfo by viewModel.videoInfo.collectAsState()
    val selectedVideoFormat by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudioFormat by viewModel.selectedAudioFormat.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var selectedDownloadMode by remember { mutableStateOf(DownloadMode.VIDEO_AUDIO) }

    // State for the editable title
    var customTitle by remember(videoInfo) { mutableStateOf(videoInfo?.title ?: "") }
    var isEditingTitle by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect quality preferences
    val defaultVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")
    val defaultAudioQuality by qualityPrefs.audioQuality.collectAsState(initial = "128kbps")
    val downloadMode by qualityPrefs.downloadMode.collectAsState(initial = DownloadMode.VIDEO_AUDIO)

    // Update local state when preferences change
    LaunchedEffect(downloadMode) {
        selectedDownloadMode = downloadMode
    }

    // If no video info, go back
    LaunchedEffect(videoInfo) {
        if (videoInfo == null) {
            onBack()
        } else {
            // Update custom title if videoInfo changes and title is not yet modified
            if (customTitle.isEmpty() || customTitle == "Unknown Title") {
                customTitle = videoInfo?.title ?: "Unknown Title"
            }
        }
    }

    videoInfo?.let { info ->
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Video Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            bottomBar = {
                // Bottom Action Buttons - M3 standard bottom bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer, // Use a standard M3 surface color
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(), // Handles system navigation bar overlap
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Use FilledTonalButton for secondary actions, a common M3 pattern
                            FilledTonalButton(
                                onClick = { showQualityDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Change Quality",
                                    fontSize = 13.sp
                                )
                            }

                            FilledTonalButton(
                                onClick = { showAdvancedSheet = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Advance Format",
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Primary action button
                        Button(
                            onClick = {
                                viewModel.downloadVideoWithQuality(
                                    videoInfo = info,
                                    customTitle = customTitle,
                                    downloadMode = selectedDownloadMode,
                                    preferredVideoQuality = defaultVideoQuality,
                                    preferredAudioQuality = defaultAudioQuality
                                )
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Download ${if (selectedDownloadMode == DownloadMode.AUDIO_ONLY) "Audio" else "Video"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Apply padding from Scaffold
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), // Inner padding
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thumbnail
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp) // Standardized rounding
                    ) {
                        AsyncImage(
                            model = info.thumbnail,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Video Information Card with Editable Title
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            // Use standard M3 surface container color
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Editable Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEditingTitle) {
                                    OutlinedTextField(
                                        value = customTitle,
                                        onValueChange = { customTitle = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = false,
                                        maxLines = 2, // Reduced max lines for cleaner UI
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { isEditingTitle = false }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save Title",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = customTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { isEditingTitle = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Title",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            // Video Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Upload By: ${info.uploader ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Duration: ${formatDuration(info.duration)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                info.viewCount?.let { views ->
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Views",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatViewCount(views),
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quality Selection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            // Use standard M3 surface container color
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Download Quality",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface // Adjusted for surfaceContainer
                                )
                            }

                            // Download Mode Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.VIDEO_AUDIO,
                                    onClick = { selectedDownloadMode = DownloadMode.VIDEO_AUDIO },
                                    label = { Text("Video + Audio", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )

                                ElevatedFilterChip(
                                    selected = selectedDownloadMode == DownloadMode.AUDIO_ONLY,
                                    onClick = { selectedDownloadMode = DownloadMode.AUDIO_ONLY },
                                    label = { Text("Audio Only", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Selection preview
                            if (selectedDownloadMode == DownloadMode.VIDEO_AUDIO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Video Quality Preview
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📹 Video",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = defaultVideoQuality,
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Audio Quality Preview
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🎵 Audio",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = defaultAudioQuality,
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Audio Only Preview
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🎵 Audio Only",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = defaultAudioQuality,
                                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Removed Spacer - Scaffold padding handles the bottom spacing
            }
        }

        // --- Dialogs and Sheets (No layout changes needed) ---

        // Quality Selection Dialog
        if (showQualityDialog) {
            QualitySelectionDialog(
                currentMode = selectedDownloadMode,
                onModeChange = { selectedDownloadMode = it },
                onAdvancedClick = {
                    showQualityDialog = false
                    showAdvancedSheet = true
                },
                onDismiss = { showQualityDialog = false }
            )
        }

        // Advanced Format Selection Sheet
        if (showAdvancedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSheet = false },
                sheetState = sheetState,
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                val formats: List<VideoFormat> = info.formats ?: emptyList()

                FormatSelectionSheet(
                    title = info.title ?: "Unknown Title",
                    thumbnailUrl = info.thumbnail,
                    formats = formats,
                    selectedVideo = selectedVideoFormat,
                    selectedAudio = selectedAudioFormat,
                    onSelectVideo = { format -> viewModel.selectVideoFormat(format) },
                    onSelectAudio = { format -> viewModel.selectAudioFormat(format) },
                    onSelectSuggested = { video, audio ->
                        viewModel.selectVideoFormat(video)
                        viewModel.selectAudioFormat(audio)
                    },
                    onDownload = {
                        val videoFormat = selectedVideoFormat
                        val audioFormat = selectedAudioFormat
                        if (videoFormat != null && audioFormat != null) {
                            viewModel.downloadVideo(info, videoFormat, audioFormat, customTitle)
                            showAdvancedSheet = false
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

// Helper functions (unchanged)
private fun formatDuration(duration: Int?): String {
    if (duration == null) return "Unknown"
    val hours = duration / 3600
    val minutes = (duration % 3600) / 60
    val seconds = duration % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatViewCount(count: Any?): String {
    val viewCount = when (count) {
        is Long -> count
        is Int -> count.toLong()
        is String -> count.toLongOrNull() ?: 0L
        else -> 0L
    }
    return when {
        viewCount >= 1_000_000 -> String.format("%.1fM", viewCount / 1_000_000.0)
        viewCount >= 1_000 -> String.format("%.1fK", viewCount / 1_000.0)
        else -> viewCount.toString()
    }
}