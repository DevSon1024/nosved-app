package com.devson.nosved

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.navigation.compose.*
import com.devson.nosved.ui.theme.NosvedTheme
import com.devson.nosved.ui.screens.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Simplified permission handling
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeDownloader()
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

    private fun initializeDownloader() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("NosvedApp", "Failed to initialize youtubedl-android", e)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    val videoInfo by viewModel.videoInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(videoInfo, isLoading) {
        if (videoInfo != null && !isLoading) {
            navController.navigate("video_info") {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            // Only show top bar on Home page
            if (currentDestination == "home") {
                OptimizedTopBar(
                    onNavigateToDownloads = {
                        navController.navigate("downloads") { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings") { launchSingleTop = true }
                    },
                    viewModel = viewModel
                )
            } else if (currentDestination == "video_info") {
                // Simple back navigation for video info
                TopAppBar(
                    title = { Text("Video Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentDestination != "video_info") {
                OptimizedBottomNavigation(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        if (currentDestination != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    },
                    onQuickDownload = { viewModel.pasteFromClipboard() },
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(200, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(200, easing = EaseInCubic)
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(200, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(200, easing = EaseInCubic)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) {
            composable("home") {
                HomeScreen(viewModel = viewModel, navController = navController)
            }
            composable("video_info") {
                VideoInfoScreen(viewModel = viewModel, onBack = { navController.navigateUp() })
            }
            composable("downloads") {
                DownloadsScreen(viewModel)
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToQualitySettings = { navController.navigate("quality_settings") },
                    onNavigateToAdvancedSettings = { navController.navigate("advanced_settings") }
                )
            }
            composable("quality_settings") {
                QualitySettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable("advanced_settings") {
                AdvancedSettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedTopBar(
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
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
            TopBarActions(
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToSettings = onNavigateToSettings,
                viewModel = viewModel
            )
        }
    )
}

@Composable
fun TopBarActions(
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

    if (runningCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text("$runningCount") }
            }
        ) {
            IconButton(onClick = onNavigateToDownloads) {
                Icon(Icons.Default.CloudDownload, "Downloads")
            }
        }
    }

    IconButton(onClick = onNavigateToSettings) {
        Icon(Icons.Default.Settings, "Settings")
    }
}

@Composable
fun OptimizedBottomNavigation(
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    onQuickDownload: () -> Unit,
    viewModel: MainViewModel
) {
    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())

    val runningCount = remember(allDownloads) {
        allDownloads.count {
            it.status == com.devson.nosved.data.DownloadStatus.DOWNLOADING ||
                    it.status == com.devson.nosved.data.DownloadStatus.QUEUED
        }
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
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            selected = currentDestination == "home",
            onClick = { onNavigate("home") }
        )

        // Central Quick Download Button
        NavigationBarItem(
            icon = {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = onQuickDownload,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Download, "Quick Download")
                    }
                }
            },
            label = { Text("Quick") },
            selected = false,
            onClick = onQuickDownload
        )

        // Downloads Tab
        NavigationBarItem(
            icon = {
                if (runningCount > 0) {
                    BadgedBox(
                        badge = { Badge { Text("$runningCount") } }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, "Downloads")
                    }
                } else {
                    Icon(Icons.AutoMirrored.Filled.List, "Downloads")
                }
            },
            label = { Text("Downloads") },
            selected = currentDestination == "downloads",
            onClick = { onNavigate("downloads") }
        )
    }
}
