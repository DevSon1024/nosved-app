package com.devson.nosved.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.devson.nosved.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            HomeTopAppBar(
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToSettings = onNavigateToSettings,
                viewModel = viewModel
            )
        },
        floatingActionButton = {
            // Updated Floating Paste Button (M3 standard)
            FloatingActionButton(
                onClick = { viewModel.pasteUrlOnly() },
                containerColor = MaterialTheme.colorScheme.secondary, // M3 uses primary or secondary
                contentColor = MaterialTheme.colorScheme.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                // Shape defaults to M3's RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste URL Only",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // URL Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        // Use standard M3 surface container color
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🔗 Video URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // URL Input Field
                        OutlinedTextField(
                            value = currentUrl,
                            onValueChange = { viewModel.updateUrl(it) },
                            label = { Text("Enter Video URL") },
                            placeholder = { Text("https://youtube.com/watch?v=...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            },
                            trailingIcon = {
                                if (currentUrl.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearUrl() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                            // Removed shape - uses M3 default
                        )

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Search/Analyze Button
                            Button(
                                onClick = { viewModel.fetchVideoInfo(currentUrl) },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(48.dp), // Standard button height
                                enabled = !isLoading && currentUrl.isNotBlank(),
                                // Removed shape - uses M3 default (pill shape)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing...")
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze Video")
                                }
                            }
                        }
                    }
                }

                // Enhanced Loading State
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 4.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "⚡ Extracting video information...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This will only take a few seconds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Recent Features Info (when not loading)
                if (!isLoading && currentUrl.isBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            // Use standard M3 surface container color
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🚀 Features",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface // Adjusted for surfaceContainer
                            )

                            FeatureItem(
                                icon = "🎬",
                                title = "Multiple Platforms",
                                description = "YouTube, Instagram, TikTok, Twitter supported"
                            )

                            FeatureItem(
                                icon = "⚙️",
                                title = "Quality Selection",
                                description = "Choose video quality and audio format"
                            )

                            FeatureItem(
                                icon = "📱",
                                title = "Quick Paste",
                                description = "Use floating paste button or quick button for direct Download"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // FAB is now part of the Scaffold, so removed from here
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        "App Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Nosved",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        actions = {
            HomeTopBarActions(
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToSettings = onNavigateToSettings,
                viewModel = viewModel
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun HomeTopBarActions(
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel
) {
    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())

    val runningCount = remember(allDownloads) {
        allDownloads.count {
            it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                    it.status == com.devson.nosved.data.DownloadStatus.QUEUED
        }
    }

    // Downloads button with badge
    if (runningCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        "$runningCount",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            IconButton(onClick = onNavigateToDownloads) {
                Icon(Icons.Default.CloudDownload, "Downloads")
            }
        }
    } else {
        IconButton(onClick = onNavigateToDownloads) {
            Icon(Icons.Default.CloudDownload, "Downloads")
        }
    }

    // Settings button
    IconButton(onClick = onNavigateToSettings) {
        Icon(Icons.Default.Settings, "Settings")
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.width(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface // Adjusted for surfaceContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Use onSurfaceVariant
            )
        }
    }
}