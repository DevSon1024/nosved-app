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
import com.devson.nosved.ui.screens.settings.SubtitleScreen
import com.devson.nosved.ui.screens.FormatSelectionScreen
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
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (targetState.destination.route == Screen.VideoInfo.route) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (initialState.destination.route == Screen.VideoInfo.route) {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(350, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(250))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(400, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                if (targetState.destination.route == Screen.VideoInfo.route) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            popExitTransition = {
                if (initialState.destination.route == Screen.VideoInfo.route) {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(350, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(250))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(300))
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
                    onBack = { navController.navigateUp() },
                    onNavigateToFormatSelection = {
                        navController.navigate(Screen.FormatSelection.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.navigateUp() }
                )
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
                QualitySettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSubtitleSettings = { navController.navigate(Screen.SubtitleSettings.route) }
                )
            }
            composable(Screen.AdvancedSettings.route) {
                AdvancedSettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.AppearanceSettings.route) {
                AppearanceSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SubtitleSettings.route) {
                SubtitleScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.FormatSelection.route) {
                FormatSelectionScreen(
                    viewModel = mainViewModel,
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}
