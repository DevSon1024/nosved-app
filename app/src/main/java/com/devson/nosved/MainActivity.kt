package com.devson.nosved

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.devson.nosved.ui.theme.NosvedTheme
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.mapper.VideoInfo

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            } else {
                // Handle permission denial
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
            VideoCard(videoInfo = it, onDownloadClicked = { viewModel.downloadVideo(url) })
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
                    Text("Download")
                }
            }
        }
    }
}