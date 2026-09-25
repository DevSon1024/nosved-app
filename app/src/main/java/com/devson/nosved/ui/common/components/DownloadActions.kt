package com.devson.nosved.ui.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus
import com.devson.nosved.ui.model.DownloadAction

@Composable
fun DownloadActions(
    download: DownloadEntity,
    onAction: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Download options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // Context-aware menu items based on download status
            when (download.status) {
                DownloadStatus.COMPLETED -> {
                    val path = download.filePath
                    if (!path.isNullOrEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Play") },
                            onClick = {
                                onAction(DownloadAction.Play(path))
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                onAction(DownloadAction.Share(path))
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                    }
                }

                DownloadStatus.FAILED -> {
                    DropdownMenuItem(
                        text = { Text("Retry") },
                        onClick = {
                            onAction(DownloadAction.Retry(download.id))
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                }

                DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                    DropdownMenuItem(
                        text = { Text("Cancel") },
                        onClick = {
                            onAction(DownloadAction.Cancel(download.id))
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                }

                else -> {}
            }

            // Delete option (always available)
            DropdownMenuItem(
                text = {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onAction(DownloadAction.Delete(download.id))
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}
