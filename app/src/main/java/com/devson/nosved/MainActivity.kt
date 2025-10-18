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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.devson.nosved.ui.theme.NosvedTheme
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
                    NosvedApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun NosvedApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "download_screen") {
        composable("download_screen") {
            DownloadScreen(viewModel, navController)
        }
        composable("download_queue") {
            DownloadQueueScreen(viewModel, navController)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: MainViewModel, navController: NavController) {
    var url by remember { mutableStateOf("") }
    val videoInfo by viewModel.videoInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedVideoFormat by viewModel.selectedVideoFormat.collectAsState()
    val selectedAudioFormat by viewModel.selectedAudioFormat.collectAsState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nosved") },
                actions = {
                    IconButton(onClick = { /* TODO: Implement settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter URL") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { url = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.fetchVideoInfo(url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && url.isNotBlank()
            ) {
                Text("Search Video")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("download_queue") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Download Queue")
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            videoInfo?.let {
                Spacer(modifier = Modifier.height(16.dp))
                VideoInfoCard(
                    videoInfo = it,
                    selectedVideoFormat = selectedVideoFormat,
                    selectedAudioFormat = selectedAudioFormat,
                    onVideoFormatSelected = { format -> viewModel.selectVideoFormat(format) },
                    onAudioFormatSelected = { format -> viewModel.selectAudioFormat(format) },
                    onDownloadClicked = { vFormat, aFormat ->
                        viewModel.downloadVideo(it, vFormat, aFormat)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(viewModel: MainViewModel, navController: NavController) {
    // TODO: Implement UI for download queue
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Queue") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Download queue will be shown here.", textAlign = TextAlign.Center)
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = videoInfo.thumbnail,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(videoInfo.title.toString(), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Uploader: ${videoInfo.uploader}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Duration: ${videoInfo.duration}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val videoFormats =
                videoInfo.formats?.filter { it.vcodec != "none" && it.acodec == "none" }
                    ?: emptyList()
            val audioFormats =
                videoInfo.formats?.filter { it.acodec != "none" && it.vcodec == "none" }
                    ?: emptyList()

            Row(Modifier.fillMaxWidth()) {
                QualityDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Video Quality",
                    items = videoFormats,
                    selectedItem = selectedVideoFormat,
                    onItemSelected = onVideoFormatSelected,
                    itemLabel = { "${it.height}p${it.fps ?: ""} (${it.fileSize.toString()})" }
                )
                Spacer(modifier = Modifier.width(8.dp))
                QualityDropdown(
                    modifier = Modifier.weight(1f),
                    label = "Audio Quality",
                    items = audioFormats,
                    selectedItem = selectedAudioFormat,
                    onItemSelected = onAudioFormatSelected,
                    itemLabel = { "${it.abr}kbps (${it.fileSize.toString()})" }
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedVideoFormat != null && selectedAudioFormat != null) {
                        onDownloadClicked(selectedVideoFormat, selectedAudioFormat)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedVideoFormat != null && selectedAudioFormat != null
            ) {
                Text("Download")
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
            value = if (selectedItem != null) itemLabel(selectedItem) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item), style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}