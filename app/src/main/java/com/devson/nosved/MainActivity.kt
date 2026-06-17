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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.ui.navigation.MainContent
import com.devson.nosved.ui.theme.NosvedPlayerTheme
import com.devson.nosved.viewmodel.MainViewModel
import com.devson.nosved.viewmodel.SettingsViewModel
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Simplified permission handling
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val forceDark by settingsViewModel.isDarkTheme.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()
            val selectedPalette by settingsViewModel.selectedPalette.collectAsState()
            val isNavBarTransparent by settingsViewModel.isNavBarTransparent.collectAsState()
            val isAmoledTheme by settingsViewModel.isAmoledTheme.collectAsState()

            NosvedPlayerTheme(
                forceDark = forceDark,
                dynamicColor = dynamicColor,
                palette = selectedPalette,
                isNavBarTransparent = isNavBarTransparent,
                isAmoledTheme = isAmoledTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(viewModel)
                }
            }
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