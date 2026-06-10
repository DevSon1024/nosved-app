package com.devson.nosved.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object VideoInfo : Screen("video_info")
    object Downloads : Screen("downloads")
    object AppVersion : Screen("app_version")
    object Settings : Screen("settings")
    object Credits : Screen("credits")
    object QualitySettings : Screen("quality_settings")
    object AdvancedSettings : Screen("advanced_settings")
    object AppearanceSettings : Screen("appearance_settings")
}
