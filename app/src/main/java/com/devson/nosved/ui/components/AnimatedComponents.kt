package com.devson.nosved.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.data.DownloadStatus

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedProgressCard(
    download: DownloadEntity,
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = download.progress / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    val scaleAnimation by animateFloatAsState(
        targetValue = when (download.status) {
            DownloadStatus.DOWNLOADING -> 1.02f
            DownloadStatus.COMPLETED -> 1.01f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val cardElevation by animateDpAsState(
        targetValue = when (download.status) {
            DownloadStatus.DOWNLOADING -> 8.dp
            DownloadStatus.COMPLETED -> 6.dp
            DownloadStatus.FAILED -> 4.dp
            else -> 4.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )

    Card(
        modifier = modifier
            .scale(scaleAnimation)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = when (download.status) {
                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    AnimatedVisibility(
                        visible = !download.uploader.isNullOrEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = download.uploader ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AnimatedStatusIndicator(status = download.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    AnimatedProgressIndicator(
                        progress = animatedProgress,
                        text = "${download.progress}%"
                    )
                }
                DownloadStatus.COMPLETED -> {
                    AnimatedSuccessIndicator()
                }
                DownloadStatus.FAILED -> {
                    AnimatedErrorIndicator(onRetry = onRetry)
                }
                else -> {
                    AnimatedQueuedIndicator()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!download.videoFormat.isNullOrEmpty()) {
                    AnimatedChip(text = download.videoFormat!!)
                }
                if (!download.audioFormat.isNullOrEmpty()) {
                    AnimatedChip(text = download.audioFormat!!)
                }
            }
        }
    }
}

@Composable
fun AnimatedProgressIndicator(
    progress: Float,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .animateContentSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnimatedStatusIndicator(
    status: DownloadStatus,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "status_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                rotationZ = if (status == DownloadStatus.DOWNLOADING) rotation else 0f
            }
    ) {
        when (status) {
            DownloadStatus.DOWNLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            DownloadStatus.COMPLETED -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.Green,
                        radius = size.minDimension / 2
                    )
                    drawCheckmark(this)
                }
            }
            DownloadStatus.FAILED -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.Red,
                        radius = size.minDimension / 2
                    )
                    drawX(this)
                }
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.Gray,
                        radius = size.minDimension / 2
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedSuccessIndicator() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✅ Download completed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AnimatedErrorIndicator(
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "❌ Download failed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun AnimatedQueuedIndicator() {
    val alpha by rememberInfiniteTransition(label = "queued_alpha").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = "⏳ Queued for download",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    )
}

@Composable
fun AnimatedChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chip_scale"
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Helper functions for drawing icons
private fun drawCheckmark(drawScope: DrawScope) {
    val size = drawScope.size
    val strokeWidth = size.minDimension * 0.1f

    drawScope.drawLine(
        color = Color.White,
        start = Offset(size.width * 0.25f, size.height * 0.5f),
        end = Offset(size.width * 0.45f, size.height * 0.7f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawScope.drawLine(
        color = Color.White,
        start = Offset(size.width * 0.45f, size.height * 0.7f),
        end = Offset(size.width * 0.75f, size.height * 0.3f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun drawX(drawScope: DrawScope) {
    val size = drawScope.size
    val strokeWidth = size.minDimension * 0.1f
    val padding = size.minDimension * 0.25f

    drawScope.drawLine(
        color = Color.White,
        start = Offset(padding, padding),
        end = Offset(size.width - padding, size.height - padding),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawScope.drawLine(
        color = Color.White,
        start = Offset(size.width - padding, padding),
        end = Offset(padding, size.height - padding),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
