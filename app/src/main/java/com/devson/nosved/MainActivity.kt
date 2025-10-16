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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
                    DownloadScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloadScreen(viewModel: MainViewModel) {
    var url by remember { mutableStateOf("") }
    val videoInfo by viewModel.videoInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showQualityDialog by viewModel.showQualityDialog.collectAsState()

    if (showQualityDialog && videoInfo != null) {
        QualitySelectionDialog(
            videoInfo = videoInfo!!,
            onDismiss = { viewModel.hideQualityDialog() },
            onDownload = { format ->
                viewModel.downloadVideo(videoInfo!!, format)
                viewModel.hideQualityDialog()
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Video URL") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { url = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        Button(
            onClick = { viewModel.fetchVideoInfo(url) },
            modifier = Modifier.padding(top = 8.dp),
            enabled = !isLoading
        ) {
            Text("Fetch Info")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        videoInfo?.let {
            VideoCard(videoInfo = it, onDownloadClicked = { viewModel.showQualityDialog() })
        }
    }
}

@Composable
fun VideoCard(
    videoInfo: VideoInfo,
    onDownloadClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = videoInfo.thumbnail,
                contentDescription = "Video Thumbnail",
                modifier = Modifier.size(120.dp)
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(videoInfo.title.toString(), style = MaterialTheme.typography.titleMedium)
                Text("Uploader: ${videoInfo.uploader}", style = MaterialTheme.typography.bodyMedium)
                Text("Duration: ${videoInfo.duration}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onDownloadClicked, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Download Again")
                }
            }
        }
    }
}

@Composable
fun QualitySelectionDialog(
    videoInfo: VideoInfo,
    onDismiss: () -> Unit,
    onDownload: (VideoFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf<VideoFormat?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality") },
        text = {
            LazyColumn {
                items(videoInfo.formats.orEmpty()) { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("${format.formatNote} (${format.ext})")
                            Text(format.getFormattedFileSize(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFormat?.let { onDownload(it) }
                },
                enabled = selectedFormat != null
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}