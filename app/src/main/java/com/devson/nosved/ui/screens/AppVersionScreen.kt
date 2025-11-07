package com.devson.nosved.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.MainViewModel
import com.devson.nosved.util.YtDlpUpdater
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVersionScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appVersion by remember { mutableStateOf("Loading...") }
    var ytdlpVersion by remember { mutableStateOf("Loading...") }
    var lastUpdated by remember { mutableStateOf("Loading...") }
    var isUpdatingYtdlp by remember { mutableStateOf(false) }

    // Create YtDlpUpdater instance
    val ytdlpUpdater = remember { YtDlpUpdater(context.applicationContext as android.app.Application) }

    // Function to load version info
    fun loadVersionInfo() {
        scope.launch {
            appVersion = getAppVersion(context)

            withContext(Dispatchers.IO) {
                try {
                    // Get YT-DLP version
                    val version = try {
                        YoutubeDL.getInstance().version(context)
                    } catch (e: Exception) {
                        null
                    }

                    // Get last update time from preferences
                    val lastUpdateTime = ytdlpUpdater.getLastUpdateTime()

                    withContext(Dispatchers.Main) {
                        ytdlpVersion = version ?: ytdlpUpdater.getCurrentVersion()
                        lastUpdated = lastUpdateTime

                        // If still unknown, show a helpful message
                        if (ytdlpVersion == "Unknown") {
                            ytdlpVersion = "Not yet updated"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        ytdlpVersion = "Error loading version"
                        lastUpdated = "Unknown"
                    }
                }
            }
        }
    }

    // Load version information on first composition
    LaunchedEffect(Unit) {
        loadVersionInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Version") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Version Card
            item {
                VersionCard(
                    title = "App Version",
                    version = appVersion,
                    icon = Icons.Default.Android,
                    subtitle = "Nosved Video Downloader",
                    onUpdateClick = null
                )
            }

            // YT-DLP Version Card with Manual Update
            item {
                VersionCard(
                    title = "YT-DLP Version",
                    version = ytdlpVersion,
                    icon = Icons.Default.Download,
                    subtitle = "Last updated: $lastUpdated",
                    onUpdateClick = if (!isUpdatingYtdlp) {
                        {
                            isUpdatingYtdlp = true
                            scope.launch {
                                try {
                                    showToastAppVersion(context, "🔄 Updating YT-DLP...")

                                    withContext(Dispatchers.IO) {
                                        // Force update YT-DLP
                                        ytdlpUpdater.checkAndUpdate(force = true)
                                    }

                                    // Wait a moment for update to complete
                                    kotlinx.coroutines.delay(1500)

                                    // Reload version info
                                    loadVersionInfo()

                                    showToastAppVersion(context, "✅ YT-DLP updated successfully!")
                                } catch (e: Exception) {
                                    showToastAppVersion(context, "❌ Update failed: ${e.message}")
                                } finally {
                                    isUpdatingYtdlp = false
                                }
                            }
                        }
                    } else null,
                    isLoading = isUpdatingYtdlp
                )
            }

            // Version Info Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "About Updates",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            item {
                InfoCard(
                    icon = Icons.Default.Info,
                    title = "Automatic Updates",
                    description = "YT-DLP is automatically checked for updates when the app starts. You can also manually update it using the button above."
                )
            }

            item {
                InfoCard(
                    icon = Icons.Default.Schedule,
                    title = "Update Frequency",
                    description = "YT-DLP is updated frequently to support new sites and fix issues. Check for updates regularly for the best experience."
                )
            }

            item {
                InfoCard(
                    icon = Icons.Default.Security,
                    title = "Safe Updates",
                    description = "Updates are downloaded directly from the official YT-DLP repository and verified for security."
                )
            }
        }
    }
}

@Composable
fun VersionCard(
    title: String,
    version: String,
    icon: ImageVector,
    subtitle: String,
    onUpdateClick: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Update button for YT-DLP
                onUpdateClick?.let { onClick ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onClick) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Update",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${packageInfo.versionName} (${packageInfo.longVersionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

private fun showToastAppVersion(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
}