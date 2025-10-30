package com.devson.nosved.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.model.DownloadTabType

@Composable
fun DownloadEmptyState(
    tabType: DownloadTabType,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (tabType) {
        DownloadTabType.ALL -> Triple(
            Icons.Default.DownloadDone,
            "No downloads yet",
            "Your download history will appear here once you start downloading videos"
        )
        DownloadTabType.ACTIVE -> Triple(
            Icons.Default.Download,
            "No active downloads",
            "Downloads currently in progress will be shown here"
        )
        DownloadTabType.COMPLETED -> Triple(
            Icons.Default.CheckCircle,
            "No completed downloads",
            "Successfully downloaded videos will appear here"
        )
        DownloadTabType.FAILED -> Triple(
            Icons.Default.Error,
            "No failed downloads",
            "Downloads that encounter errors will be listed here"
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}
