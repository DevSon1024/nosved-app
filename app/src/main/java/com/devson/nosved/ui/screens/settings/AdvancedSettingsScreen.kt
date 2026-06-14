package com.devson.nosved.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.viewmodel.SettingsViewModel
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

    val downloadNotification by viewModel.downloadNotification.collectAsState()
    val configureBeforeDownload by viewModel.configureBeforeDownload.collectAsState()
    val saveThumbnail by viewModel.saveThumbnail.collectAsState()
    val detailedOutput by viewModel.detailedOutput.collectAsState()

    val incognitoMode by viewModel.incognitoMode.collectAsState()
    val disablePreview by viewModel.disablePreview.collectAsState()

    val downloadPlaylist by viewModel.downloadPlaylist.collectAsState()
    val downloadArchive by viewModel.downloadArchive.collectAsState()
    val enableSponsorsBlock by viewModel.enableSponsorsBlock.collectAsState()

    val extractAudio by qualityPrefs.extractAudio.collectAsState(initial = false)
    val keepVideoAfterAudioExtraction by qualityPrefs.keepVideoAfterAudioExtraction.collectAsState(initial = false)
    val enableCookies by qualityPrefs.enableCookies.collectAsState(initial = false)
    val maxDownloadRetries by qualityPrefs.maxDownloadRetries.collectAsState(initial = 3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    AdvancedSettingCard(
                        title = "Download Notification",
                        subtitle = "Show notification for download progress",
                        checked = downloadNotification,
                        onToggle = { viewModel.setDownloadNotification(it) }
                    )
                    AdvancedSettingCard(
                        title = "Configure Before Download",
                        subtitle = "Show format chooser before downloading",
                        checked = configureBeforeDownload,
                        onToggle = { viewModel.setConfigureBeforeDownload(it) }
                    )
                    AdvancedSettingCard(
                        title = "Save Thumbnail",
                        subtitle = "Download video thumbnail file",
                        checked = saveThumbnail,
                        onToggle = { viewModel.setSaveThumbnail(it) }
                    )
                    AdvancedSettingCard(
                        title = "Detailed Output",
                        subtitle = "Enable verbose yt-dlp logging",
                        checked = detailedOutput,
                        onToggle = { viewModel.setDetailedOutput(it) }
                    )
                }
            }

            // Privacy Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    AdvancedSettingCard(
                        title = "Incognito Mode",
                        subtitle = "Do not save download history in app",
                        checked = incognitoMode,
                        onToggle = { viewModel.setIncognitoMode(it) }
                    )
                    AdvancedSettingCard(
                        title = "Disable Preview",
                        subtitle = "Do not fetch/display video thumbnails in UI",
                        checked = disablePreview,
                        onToggle = { viewModel.setDisablePreview(it) }
                    )
                }
            }

            // Downloader Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Downloader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    AdvancedSettingCard(
                        title = "Download Playlist",
                        subtitle = "Download full playlist if URL contains one",
                        checked = downloadPlaylist,
                        onToggle = { viewModel.setDownloadPlaylist(it) }
                    )
                    AdvancedSettingCard(
                        title = "Download Archive",
                        subtitle = "Only download videos not listed in archive file",
                        checked = downloadArchive,
                        onToggle = { viewModel.setDownloadArchive(it) }
                    )
                    AdvancedSettingCard(
                        title = "SponsorBlock",
                        subtitle = "Skip sponsored segments automatically",
                        checked = enableSponsorsBlock,
                        onToggle = { viewModel.setEnableSponsorsBlock(it) }
                    )
                    AdvancedSettingCard(
                        title = "Extract Audio",
                        subtitle = "Extract audio from downloaded videos",
                        checked = extractAudio,
                        onToggle = { scope.launch { qualityPrefs.setExtractAudio(it) } }
                    )
                    AdvancedSettingCard(
                        title = "Keep Original Video",
                        subtitle = "Keep video file after audio extraction",
                        checked = keepVideoAfterAudioExtraction,
                        enabled = extractAudio,
                        onToggle = { scope.launch { qualityPrefs.setKeepVideoAfterAudioExtraction(it) } }
                    )
                    AdvancedSettingCard(
                        title = "Use Cookies",
                        subtitle = "Enable cookies for private/restricted content",
                        checked = enableCookies,
                        onToggle = { scope.launch { qualityPrefs.setEnableCookies(it) } }
                    )
                }
            }

            // Network & Reliability Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Network & Reliability",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        "Download retries",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Maximum retry attempts on network failure",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = maxDownloadRetries.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Slider(
                                value = maxDownloadRetries.toFloat(),
                                onValueChange = { scope.launch { qualityPrefs.setMaxDownloadRetries(it.toInt()) } },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onToggle(!checked) } else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}