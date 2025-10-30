package com.devson.nosved.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val qualityPrefs = remember { QualityPreferences(context) }
    val scope = rememberCoroutineScope()

    // Collect advanced preferences
    val downloadSubtitles by qualityPrefs.downloadSubtitles.collectAsState(initial = false)
    val subtitleFormat by qualityPrefs.subtitleFormat.collectAsState(initial = "srt")
    val convertSubtitles by qualityPrefs.convertSubtitles.collectAsState(initial = false)
    val downloadAutoCaptions by qualityPrefs.downloadAutoCaptions.collectAsState(initial = false)
    val enableSponsorsBlock by qualityPrefs.enableSponsorsBlock.collectAsState(initial = false)
    val extractAudio by qualityPrefs.extractAudio.collectAsState(initial = false)
    val keepVideoAfterAudioExtraction by qualityPrefs.keepVideoAfterAudioExtraction.collectAsState(initial = false)
    val enableCookies by qualityPrefs.enableCookies.collectAsState(initial = false)
    val maxDownloadRetries by qualityPrefs.maxDownloadRetries.collectAsState(initial = 3)
    val preferredLanguage by qualityPrefs.preferredLanguage.collectAsState(initial = "en")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subtitle Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📄 Subtitle Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Download Subtitles Toggle
                        AdvancedSettingRow(
                            title = "Download Subtitles",
                            subtitle = "Download all available subtitles",
                            checked = downloadSubtitles,
                            onToggle = { scope.launch { qualityPrefs.setDownloadSubtitles(it) } }
                        )

                        if (downloadSubtitles) {
                            // Subtitle Format Selection
                            Column {
                                Text(
                                    "Subtitle Format",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SubtitleFormatChips.forEach { format ->
                                        FilterChip(
                                            selected = subtitleFormat == format.value,
                                            onClick = { scope.launch { qualityPrefs.setSubtitleFormat(format.value) } },
                                            label = { Text(format.label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Convert Subtitles Toggle
                            AdvancedSettingRow(
                                title = "Convert Subtitles",
                                subtitle = "Convert to selected format for compatibility",
                                checked = convertSubtitles,
                                onToggle = { scope.launch { qualityPrefs.setConvertSubtitles(it) } }
                            )
                        }

                        // Download Auto Captions Toggle
                        AdvancedSettingRow(
                            title = "Automatic Captions",
                            subtitle = "Download YouTube's auto-generated captions",
                            checked = downloadAutoCaptions,
                            onToggle = { scope.launch { qualityPrefs.setDownloadAutoCaptions(it) } }
                        )
                    }
                }
            }

            // Download Enhancement Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚡ Download Enhancements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // SponsorBlock Integration
                        AdvancedSettingRow(
                            title = "SponsorBlock",
                            subtitle = "Skip sponsored segments automatically",
                            checked = enableSponsorsBlock,
                            onToggle = { scope.launch { qualityPrefs.setEnableSponsorsBlock(it) } }
                        )

                        // Extract Audio from Video
                        AdvancedSettingRow(
                            title = "Extract Audio",
                            subtitle = "Extract audio from downloaded videos",
                            checked = extractAudio,
                            onToggle = { scope.launch { qualityPrefs.setExtractAudio(it) } }
                        )

                        if (extractAudio) {
                            AdvancedSettingRow(
                                title = "Keep Original Video",
                                subtitle = "Keep video file after audio extraction",
                                checked = keepVideoAfterAudioExtraction,
                                onToggle = { scope.launch { qualityPrefs.setKeepVideoAfterAudioExtraction(it) } }
                            )
                        }

                        // Enable Cookies
                        AdvancedSettingRow(
                            title = "Use Cookies",
                            subtitle = "Enable cookies for private/restricted content",
                            checked = enableCookies,
                            onToggle = { scope.launch { qualityPrefs.setEnableCookies(it) } }
                        )
                    }
                }
            }

            // Network & Reliability Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🌐 Network & Reliability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        // Max Download Retries
                        Column {
                            Text(
                                "Download Retries: $maxDownloadRetries",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = maxDownloadRetries.toFloat(),
                                onValueChange = { scope.launch { qualityPrefs.setMaxDownloadRetries(it.toInt()) } },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                            Text(
                                "Number of retry attempts for failed downloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Preferred Language Selection
                        Column {
                            Text(
                                "Preferred Language",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PreferredLanguages.take(3).forEach { lang ->
                                    FilterChip(
                                        selected = preferredLanguage == lang.code,
                                        onClick = { scope.launch { qualityPrefs.setPreferredLanguage(lang.code) } },
                                        label = { Text(lang.label) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

// Subtitle format options
val SubtitleFormatChips = listOf(
    QualityOption("SRT", "srt"),
    QualityOption("VTT", "vtt"),
    QualityOption("ASS", "ass")
)

// Preferred language options
data class LanguageOption(val label: String, val code: String)

val PreferredLanguages = listOf(
    LanguageOption("English", "en"),
    LanguageOption("Spanish", "es"),
    LanguageOption("French", "fr"),
    LanguageOption("German", "de"),
    LanguageOption("Japanese", "ja"),
    LanguageOption("Korean", "ko")
)
