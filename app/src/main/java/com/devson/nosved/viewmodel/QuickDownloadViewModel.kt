package com.devson.nosved.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.DownloadDatabase
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.data.repository.DownloadRepository
import com.devson.nosved.data.service.DownloadService
import com.devson.nosved.util.NotificationHelper
import com.devson.nosved.util.QualityHelper
import com.devson.nosved.util.VideoInfoUtil
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuickDownloadViewModel(application: Application) : AndroidViewModel(application) {

    sealed class State {
        object Idle : State()
        data class Loading(val message: String) : State()
        data class Ready(
            val videoInfo: VideoInfo,
            val isAudio: Boolean,
            val videoQuality: String,
            val audioQuality: String,
            val videoContainer: String,
            val audioContainer: String
        ) : State()
        object Queued : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // Fires true once the download is queued — Activity observes this to finish()
    private val _dismissSignal = MutableStateFlow(false)
    val dismissSignal: StateFlow<Boolean> = _dismissSignal.asStateFlow()

    private var _url: String = ""
    private val ctx get() = getApplication<Application>()

    private val db by lazy { DownloadDatabase.getDatabase(ctx) }
    private val repository by lazy { DownloadRepository(db.downloadDao()) }
    private val notificationHelper by lazy {
        NotificationHelper(ctx).also { it.createNotificationChannel() }
    }
    private val qualityPrefs by lazy { QualityPreferences(ctx) }

    // App-level scope: survives Activity and ViewModel lifecycle
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadService by lazy {
        DownloadService(
            context = ctx,
            repository = repository,
            notificationHelper = notificationHelper,
            progressFlow = MutableStateFlow(emptyMap()),
            coroutineScope = appScope
        )
    }

    fun processUrl(url: String) {
        _url = url
        fetchInfo()
    }

    fun retry() {
        fetchInfo()
    }

    private fun fetchInfo() {
        _state.value = State.Loading("Checking link...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = VideoInfoUtil.fetchVideoInfoProgressive(_url) { }
                val info = result.getOrThrow()

                val vq = qualityPrefs.videoQuality.first()
                val aq = qualityPrefs.audioQuality.first()
                val vc = qualityPrefs.videoContainer.first()
                val ac = qualityPrefs.audioContainer.first()

                _state.value = State.Ready(
                    videoInfo = info,
                    isAudio = false,
                    videoQuality = vq,
                    audioQuality = aq,
                    videoContainer = vc,
                    audioContainer = ac
                )
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to fetch video info")
            }
        }
    }

    fun setAudioMode(isAudio: Boolean) {
        (_state.value as? State.Ready)?.let { _state.value = it.copy(isAudio = isAudio) }
    }

    fun setVideoQuality(q: String) {
        (_state.value as? State.Ready)?.let { _state.value = it.copy(videoQuality = q) }
    }

    fun setAudioQuality(q: String) {
        (_state.value as? State.Ready)?.let { _state.value = it.copy(audioQuality = q) }
    }

    fun setVideoContainer(c: String) {
        (_state.value as? State.Ready)?.let { _state.value = it.copy(videoContainer = c) }
    }

    fun setAudioContainer(c: String) {
        (_state.value as? State.Ready)?.let { _state.value = it.copy(audioContainer = c) }
    }

    /**
     * Called when the user taps Download.
     * 1. Resolves formats.
     * 2. Delegates execution to DownloadService.
     * 3. Flips state to Queued -> Activity sees dismissSignal and calls finish().
     * 4. Hands actual YoutubeDL execution to appScope (lifecycle-independent).
     */
    fun startDownload() {
        val ready = _state.value as? State.Ready ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = ready.videoInfo
                val formats = info.formats ?: throw Exception("No formats available")

                // Signal UI to close immediately
                _state.value = State.Queued
                _dismissSignal.value = true

                if (ready.isAudio) {
                    val targetBitrate = QualityHelper.parseQualityFromString(ready.audioQuality)
                    val ac = ready.audioContainer.lowercase()
                    val audioFmt = QualityHelper.findNearestAudioQuality(formats, targetBitrate, ac)
                        ?: QualityHelper.findNearestAudioQuality(formats, targetBitrate, "m4a")
                        ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }
                        ?: throw Exception("No audio format found")

                    // Run in detached scope - survives activity finish
                    appScope.launch {
                        downloadService.startAudioDownload(
                            url = _url,
                            videoInfo = info,
                            audioFormat = audioFmt,
                            customTitle = "",
                            downloadSubtitles = false,
                            subtitleLang = ""
                        )
                    }

                } else {
                    val targetHeight = QualityHelper.parseQualityFromString(ready.videoQuality)
                    val vc = ready.videoContainer.lowercase()
                    val ac = ready.audioContainer.lowercase()

                    val videoFmt = QualityHelper.findNearestVideoQuality(formats, targetHeight, vc)
                        ?: QualityHelper.findNearestVideoQuality(formats, targetHeight, "mp4")
                        ?: formats.filter { it.vcodec != "none" && it.acodec == "none" }.maxByOrNull { it.height ?: 0 }
                        ?: throw Exception("No video format found")

                    val audioFmt = QualityHelper.findNearestAudioQuality(formats, 128, ac)
                        ?: QualityHelper.findNearestAudioQuality(formats, 128, "m4a")
                        ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }
                        ?: throw Exception("No audio format found")

                    // Run in detached scope - survives activity finish
                    appScope.launch {
                        downloadService.startVideoDownload(
                            url = _url,
                            videoInfo = info,
                            videoFormat = videoFmt,
                            audioFormat = audioFmt,
                            customTitle = "",
                            downloadSubtitles = false,
                            subtitleLang = ""
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Download failed")
            }
        }
    }

    private fun showToast(msg: String) {
        appScope.launch(Dispatchers.Main) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
