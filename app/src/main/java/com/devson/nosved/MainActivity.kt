package com.devson.nosved

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.devson.nosved.ui.theme.NosvedTheme
import com.devson.nosved.ui.screens.DownloadsScreen
import com.devson.nosved.ui.screens.SettingsScreen
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("NosvedApp", "Failed to initialize youtubedl-android", e)
        }

        askNotificationPermission()

        setContent {
            NosvedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // App Icon
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // App Name
                        Text(
                            text = "Nosved",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Notification/Downloads count indicator
                    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
                    val runningDownloads = allDownloads.count {
                        it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                                it.status == com.devson.nosved.data.DownloadStatus.QUEUED
                    }

                    if (runningDownloads > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text("$runningDownloads")
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    navController.navigate("downloads") {
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Active Downloads"
                                )
                            }
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text("Download") },
                    selected = currentDestination == "download",
                    onClick = {
                        if (currentDestination != "download") {
                            navController.navigate("download") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = {
                        val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
                        val runningCount = allDownloads.count {
                            it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                                    it.status == com.devson.nosved.data.DownloadStatus.QUEUED
                        }

                        if (runningCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("$runningCount") }
                                }
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                            }
                        } else {
                            Icon(Icons.Default.List, contentDescription = null)
                        }
                    },
                    label = { Text("Downloads") },
                    selected = currentDestination == "downloads",
                    onClick = {
                        if (currentDestination != "downloads") {
                            navController.navigate("downloads") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "download",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("download") {
                DownloadScreen(viewModel)
            }
            composable("downloads") {
                DownloadsScreen(viewModel)
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadScreen(viewModel: MainViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val videoInfo by viewModel.videoInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedVideoFormat by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudioFormat by viewModel.selectedAudioFormat.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // URL Input Section with Paste Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentUrl,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("Enter URL") },
                placeholder = { Text("Paste video URL here...") },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                trailingIcon = {
                    if (currentUrl.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearUrl() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Paste Button
            OutlinedButton(
                onClick = { viewModel.pasteFromClipboard() },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Button
        Button(
            onClick = { viewModel.fetchVideoInfo(currentUrl) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && currentUrl.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching...")
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Video")
            }
        }

        // Loading State
        if (isLoading) {
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Fetching video information...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Video Info Card with proper scrollable content
        videoInfo?.let {
            Spacer(modifier = Modifier.height(16.dp))

            // Make the video info card take remaining space and be scrollable
            Box(modifier = Modifier.weight(1f)) {
                VideoInfoCard(
                    videoInfo = it,
                    selectedVideoFormat = selectedVideoFormat,
                    selectedAudioFormat = selectedAudioFormat,
                    onVideoFormatSelected = { format: VideoFormat -> viewModel.selectVideoFormat(format) },
                    onAudioFormatSelected = { format: VideoFormat -> viewModel.selectAudioFormat(format) },
                    onDownloadClicked = { vFormat: VideoFormat, aFormat: VideoFormat ->
                        viewModel.downloadVideo(it, vFormat, aFormat)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoCard(
    videoInfo: VideoInfo,
    selectedVideoFormat: VideoFormat?,
    selectedAudioFormat: VideoFormat?,
    onVideoFormatSelected: (VideoFormat) -> Unit,
    onAudioFormatSelected: (VideoFormat) -> Unit,
    onDownloadClicked: (VideoFormat, VideoFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full-size Thumbnail
            item {
                AsyncImage(
                    model = videoInfo.thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Video Information
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = videoInfo.title ?: "Unknown Title",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start
                    )

                    Text(
                        text = "Uploader: ${videoInfo.uploader ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Duration: ${formatDuration(videoInfo.duration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quality Selection Section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quality Selection",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val videoFormats = videoInfo.formats
                        ?.filter { it.vcodec != "none" && it.acodec == "none" }
                        ?: emptyList()
                    val audioFormats = videoInfo.formats
                        ?.filter { it.acodec != "none" && it.vcodec == "none" }
                        ?: emptyList()

                    // Video Quality Dropdown
                    QualityDropdown(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Video Quality",
                        items = videoFormats,
                        selectedItem = selectedVideoFormat,
                        onItemSelected = onVideoFormatSelected,
                        itemLabel = { format ->
                            "${format.height ?: "?"}p${format.fps?.let { "/$it" } ?: ""} (${format.getFormattedFileSize()})"
                        }
                    )

                    // Audio Quality Dropdown
                    QualityDropdown(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Audio Quality",
                        items = audioFormats,
                        selectedItem = selectedAudioFormat,
                        onItemSelected = onAudioFormatSelected,
                        itemLabel = { format ->
                            "${format.abr ?: "?"}kbps (${format.getFormattedFileSize()})"
                        }
                    )
                }
            }

            // Download Button
            item {
                Button(
                    onClick = {
                        if (selectedVideoFormat != null && selectedAudioFormat != null) {
                            onDownloadClicked(selectedVideoFormat, selectedAudioFormat)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = selectedVideoFormat != null && selectedAudioFormat != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download Video",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Add some bottom padding to ensure the download button is fully visible
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> QualityDropdown(
    modifier: Modifier = Modifier,
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedItem?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Helper function to format duration
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
