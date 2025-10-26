package com.devson.nosved.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.devson.nosved.MainViewModel
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.youtubedl_android.mapper.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: MainViewModel
) {
    val info by viewModel.videoInfo.collectAsState()
    val selVideo by viewModel.selectedVideoFormat.collectAsState()
    val selAudio by viewModel.selectedAudioFormat.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Somewhere in your UI header: a button “Formats”
    // Button(onClick = { showSheet = true }) { Text("Formats") }

    if (showSheet && info != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            val formats: List<VideoFormat> = info!!.formats ?: emptyList()
            FormatSelectionSheet(
                title = info!!.title ?: "",
                thumbnailUrl = info!!.thumbnail,
                formats = formats,
                selectedVideo = selVideo,
                selectedAudio = selAudio,
                onSelectVideo = { viewModel.selectVideoFormat(it) },
                onSelectAudio = { viewModel.selectAudioFormat(it) },
                onSelectSuggested = { v, a ->
                    viewModel.selectVideoFormat(v)
                    viewModel.selectAudioFormat(a)
                },
                onDownload = {
                    val v = selVideo ?: return@FormatSelectionSheet
                    val a = selAudio ?: return@FormatSelectionSheet
                    viewModel.downloadVideo(info!!, v, a)
                    showSheet = false
                }
            )
        }
    }
}
