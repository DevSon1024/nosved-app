package com.devson.nosved

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devson.nosved.ui.theme.NosvedTheme
import com.devson.nosved.ui.screens.HomeScreen
import com.devson.nosved.ui.screens.VideoInfoScreen
import com.devson.nosved.ui.screens.DownloadsScreen
import com.devson.nosved.ui.screens.SettingsScreen
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("NosvedApp", "Failed to initialize youtubedl-android", e)
        }

        askNotificationPermission()

        setContent {
            NosvedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    // Listen for video info changes and navigate automatically
    val videoInfo by viewModel.videoInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(videoInfo) {
        // Navigate to VideoInfo screen when video info is successfully fetched
        if (videoInfo != null && !isLoading) {
            navController.navigate("video_info") {
                // Don't add to back stack if we're already there
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            // Show back button only on VideoInfo screen
            if (currentDestination == "video_info") {
                TopAppBar(
                    title = { Text("Video Details") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Home"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                // Regular top bar for other screens
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Nosved",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
                        val runningDownloads = allDownloads.count {
                            it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                                    it.status == com.devson.nosved.data.DownloadStatus.QUEUED
                        }

                        if (runningDownloads > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("$runningDownloads")
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = {
                                        navController.navigate("downloads") {
                                            launchSingleTop = true
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Active Downloads"
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        bottomBar = {
            // Hide bottom navigation on VideoInfo screen for cleaner look
            if (currentDestination != "video_info") {
                EnhancedBottomNavigation(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        if (currentDestination != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    },
                    onQuickDownload = {
                        viewModel.pasteFromClipboard()
                    },
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable("video_info") {
                VideoInfoScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigateUp() }
                )
            }
            composable("downloads") {
                DownloadsScreen(viewModel)
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}

@Composable
fun EnhancedBottomNavigation(
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    onQuickDownload: () -> Unit,
    viewModel: MainViewModel
) {
    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
    val runningCount = allDownloads.count {
        it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                it.status == com.devson.nosved.data.DownloadStatus.QUEUED
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // Home Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = if (currentDestination == "home")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "Home",
                    color = if (currentDestination == "home")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentDestination == "home",
            onClick = { onNavigate("home") }
        )

        // Central Quick Download Button
        NavigationBarItem(
            icon = {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Quick Download",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            label = {
                Text(
                    "Quick Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            selected = false,
            onClick = onQuickDownload
        )

        // Downloads Tab
        NavigationBarItem(
            icon = {
                if (runningCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text("$runningCount")
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = if (currentDestination == "downloads")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = if (currentDestination == "downloads")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            label = {
                Text(
                    "Downloads",
                    color = if (currentDestination == "downloads")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentDestination == "downloads",
            onClick = { onNavigate("downloads") }
        )
    }
}