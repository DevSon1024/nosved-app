package com.devson.nosved.ui.navigation

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
import androidx.navigation.compose.*
import com.devson.nosved.ui.screens.*
import com.devson.nosved.ui.screens.settings.AdvancedSettingsScreen
import com.devson.nosved.ui.screens.settings.AppVersionScreen
import com.devson.nosved.ui.screens.settings.AppearanceSettingsScreen
import com.devson.nosved.ui.screens.settings.CreditsScreen
import com.devson.nosved.ui.screens.settings.QualitySettingsScreen
import com.devson.nosved.viewmodel.MainViewModel

@Composable
fun MainContent(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    val videoInfo by mainViewModel.videoInfo.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()

    LaunchedEffect(videoInfo, isLoading) {
        if (videoInfo != null && !isLoading) {
            navController.navigate(Screen.VideoInfo.route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination != Screen.VideoInfo.route) {
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
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                when (targetState.destination.route) {
                    Screen.Home.route -> slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, 50))

                    Screen.VideoInfo.route -> slideInVertically(
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
                    Screen.Home.route -> slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(150))

                    Screen.VideoInfo.route -> slideOutVertically(
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
                    Screen.Home.route -> slideInHorizontally(
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
                    Screen.VideoInfo.route -> slideOutVertically(
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
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = mainViewModel,
                    navController = navController,
                    onNavigateToDownloads = {
                        navController.navigate(Screen.Downloads.route) { launchSingleTop = true }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.VideoInfo.route) {
                VideoInfoScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.navigateUp() }
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(mainViewModel)
            }
            composable(Screen.AppVersion.route) {
                AppVersionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = mainViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQualitySettings = { navController.navigate(Screen.QualitySettings.route) },
                    onNavigateToAdvancedSettings = { navController.navigate(Screen.AdvancedSettings.route) },
                    onNavigateToAppVersion = { navController.navigate(Screen.AppVersion.route) },
                    onNavigateToCredits = { navController.navigate(Screen.Credits.route) },
                    onNavigateToAppearanceSettings = { navController.navigate(Screen.AppearanceSettings.route) }
                )
            }
            composable(Screen.Credits.route) {
                CreditsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.QualitySettings.route) {
                QualitySettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.AdvancedSettings.route) {
                AdvancedSettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.AppearanceSettings.route) {
                AppearanceSettingsScreen(onNavigateBack = { navController.popBackStack() })
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
            selected = currentDestination == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) }
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
            selected = currentDestination == Screen.Downloads.route,
            onClick = { onNavigate(Screen.Downloads.route) }
        )
    }
}
