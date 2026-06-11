package com.devson.nosved.ui.common.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devson.nosved.ui.model.DownloadCounts
import com.devson.nosved.ui.model.DownloadTabType

@Composable
fun DownloadTabRow(
    selectedTab: DownloadTabType,
    onTabSelected: (DownloadTabType) -> Unit,
    downloadCounts: DownloadCounts,
    modifier: Modifier = Modifier
) {
    val tabs = remember(downloadCounts) {
        listOf(
            DownloadTabType.ALL to ("All" to downloadCounts.all),
            DownloadTabType.ACTIVE to ("Active" to downloadCounts.active),
            DownloadTabType.COMPLETED to ("Completed" to downloadCounts.completed),
            DownloadTabType.FAILED to ("Failed" to downloadCounts.failed)
        )
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs) { (tabType, titleAndCount) ->
            val (title, count) = titleAndCount
            val isSelected = selectedTab == tabType

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(200),
                label = "tabContainerColor"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(200),
                label = "tabContentColor"
            )

            Surface(
                color = containerColor,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(tabType) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = contentColor
                    )

                    if (count > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
