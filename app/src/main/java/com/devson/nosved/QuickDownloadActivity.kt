package com.devson.nosved

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.ui.screens.QuickDownloadSheet
import com.devson.nosved.ui.theme.NosvedPlayerTheme
import com.devson.nosved.viewmodel.QuickDownloadViewModel
import com.devson.nosved.viewmodel.SettingsViewModel

class QuickDownloadActivity : ComponentActivity() {

    private val quickViewModel: QuickDownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            else -> null
        }

        if (sharedUrl.isNullOrBlank()) {
            finish()
            return
        }

        quickViewModel.processUrl(sharedUrl)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val forceDark by settingsViewModel.isDarkTheme.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()
            val selectedPalette by settingsViewModel.selectedPalette.collectAsState()
            val isNavBarTransparent by settingsViewModel.isNavBarTransparent.collectAsState()
            val isAmoledTheme by settingsViewModel.isAmoledTheme.collectAsState()

            // Close Activity the moment download is queued
            val shouldDismiss by quickViewModel.dismissSignal.collectAsState()
            LaunchedEffect(shouldDismiss) {
                if (shouldDismiss) finish()
            }

            NosvedPlayerTheme(
                forceDark = forceDark,
                dynamicColor = dynamicColor,
                palette = selectedPalette,
                isNavBarTransparent = isNavBarTransparent,
                isAmoledTheme = isAmoledTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                ) {
                    QuickDownloadSheet(
                        viewModel = quickViewModel,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}
