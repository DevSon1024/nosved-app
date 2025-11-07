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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

@Composable
fun MainContent(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    val videoInfo by mainViewModel.videoInfo.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()

    LaunchedEffect(videoInfo, isLoading) {
        if (videoInfo != null && !isLoading) {
            navController.navigate("video_info") {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination != "video_info") {
                ModernBottomNavigation(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        if (currentDestination != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    },
                    onQuickDownload = { mainViewModel.pasteFromClipboard() },
                    viewModel = mainViewModel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            // Modern lightweight animations
            enterTransition = {
                when (targetState.destination.route) {
                    "home" -> slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, 50))

                    "video_info" -> slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))

                    else -> fadeIn(animationSpec = tween(200)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                }
            },
            exitTransition = {
                when (initialState.destination.route) {
                    "home" -> slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(150))

                    "video_info" -> slideOutVertically(
                        targetOffsetY = { it / 4 },
                        animationSpec = tween(250, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(150))

                    else -> fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(150, easing = FastOutLinearInEasing)
                    )
                }
            },
            popEnterTransition = {
                when (targetState.destination.route) {
                    "home" -> slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, 50))

                    else -> fadeIn(animationSpec = tween(200)) + scaleIn(
                        initialScale = 0.98f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                }
            },
            popExitTransition = {
                when (initialState.destination.route) {
                    "video_info" -> slideOutVertically(
                        targetOffsetY = { it / 4 },
                        animationSpec = tween(250, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(150))

                    else -> fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.98f,
                        animationSpec = tween(150, easing = FastOutLinearInEasing)
                    )
                }
            }
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = mainViewModel,
                    navController = navController,
                    onNavigateToDownloads = {
                        navController.navigate("downloads") { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings") { launchSingleTop = true }
                    }
                )
            }
            composable("video_info") {
                VideoInfoScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.navigateUp() }
                )
            }
            composable("downloads") {
                DownloadsScreen(mainViewModel)
            }
            composable("app_version") {
                AppVersionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = mainViewModel // Pass your existing MainViewModel
                )
            }

// Update the settings screen composable to include the new navigation
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQualitySettings = { navController.navigate("quality_settings") },
                    onNavigateToAdvancedSettings = { navController.navigate("advanced_settings") },
                    onNavigateToAppVersion = { navController.navigate("app_version") } // Add this line
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

@Composable
fun ModernBottomNavigation(
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Download,
                            "Quick Download",
                            modifier = Modifier.size(20.dp)
                        )
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