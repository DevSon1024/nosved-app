package com.devson.nosved.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.model.DownloadCounts
import com.devson.nosved.ui.model.DownloadTabType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTabRow(
    selectedTab: DownloadTabType,
    onTabSelected: (DownloadTabType) -> Unit,
    downloadCounts: DownloadCounts,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        DownloadTabType.ALL to ("All" to downloadCounts.all),
        DownloadTabType.ACTIVE to ("Active" to downloadCounts.active),
        DownloadTabType.COMPLETED to ("Completed" to downloadCounts.completed),
        DownloadTabType.FAILED to ("Failed" to downloadCounts.failed)
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            // Custom indicator can be added here if needed
        }
    ) {
        tabs.forEachIndexed { index, (tabType, titleAndCount) ->
            val (title, count) = titleAndCount
            val isSelected = selectedTab == tabType

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tabType) },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (count > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                },
                                shape = CircleShape
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
