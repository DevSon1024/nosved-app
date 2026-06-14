package com.devson.nosved.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.viewmodel.MainViewModel
import com.devson.nosved.viewmodel.SettingsViewModel
import com.devson.nosved.util.YtDlpUpdater
import com.devson.nosved.util.YtDlpUpdateInterval
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVersionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCredits: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appVersion by remember { mutableStateOf("Loading...") }
    var ytdlpVersion by remember { mutableStateOf("Loading...") }
    var lastUpdated by remember { mutableStateOf("Loading...") }
    var isUpdatingYtdlp by remember { mutableStateOf(false) }

    val ytdlpUpdateChannel by settingsViewModel.ytdlpUpdateChannel.collectAsState()
    val ytdlpUpdateInterval by settingsViewModel.ytdlpUpdateInterval.collectAsState()

    var remoteVersion by remember { mutableStateOf<String?>(null) }
    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var showUpdateIntervalDialog by remember { mutableStateOf(false) }

    suspend fun fetchLatestRemoteVersion(channel: String): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val repo = if (channel == "NIGHTLY") "yt-dlp/yt-dlp-nightly-builds" else "yt-dlp/yt-dlp"
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nosved-App")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            json.optString("tag_name")?.trim()
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun checkRemoteVersion() {
        scope.launch {
            isCheckingForUpdate = true
            try {
                remoteVersion = fetchLatestRemoteVersion(ytdlpUpdateChannel)
            } catch (e: Exception) {
                remoteVersion = null
            } finally {
                isCheckingForUpdate = false
            }
        }
    }

    val isLatestVersion = remember(ytdlpVersion, remoteVersion) {
        val cleanLocal = ytdlpVersion.trim()
        val cleanRemote = remoteVersion?.trim()
        cleanRemote != null && cleanLocal == cleanRemote
    }

    // Create YtDlpUpdater instance
    val ytdlpUpdater = remember { YtDlpUpdater(context.applicationContext as Application) }

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

    // Load version information on channel change
    LaunchedEffect(ytdlpUpdateChannel) {
        loadVersionInfo()
        checkRemoteVersion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Version Info", fontWeight = FontWeight.SemiBold) },
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
            // App Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Nosved Video Downloader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "v$appVersion",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // YT-DLP Version Card with Manual Update & Update Channel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "YT-DLP Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version: $ytdlpVersion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Last updated: $lastUpdated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Update Channel Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = "Update Channel",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Nightly builds get faster fixes but may be less stable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = ytdlpUpdateChannel == "STABLE",
                                    onClick = { settingsViewModel.setYtdlpUpdateChannel("STABLE") },
                                    label = { Text("Stable") }
                                )
                                FilterChip(
                                    selected = ytdlpUpdateChannel == "NIGHTLY",
                                    onClick = { settingsViewModel.setYtdlpUpdateChannel("NIGHTLY") },
                                    label = { Text("Nightly") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto Update Check Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showUpdateIntervalDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = "Auto Update Check",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Check for engine updates automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ytdlpUpdateInterval.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isLatestVersion) {
                            // Manual Update Action Button
                            Button(
                                onClick = {
                                    isUpdatingYtdlp = true
                                    scope.launch {
                                        try {
                                            showToastAppVersion(context, "Updating YT-DLP...")
                                            withContext(Dispatchers.IO) {
                                                ytdlpUpdater.checkAndUpdate(force = true)
                                            }
                                            delay(1500)
                                            loadVersionInfo()
                                            checkRemoteVersion()
                                            showToastAppVersion(context, "YT-DLP updated successfully!")
                                        } catch (e: Exception) {
                                            showToastAppVersion(context, "Update failed: ${e.message}")
                                        } finally {
                                            isUpdatingYtdlp = false
                                        }
                                    }
                                },
                                enabled = !isUpdatingYtdlp,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isUpdatingYtdlp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Updating...")
                                } else {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update Engine")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Engine is up to date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Info & Support Section (Credits, Repo, Report Issue)
            item {
                Text(
                    text = "Info & Support",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AppVersionItemRow(
                            icon = Icons.Default.Favorite,
                            title = "Credits",
                            subtitle = "Acknowledgements and licenses",
                            onClick = onNavigateToCredits
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        AppVersionItemRow(
                            icon = Icons.Default.Code,
                            title = "Official Repo",
                            subtitle = "View on GitHub",
                            onClick = { openGitHub(context) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        AppVersionItemRow(
                            icon = Icons.Default.BugReport,
                            title = "Report Issue",
                            subtitle = "Found a bug? Let us know",
                            onClick = { openIssueTracker(context) }
                        )
                    }
                }
            }

            // About Updates Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "About Engine Updates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
    if (showUpdateIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateIntervalDialog = false },
            title = { Text("Auto Update Check", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select how frequently the app checks for yt-dlp engine updates automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    YtDlpUpdateInterval.entries.forEach { interval ->
                        val isSelected = interval == ytdlpUpdateInterval
                        Surface(
                            onClick = {
                                settingsViewModel.setYtdlpUpdateInterval(interval)
                                showUpdateIntervalDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = interval.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        settingsViewModel.setYtdlpUpdateInterval(interval)
                                        showUpdateIntervalDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpdateIntervalDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
}

@Composable
fun AppVersionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
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
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun openGitHub(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
    }
}

private fun openIssueTracker(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DevSon1024/nosved-app/issues"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
    }
}