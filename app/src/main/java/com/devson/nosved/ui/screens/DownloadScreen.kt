package com.devson.nosved.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.devson.nosved.viewmodel.MainViewModel
import com.devson.nosved.data.DownloadEntity
import com.devson.nosved.ui.common.components.DownloadEmptyState
import com.devson.nosved.ui.common.components.DownloadItemCard
import com.devson.nosved.ui.common.components.DownloadTabRow
import com.devson.nosved.ui.model.DownloadAction
import com.devson.nosved.ui.model.DownloadCounts
import com.devson.nosved.ui.model.DownloadTabType
import java.io.File
import java.net.URI

private fun extractDomain(url: String): String {
    return try {
        val uri = URI(url)
        val host = uri.host?.lowercase() ?: ""
        val cleanHost = if (host.startsWith("www.")) host.substring(4) else host
        when {
            cleanHost.contains("youtube") || cleanHost.contains("youtu.be") -> "YouTube"
            cleanHost.contains("instagram") -> "Instagram"
            cleanHost.contains("twitter") || cleanHost.contains("x.com") -> "Twitter / X"
            cleanHost.contains("tiktok") -> "TikTok"
            cleanHost.contains("facebook") || cleanHost.contains("fb.com") -> "Facebook"
            cleanHost.isBlank() -> "Others"
            else -> {
                val parts = cleanHost.split(".")
                if (parts.isNotEmpty()) {
                    parts[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    cleanHost
                }
            }
        }
    } catch (e: Exception) {
        "Others"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DownloadTabType.ALL) }
    var selectedDomainFilter by remember { mutableStateOf("All") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Collect download states
    val allDownloads by viewModel.allDownloads.collectAsState(initial = emptyList())
    val runningDownloads by viewModel.runningDownloads.collectAsState(initial = emptyList())
    val completedDownloads by viewModel.completedDownloads.collectAsState(initial = emptyList())
    val failedDownloads by viewModel.failedDownloads.collectAsState(initial = emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    // Selection state variables
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val isInSelectionMode = selectedIds.isNotEmpty()

    // Bottom sheet state variables
    var activeBottomSheetDownload by remember { mutableStateOf<DownloadEntity?>(null) }

    // Delete confirmation dialog state variables
    var showDeleteDialogForIds by remember { mutableStateOf<Set<String>?>(null) }
    var deleteDialogCheckboxState by remember { mutableStateOf(false) }

    // Physical back click handler
    BackHandler(enabled = isInSelectionMode) {
        selectedIds = emptySet()
    }

    // Calculate download counts
    val downloadCounts = remember(allDownloads, runningDownloads, completedDownloads, failedDownloads) {
        DownloadCounts(
            all = allDownloads.size,
            active = runningDownloads.size,
            completed = completedDownloads.size,
            failed = failedDownloads.size
        )
    }

    // Get base downloads based on selected status tab
    val baseDownloads = remember(selectedTab, allDownloads, runningDownloads, completedDownloads, failedDownloads) {
        when (selectedTab) {
            DownloadTabType.ALL -> allDownloads
            DownloadTabType.ACTIVE -> runningDownloads
            DownloadTabType.COMPLETED -> completedDownloads
            DownloadTabType.FAILED -> failedDownloads
        }
    }

    // Dynamically extract domains list based on downloads
    val domainsList = remember(allDownloads) {
        val extracted = allDownloads.map { extractDomain(it.url) }.distinct().filter { it.isNotBlank() }.sorted()
        listOf("All") + extracted
    }

    // Ensure selectedDomainFilter is valid if domainsList changes
    LaunchedEffect(domainsList) {
        if (!domainsList.contains(selectedDomainFilter)) {
            selectedDomainFilter = "All"
        }
    }

    // Filter base downloads by search query and domain
    val filteredDownloads = remember(baseDownloads, searchQuery, selectedDomainFilter) {
        baseDownloads.filter { download ->
            val matchesSearch = searchQuery.isBlank() || download.title.contains(searchQuery, ignoreCase = true)
            val matchesDomain = selectedDomainFilter == "All" || extractDomain(download.url) == selectedDomainFilter
            matchesSearch && matchesDomain
        }
    }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection"
                            )
                        }
                    },
                    actions = {
                        val allFilteredIds = filteredDownloads.map { it.id }.toSet()
                        val isAllSelected = selectedIds.containsAll(allFilteredIds)
                        IconButton(
                            onClick = {
                                selectedIds = if (isAllSelected) {
                                    selectedIds - allFilteredIds
                                } else {
                                    selectedIds + allFilteredIds
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all"
                            )
                        }
                        IconButton(
                            onClick = {
                                deleteDialogCheckboxState = false
                                showDeleteDialogForIds = selectedIds
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search downloads...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "Downloads",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search downloads"
                                )
                            }
                        }
                        IconButton(onClick = { /* TODO: Implement sort */ }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort downloads"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Tab Row (Status filter)
            DownloadTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                downloadCounts = downloadCounts
            )

            // Horizontal Domain filter row (YouTube, Instagram, etc. extracted dynamically)
            if (domainsList.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(domainsList) { domain ->
                        FilterChip(
                            selected = selectedDomainFilter == domain,
                            onClick = { selectedDomainFilter = domain },
                            label = { Text(domain) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Content Area with transparent navigation bar support
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredDownloads.isEmpty()) {
                    DownloadEmptyState(tabType = selectedTab)
                } else {
                    DownloadList(
                        downloads = filteredDownloads,
                        downloadProgress = downloadProgress,
                        selectedIds = selectedIds,
                        isInSelectionMode = isInSelectionMode,
                        onToggleSelection = { id ->
                            selectedIds = if (selectedIds.contains(id)) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                        },
                        onShowBottomSheet = { download ->
                            activeBottomSheetDownload = download
                        },
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = paddingValues.calculateBottomPadding() + 24.dp
                        ),
                        onAction = { action ->
                            handleDownloadAction(action, viewModel, context)
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialogForIds?.let { idsToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialogForIds = null },
            title = {
                Text(
                    text = if (idsToDelete.size > 1) "Remove Downloads" else "Remove Download",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (idsToDelete.size > 1) {
                            "Are you sure you want to remove the ${idsToDelete.size} selected downloads from your history?"
                        } else {
                            "Are you sure you want to remove this download from your history?"
                        }
                    )
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteDialogCheckboxState = !deleteDialogCheckboxState }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = deleteDialogCheckboxState,
                            onCheckedChange = { deleteDialogCheckboxState = it }
                        )
                        Text(
                            text = "Also delete downloaded files from storage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleteFiles = deleteDialogCheckboxState
                        
                        if (idsToDelete.size > 1) {
                            if (deleteFiles) {
                                viewModel.deleteDownloadsBulk(idsToDelete.toList())
                            } else {
                                viewModel.removeFromAppBulk(idsToDelete.toList())
                            }
                        } else {
                            idsToDelete.firstOrNull()?.let { id ->
                                if (deleteFiles) {
                                    viewModel.deleteDownload(id)
                                } else {
                                    viewModel.removeFromApp(id)
                                }
                            }
                        }
                        
                        selectedIds = selectedIds - idsToDelete
                        showDeleteDialogForIds = null
                        activeBottomSheetDownload = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogForIds = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modern M3 Actions Bottom Sheet
    activeBottomSheetDownload?.let { download ->
        ModalBottomSheet(
            onDismissRequest = { activeBottomSheetDownload = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    download.uploader?.let { uploader ->
                        Text(
                            text = uploader,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Video Link", download.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = download.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isPlayable = download.status == com.devson.nosved.data.DownloadStatus.COMPLETED && !download.filePath.isNullOrEmpty()
                    if (isPlayable) {
                        ListItem(
                            headlineContent = { Text("Play Video") },
                            leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    handleDownloadAction(DownloadAction.Play(download.filePath.orEmpty()), viewModel, context)
                                    activeBottomSheetDownload = null
                                }
                        )
                        ListItem(
                            headlineContent = { Text("Share Video File") },
                            leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    handleDownloadAction(DownloadAction.Share(download.filePath.orEmpty()), viewModel, context)
                                    activeBottomSheetDownload = null
                                }
                        )
                    }

                    ListItem(
                        headlineContent = { Text("Share Video Link") },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, download.url)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Link"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share link", Toast.LENGTH_SHORT).show()
                                }
                                activeBottomSheetDownload = null
                            }
                    )

                    ListItem(
                        headlineContent = { Text("Remove from History", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { 
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                deleteDialogCheckboxState = false
                                showDeleteDialogForIds = setOf(download.id)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadList(
    downloads: List<DownloadEntity>,
    downloadProgress: Map<String, com.devson.nosved.data.DownloadProgress>,
    selectedIds: Set<String>,
    isInSelectionMode: Boolean,
    onToggleSelection: (String) -> Unit,
    onShowBottomSheet: (DownloadEntity) -> Unit,
    contentPadding: PaddingValues,
    onAction: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(
            items = downloads,
            key = { it.id }
        ) { download ->
            DownloadItemCard(
                download = download,
                progress = downloadProgress[download.id],
                onAction = onAction,
                isSelected = selectedIds.contains(download.id),
                isInSelectionMode = isInSelectionMode,
                onToggleSelection = { onToggleSelection(download.id) },
                onShowBottomSheet = { onShowBottomSheet(download) }
            )
        }
    }
}

private fun handleDownloadAction(
    action: DownloadAction,
    viewModel: MainViewModel,
    context: Context
) {
    when (action) {
        is DownloadAction.Play -> playVideo(context, action.filePath)
        is DownloadAction.Share -> shareVideo(context, action.filePath)
        is DownloadAction.Retry -> viewModel.retryDownload(action.downloadId)
        is DownloadAction.Cancel -> viewModel.cancelDownload(action.downloadId)
        is DownloadAction.Delete -> viewModel.deleteDownload(action.downloadId)
        is DownloadAction.RemoveFromApp -> viewModel.removeFromApp(action.downloadId)
        is DownloadAction.Redownload -> viewModel.redownloadVideo(action.downloadId, action.sameQuality)
        is DownloadAction.Pause -> viewModel.pauseDownload(action.downloadId)
        is DownloadAction.Resume -> viewModel.resumeDownload(action.downloadId)
    }
}

private fun playVideo(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Play Video"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareVideo(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Video"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
