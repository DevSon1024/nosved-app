package com.devson.nosved.viewmodel

import android.app.Application
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devson.nosved.data.DownloadDatabase
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.data.repository.DownloadRepository
import com.devson.nosved.util.NotificationHelper
import com.devson.nosved.util.QualityHelper
import com.devson.nosved.util.VideoInfoUtil
import com.devson.nosved.util.extractETA
import com.devson.nosved.util.extractSpeed
import com.devson.nosved.util.sanitizeTitle
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.abs

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
     * 2. Inserts entity into Room (QUEUED).
     * 3. Flips state to Queued -> Activity sees dismissSignal and calls finish().
     * 4. Hands actual YoutubeDL execution to appScope (lifecycle-independent).
     */
    fun startDownload() {
        val ready = _state.value as? State.Ready ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = ready.videoInfo
                val formats = info.formats ?: throw Exception("No formats available")
                val title = info.title ?: "Unknown Title"
                val sanitized = sanitizeTitle(title)
                val downloadId = UUID.randomUUID().toString()
                val notifId = abs(downloadId.hashCode())
                val prefs = ctx.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                val notifEnabled = prefs.getBoolean("download_notification_enabled", true)

                if (ready.isAudio) {
                    val targetBitrate = QualityHelper.parseQualityFromString(ready.audioQuality)
                    val ac = ready.audioContainer.lowercase()
                    val audioFmt = QualityHelper.findNearestAudioQuality(formats, targetBitrate, ac)
                        ?: QualityHelper.findNearestAudioQuality(formats, targetBitrate, "m4a")
                        ?: formats.filter { it.acodec != "none" && it.vcodec == "none" }.maxByOrNull { it.abr ?: 0 }
                        ?: throw Exception("No audio format found")

                    val convertEnabled = qualityPrefs.convertAudioFormatEnabled.first()
                    val convertFmt = qualityPrefs.convertAudioFormat.first()
                    val targetExt = if (convertEnabled) convertFmt else (audioFmt.ext ?: "m4a")

                    val entity = buildAudioEntity(downloadId, title, info, audioFmt)
                    repository.insertDownload(entity)

                    // Signal UI to close immediately
                    _state.value = State.Queued
                    _dismissSignal.value = true
                    showToast("Quick Nosved: audio download queued")

                    // Run in detached scope - survives activity finish
                    appScope.launch {
                        executeQuickAudio(entity, audioFmt, sanitized, notifId, notifEnabled, targetExt)
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

                    val entity = buildVideoEntity(downloadId, title, info, videoFmt, audioFmt)
                    repository.insertDownload(entity)

                    // Signal UI to close immediately
                    _state.value = State.Queued
                    _dismissSignal.value = true
                    showToast("Quick Nosved: video download queued")

                    // Run in detached scope - survives activity finish
                    appScope.launch {
                        executeQuickVideo(entity, videoFmt, audioFmt, sanitized, notifId, notifEnabled)
                    }
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Download failed")
            }
        }
    }

    private fun buildVideoEntity(
        id: String,
        title: String,
        info: VideoInfo,
        video: VideoFormat,
        audio: VideoFormat
    ) = DownloadEntity(
        id = id,
        title = title,
        url = _url,
        thumbnail = info.thumbnail?.replace("hqdefault.jpg", "mqdefault.jpg") ?: info.thumbnail,
        filePath = null,
        fileName = null,
        fileSize = (video.fileSize ?: 0L) + (audio.fileSize ?: 0L),
        status = DownloadStatus.QUEUED,
        duration = info.duration?.toString(),
        uploader = info.uploader,
        videoFormat = "${video.height}p",
        audioFormat = "${audio.abr}kbps"
    )

    private fun buildAudioEntity(
        id: String,
        title: String,
        info: VideoInfo,
        audio: VideoFormat
    ) = DownloadEntity(
        id = id,
        title = "$title (Audio)",
        url = _url,
        thumbnail = info.thumbnail?.replace("hqdefault.jpg", "mqdefault.jpg") ?: info.thumbnail,
        filePath = null,
        fileName = null,
        fileSize = audio.fileSize ?: 0L,
        status = DownloadStatus.QUEUED,
        duration = info.duration?.toString(),
        uploader = info.uploader,
        videoFormat = "Audio Only",
        audioFormat = "${audio.abr}kbps"
    )

    private suspend fun executeQuickVideo(
        entity: DownloadEntity,
        video: VideoFormat,
        audio: VideoFormat,
        sanitized: String,
        notifId: Int,
        notifEnabled: Boolean
    ) {
        try {
            repository.updateDownloadStatus(entity.id, DownloadStatus.DOWNLOADING)

            val nosvedDir = ensureNosvedDir()
            val request = YoutubeDLRequest(_url)
            request.addOption("-o", File(nosvedDir, "$sanitized.%(ext)s").absolutePath)
            request.addOption("-f", "${video.formatId}+${audio.formatId}/best")
            request.addOption("--merge-output-format", "mp4")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")
            request.addOption("--socket-timeout", "10")
            request.addOption("--retries", "3")
            request.addOption("--fragment-retries", "3")
            request.addOption(
                "--user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            request.addOption("--referer", "https://www.youtube.com/")

            val finalFile = File(nosvedDir, "$sanitized.mp4")

            if (notifEnabled) {
                notificationHelper.showDownloadProgressNotification(notifId, entity.title, "Starting...")
            }

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                if (notifEnabled) {
                    notificationHelper.showDownloadProgressNotification(notifId, entity.title, line)
                }
                // Throttle DB progress writes to avoid hammering Room
                if (progress.toInt() % 5 == 0) {
                    appScope.launch {
                        repository.updateDownload(
                            entity.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress.toInt()
                            )
                        )
                    }
                }
            }

            repository.updateDownload(
                entity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFile.absolutePath,
                    fileName = "$sanitized.mp4",
                    completedAt = System.currentTimeMillis(),
                    progress = 100
                )
            )
            if (notifEnabled) {
                notificationHelper.showDownloadCompleteNotification(
                    notifId, entity.title, finalFile.absolutePath, false
                )
            }
        } catch (e: Exception) {
            repository.updateDownload(entity.copy(status = DownloadStatus.FAILED, error = e.message))
            notificationHelper.cancelNotification(notifId)
        }
    }

    private suspend fun executeQuickAudio(
        entity: DownloadEntity,
        audio: VideoFormat,
        sanitized: String,
        notifId: Int,
        notifEnabled: Boolean,
        targetExt: String
    ) {
        try {
            repository.updateDownloadStatus(entity.id, DownloadStatus.DOWNLOADING)

            val nosvedDir = ensureNosvedDir()
            val request = YoutubeDLRequest(_url)
            request.addOption("-o", File(nosvedDir, "$sanitized.%(ext)s").absolutePath)
            request.addOption("-f", audio.formatId ?: "bestaudio")
            request.addOption("-x")
            request.addOption("--audio-format", targetExt)
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")
            request.addOption("--socket-timeout", "10")
            request.addOption("--retries", "3")

            val finalFile = File(nosvedDir, "$sanitized.$targetExt")

            if (notifEnabled) {
                notificationHelper.showDownloadProgressNotification(notifId, entity.title, "Starting...")
            }

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                if (notifEnabled) {
                    notificationHelper.showDownloadProgressNotification(notifId, entity.title, line)
                }
                if (progress.toInt() % 5 == 0) {
                    appScope.launch {
                        repository.updateDownload(
                            entity.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress.toInt()
                            )
                        )
                    }
                }
            }

            repository.updateDownload(
                entity.copy(
                    status = DownloadStatus.COMPLETED,
                    filePath = finalFile.absolutePath,
                    fileName = "$sanitized.$targetExt",
                    completedAt = System.currentTimeMillis(),
                    progress = 100
                )
            )
            if (notifEnabled) {
                notificationHelper.showDownloadCompleteNotification(
                    notifId, entity.title, finalFile.absolutePath, true
                )
            }
        } catch (e: Exception) {
            repository.updateDownload(entity.copy(status = DownloadStatus.FAILED, error = e.message))
            notificationHelper.cancelNotification(notifId)
        }
    }

    private fun ensureNosvedDir(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "nosved"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun showToast(msg: String) {
        appScope.launch(Dispatchers.Main) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
