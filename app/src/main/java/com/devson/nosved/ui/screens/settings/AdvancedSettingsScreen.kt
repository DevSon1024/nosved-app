package com.devson.nosved.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.data.*
import com.devson.nosved.viewmodel.SettingsViewModel
import com.devson.nosved.util.YtDlpUpdateInterval
import com.devson.nosved.util.YtDlpUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    val ytdlpUpdateChannel by viewModel.ytdlpUpdateChannel.collectAsState()
    val ytdlpUpdateInterval by viewModel.ytdlpUpdateInterval.collectAsState()
    val downloadNotification by viewModel.downloadNotification.collectAsState()
    val configureBeforeDownload by viewModel.configureBeforeDownload.collectAsState()
    val saveThumbnail by viewModel.saveThumbnail.collectAsState()
    val detailedOutput by viewModel.detailedOutput.collectAsState()

    val incognitoMode by viewModel.incognitoMode.collectAsState()
    val disablePreview by viewModel.disablePreview.collectAsState()

    val downloadPlaylist by viewModel.downloadPlaylist.collectAsState()
    val downloadArchive by viewModel.downloadArchive.collectAsState()
    val enableSponsorsBlock by viewModel.enableSponsorsBlock.collectAsState()

    val downloadSubtitles by qualityPrefs.downloadSubtitles.collectAsState(initial = false)
    val subtitleFormat by qualityPrefs.subtitleFormat.collectAsState(initial = "undefined")
    val convertSubtitles by qualityPrefs.convertSubtitles.collectAsState(initial = false)
    val downloadAutoCaptions by qualityPrefs.downloadAutoCaptions.collectAsState(initial = false)
    val extractAudio by qualityPrefs.extractAudio.collectAsState(initial = false)
    val keepVideoAfterAudioExtraction by qualityPrefs.keepVideoAfterAudioExtraction.collectAsState(initial = false)
    val enableCookies by qualityPrefs.enableCookies.collectAsState(initial = false)
    val maxDownloadRetries by qualityPrefs.maxDownloadRetries.collectAsState(initial = 3)
    val preferredLanguage by qualityPrefs.preferredLanguage.collectAsState(initial = "en")
    val customSubtitleLanguages by qualityPrefs.customSubtitleLanguages.collectAsState(initial = "en,es,fr")

    val ytdlpVersion = remember {
        try {
            val app = context.applicationContext as android.app.Application
            YtDlpUpdater(app).getCurrentVersion()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var tempLanguageInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚙️ General Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("YT-DLP Update Channel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    text = "v$ytdlpVersion",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = ytdlpUpdateChannel == "STABLE", onClick = { viewModel.setYtdlpUpdateChannel("STABLE") }, label = { Text("Stable") })
                                FilterChip(selected = ytdlpUpdateChannel == "NIGHTLY", onClick = { viewModel.setYtdlpUpdateChannel("NIGHTLY") }, label = { Text("Nightly") })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto Update Check", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("Check for updates automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            UpdateIntervalDropdown(currentInterval = ytdlpUpdateInterval, onIntervalSelected = { viewModel.setYtdlpUpdateInterval(it) })
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        AdvancedSettingRow("Download Notification", "Show notification for download progress", downloadNotification) { viewModel.setDownloadNotification(it) }
                        AdvancedSettingRow("Configure Before Download", "Show format chooser before downloading", configureBeforeDownload) { viewModel.setConfigureBeforeDownload(it) }
                        AdvancedSettingRow("Save Thumbnail", "Download video thumbnail file", saveThumbnail) { viewModel.setSaveThumbnail(it) }
                        AdvancedSettingRow("Detailed Output", "Enable verbose yt-dlp logging", detailedOutput) { viewModel.setDetailedOutput(it) }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🔒 Privacy Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AdvancedSettingRow("Incognito Mode", "Do not save download history in app", incognitoMode) { viewModel.setIncognitoMode(it) }
                        AdvancedSettingRow("Disable Preview", "Do not fetch/display video thumbnails in UI", disablePreview) { viewModel.setDisablePreview(it) }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🚀 Downloader Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AdvancedSettingRow("Download Playlist", "Download full playlist if URL contains one", downloadPlaylist) { viewModel.setDownloadPlaylist(it) }
                        AdvancedSettingRow("Download Archive", "Only download videos not listed in archive file", downloadArchive) { viewModel.setDownloadArchive(it) }
                        AdvancedSettingRow("SponsorBlock", "Skip sponsored segments automatically", enableSponsorsBlock) { viewModel.setEnableSponsorsBlock(it) }
                        AdvancedSettingRow("Extract Audio", "Extract audio from downloaded videos", extractAudio) { scope.launch { qualityPrefs.setExtractAudio(it) } }
                        if (extractAudio) {
                            AdvancedSettingRow("Keep Original Video", "Keep video file after audio extraction", keepVideoAfterAudioExtraction) { scope.launch { qualityPrefs.setKeepVideoAfterAudioExtraction(it) } }
                        }
                        AdvancedSettingRow("Use Cookies", "Enable cookies for private/restricted content", enableCookies) { scope.launch { qualityPrefs.setEnableCookies(it) } }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📄 Subtitle Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AdvancedSettingRow("Download Subtitles", "Download all available subtitles", downloadSubtitles) { scope.launch { qualityPrefs.setDownloadSubtitles(it) } }
                        if (downloadSubtitles) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Subtitle Languages", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("Current: $customSubtitleLanguages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { tempLanguageInput = customSubtitleLanguages; showLanguageDialog = true }) { Text("Edit") }
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Column {
                                Text("Subtitle Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = subtitleFormat == "undefined", onClick = { scope.launch { qualityPrefs.setSubtitleFormat("undefined") } }, label = { Text("Undefined") })
                                }
                            }
                            AdvancedSettingRow("Convert Subtitles", "Convert to selected format for compatibility", convertSubtitles) { scope.launch { qualityPrefs.setConvertSubtitles(it) } }
                            if (convertSubtitles) {
                                Column {
                                    Text("Convert To Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ConvertableSubtitleFormats.forEach { format ->
                                            FilterChip(selected = subtitleFormat == format.value, onClick = { scope.launch { qualityPrefs.setSubtitleFormat(format.value) } }, label = { Text(format.label) })
                                        }
                                    }
                                }
                            }
                        }
                        AdvancedSettingRow("Automatic Captions", "Download YouTube's auto-generated captions", downloadAutoCaptions) { scope.launch { qualityPrefs.setDownloadAutoCaptions(it) } }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Network & Reliability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("Download Retries: $maxDownloadRetries", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Slider(value = maxDownloadRetries.toFloat(), onValueChange = { scope.launch { qualityPrefs.setMaxDownloadRetries(it.toInt()) } }, valueRange = 1f..10f, steps = 8)
                        }
                        Column {
                            Text("Preferred Language", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PreferredLanguages.take(3).forEach { lang ->
                                    FilterChip(selected = preferredLanguage == lang.code, onClick = { scope.launch { qualityPrefs.setPreferredLanguage(lang.code) } }, label = { Text(lang.label) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Subtitle Languages") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter language codes separated by commas (e.g., en,es,fr,de,ja,ko)", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = tempLanguageInput, onValueChange = { tempLanguageInput = it }, label = { Text("Language codes") }, modifier = Modifier.fillMaxWidth())
                    Text("Common: en (English), es (Spanish), fr (French), de (German), ja (Japanese), ko (Korean), pt (Portuguese), it (Italian), ru (Russian), zh (Chinese)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { scope.launch { qualityPrefs.setCustomSubtitleLanguages(tempLanguageInput) }; showLanguageDialog = false }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateIntervalDropdown(
    currentInterval: YtDlpUpdateInterval,
    onIntervalSelected: (YtDlpUpdateInterval) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentInterval.displayName)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            YtDlpUpdateInterval.entries.forEach { interval ->
                DropdownMenuItem(text = { Text(interval.displayName) }, onClick = { onIntervalSelected(interval); expanded = false })
            }
        }
    }
}

@Composable
fun AdvancedSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

val ConvertableSubtitleFormats = listOf(
    QualityOption("SRT", "srt"),
    QualityOption("VTT", "vtt"),
    QualityOption("ASS", "ass"),
    QualityOption("LRC", "lrc")
)

data class LanguageOption(val label: String, val code: String)

val PreferredLanguages = listOf(
    LanguageOption("English", "en"),
    LanguageOption("Spanish", "es"),
    LanguageOption("French", "fr"),
    LanguageOption("German", "de"),
    LanguageOption("Japanese", "ja"),
    LanguageOption("Korean", "ko")
)