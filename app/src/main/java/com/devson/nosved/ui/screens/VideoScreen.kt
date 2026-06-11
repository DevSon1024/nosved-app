package com.devson.nosved.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.ui.common.sheet.FormatSelectionSheet
import com.devson.nosved.viewmodel.MainViewModel
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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val defaultVideoQuality by qualityPrefs.videoQuality.collectAsState(initial = "720p")

    if (showSheet && info != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            modifier = Modifier.fillMaxSize()
        ) {
            var sheetTitle by remember(info) { mutableStateOf(info!!.title ?: "") }
            val formats: List<VideoFormat> = info!!.formats ?: emptyList()
            FormatSelectionSheet(
                title = sheetTitle,
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
                    val v = selVideo
                    val a = selAudio
                    if (v != null && a != null) {
                        viewModel.downloadVideo(info!!, v, a, sheetTitle)
                        showSheet = false
                    }
                },
                onClose = { showSheet = false },
                uploader = info!!.uploader,
                duration = info!!.duration?.let { formatDuration(it) },
                onUpdateTitle = { sheetTitle = it },
                defaultVideoQuality = defaultVideoQuality
            )
        }
    }
}

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