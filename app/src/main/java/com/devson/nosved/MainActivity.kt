package com.devson.nosved

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.theme.NosvedTheme
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("NosvedApp", "Failed to initialize youtubedl-android", e)
        }
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

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Video URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.fetchVideoInfo(url) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Fetch Info")
        }

        videoInfo?.let {
            Text("Title: ${it.title}", modifier = Modifier.padding(top = 16.dp))
            Text("Uploader: ${it.uploader}")
            Text("Duration: ${it.duration}")
        }
    }
}