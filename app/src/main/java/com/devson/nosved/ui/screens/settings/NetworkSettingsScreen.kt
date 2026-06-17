package com.devson.nosved.ui.screens.settings

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devson.nosved.data.QualityPreferences
import com.devson.nosved.util.CookieExtractor
import com.devson.nosved.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class SubPage {
    MAIN,
    COOKIES,
    WEBVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qualityPrefs = remember { QualityPreferences(context) }
    val sharedPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    // Screen State Machine
    var currentSubPage by remember { mutableStateOf(SubPage.MAIN) }
    var activeCookieUrl by remember { mutableStateOf("https://www.youtube.com") }

    // Preferences
    val limitSpeed by viewModel.limitSpeed.collectAsState()
    val maxSpeedKb by viewModel.maxSpeedKb.collectAsState()
    val downloadUsingCellular by viewModel.downloadUsingCellular.collectAsState()
    val useAria2c by viewModel.useAria2c.collectAsState()
    val multiThreadedDownloadThreads by viewModel.multiThreadedDownloadThreads.collectAsState()
    val forceIpv4 by viewModel.forceIpv4.collectAsState()
    
    val useProxy by viewModel.useProxy.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()
    val enableCookies by qualityPrefs.enableCookies.collectAsState(initial = false)

    // Dialog States
    var showRateLimitDialog by remember { mutableStateOf(false) }
    var showThreadSliderDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }

    // Cookie Profile URLs
    var cookieProfileUrls by remember {
        mutableStateOf(
            sharedPrefs.getStringSet("cookie_profile_urls", null)?.toList()
                ?: listOf("https://www.youtube.com", "https://www.instagram.com")
        )
    }

    var showAddCookieProfileDialog by remember { mutableStateOf(false) }
    var showManageCookieProfileDialog by remember { mutableStateOf(false) }
    var selectedCookieProfileUrl by remember { mutableStateOf("") }

    // Statistics
    var cookiesFileCount by remember { mutableIntStateOf(CookieExtractor.getCookiesFileCount(context)) }

    // Intercept back button for sub-pages
    BackHandler(enabled = currentSubPage != SubPage.MAIN) {
        if (currentSubPage == SubPage.WEBVIEW) {
            currentSubPage = SubPage.COOKIES
        } else {
            currentSubPage = SubPage.MAIN
        }
    }

    when (currentSubPage) {
        SubPage.MAIN -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Network", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // General Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "General",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )

                            ClickableSwitchSettingCard(
                                title = "Rate limit",
                                subtitle = if (limitSpeed) "Limit: ${maxSpeedKb} KB/s" else "Limit the maximum download speed",
                                checked = limitSpeed,
                                icon = Icons.Default.Speed,
                                onClick = { showRateLimitDialog = true },
                                onToggle = { viewModel.setLimitSpeed(it) }
                            )

                            SimpleSwitchSettingCard(
                                title = "Download using cellular",
                                subtitle = "Allow downloading media when connected to metered networks",
                                checked = downloadUsingCellular,
                                icon = Icons.Default.SignalCellularAlt,
                                onToggle = { viewModel.setDownloadUsingCellular(it) }
                            )
                        }
                    }

                    // Advanced Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Advanced",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )

                            SimpleSwitchSettingCard(
                                title = "Aria2",
                                subtitle = "Use aria2c as the external downloader",
                                checked = useAria2c,
                                icon = Icons.Default.Bolt,
                                onToggle = { viewModel.setUseAria2c(it) }
                            )

                            ClickableSwitchSettingCard(
                                title = "Proxy",
                                subtitle = if (useProxy && proxyUrl.isNotEmpty()) "Proxy: $proxyUrl" else "Use proxy for internet connections",
                                checked = useProxy,
                                icon = Icons.Default.VpnKey,
                                onClick = { showProxyDialog = true },
                                onToggle = { viewModel.setUseProxy(it) }
                            )

                            ClickableSettingCard(
                                title = "Multi-threaded download",
                                subtitle = "Download more parts of M3U8/MPD videos in parallel",
                                enabled = !useAria2c,
                                icon = Icons.Default.OfflineBolt,
                                onClick = { showThreadSliderDialog = true }
                            )

                            SimpleSwitchSettingCard(
                                title = "Force IPv4",
                                subtitle = "Make all connections via IPv4",
                                checked = forceIpv4,
                                icon = Icons.Default.SwapHoriz,
                                onToggle = { viewModel.setForceIpv4(it) }
                            )

                            ClickableSwitchSettingCard(
                                title = "Cookies",
                                subtitle = if (enableCookies) "Use cookie file ($cookiesFileCount cookies loaded)" else "Use Netscape formatted cookies for downloads",
                                checked = enableCookies,
                                icon = Icons.Default.Cookie,
                                onClick = {
                                    cookiesFileCount = CookieExtractor.getCookiesFileCount(context)
                                    currentSubPage = SubPage.COOKIES
                                },
                                onToggle = { scope.launch { qualityPrefs.setEnableCookies(it) } }
                            )
                        }
                    }
                }
            }
        }

        SubPage.COOKIES -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Cookies", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { currentSubPage = SubPage.MAIN }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val count = CookieExtractor.extractAndSaveCookies(context)
                                    withContext(Dispatchers.Main) {
                                        cookiesFileCount = count
                                        Toast.makeText(context, "Extracted $count cookies from database!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Refresh, "Refresh from system WebView")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SimpleSwitchSettingCard(
                            title = "Use cookies",
                            subtitle = "Enable cookie injection for private/restricted downloads",
                            checked = enableCookies,
                            icon = Icons.Default.Cookie,
                            onToggle = { scope.launch { qualityPrefs.setEnableCookies(it) } }
                        )
                    }

                    item {
                        Text(
                            text = "Websites",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    items(cookieProfileUrls) { profileUrl ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCookieProfileUrl = profileUrl
                                    showManageCookieProfileDialog = true
                                }
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        text = profileUrl.substringAfter("://").substringBefore("/"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = profileUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAddCookieProfileDialog = true }
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Add website",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cookies inside database: $cookiesFileCount. Log in to your account inside the built-in browser window to fetch Netscape cookies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        SubPage.WEBVIEW -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(activeCookieUrl.substringAfter("://").substringBefore("/"), maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = { currentSubPage = SubPage.COOKIES }) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    // Flush the Webview CookieManager cookies
                                    CookieManager.getInstance().flush()
                                    val count = CookieExtractor.extractAndSaveCookies(context)
                                    withContext(Dispatchers.Main) {
                                        cookiesFileCount = count
                                        Toast.makeText(context, "Extracted $count cookies!", Toast.LENGTH_SHORT).show()
                                        currentSubPage = SubPage.COOKIES
                                    }
                                }
                            }) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                loadUrl(activeCookieUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Rate Limit dialog
    if (showRateLimitDialog) {
        RateLimitSpeedDialog(
            currentSpeed = maxSpeedKb,
            onDismiss = { showRateLimitDialog = false },
            onConfirm = { speed ->
                viewModel.setMaxSpeedKb(speed)
                showRateLimitDialog = false
            }
        )
    }

    // Multi-threaded dialog
    if (showThreadSliderDialog) {
        ThreadSliderDialog(
            currentThreads = multiThreadedDownloadThreads,
            onDismiss = { showThreadSliderDialog = false },
            onConfirm = { threads ->
                viewModel.setMultiThreadedDownloadThreads(threads)
                showThreadSliderDialog = false
            }
        )
    }

    // Proxy dialog
    if (showProxyDialog) {
        ProxyConfigDialog(
            currentProxyUrl = proxyUrl,
            onDismiss = { showProxyDialog = false },
            onConfirm = { url ->
                viewModel.setProxyUrl(url)
                showProxyDialog = false
            }
        )
    }

    // Add Cookie profile dialog
    if (showAddCookieProfileDialog) {
        var newUrlText by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showAddCookieProfileDialog = false },
            title = { Text("Add website", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newUrlText,
                    onValueChange = { newUrlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUrlText.startsWith("http://") || newUrlText.startsWith("https://")) {
                            val updatedList = cookieProfileUrls + newUrlText
                            cookieProfileUrls = updatedList
                            sharedPrefs.edit().putStringSet("cookie_profile_urls", updatedList.toSet()).apply()
                            showAddCookieProfileDialog = false
                        } else {
                            Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCookieProfileDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Manage Cookie profile dialog
    if (showManageCookieProfileDialog) {
        AlertDialog(
            onDismissRequest = { showManageCookieProfileDialog = false },
            title = { Text("Manage cookies", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Website: $selectedCookieProfileUrl",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeCookieUrl = selectedCookieProfileUrl
                        currentSubPage = SubPage.WEBVIEW
                        showManageCookieProfileDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in & generate")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val updatedList = cookieProfileUrls.filter { it != selectedCookieProfileUrl }
                        cookieProfileUrls = updatedList
                        sharedPrefs.edit().putStringSet("cookie_profile_urls", updatedList.toSet()).apply()
                        showManageCookieProfileDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete site profile")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun SimpleSwitchSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onToggle(!checked) } else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .alpha(if (enabled) 1f else 0.38f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (checked && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ClickableSwitchSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                    .clickable(enabled = enabled) { onClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (checked && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "TAP TO SETUP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ClickableSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RateLimitSpeedDialog(
    currentSpeed: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var speedText by remember { mutableStateOf(currentSpeed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Rate limit",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set the maximum download speed in KB/s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = speedText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            speedText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Limit speed (KB/s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalSpeed = if (speedText.isBlank()) "1024" else speedText
                    onConfirm(finalSpeed)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun ProxyConfigDialog(
    currentProxyUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentProxyUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Proxy Settings",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter proxy address (e.g. http://proxy.example.com:8080 or socks5://127.0.0.1:1080)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Proxy URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(urlText)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun ThreadSliderDialog(
    currentThreads: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val steps = listOf(1, 8, 16, 24)
    val initialIndex = steps.indexOf(currentThreads).coerceAtLeast(0)
    var selectedIndex by remember { mutableStateOf(initialIndex) }
    val threadCount = steps[selectedIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.OfflineBolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Multi-threaded download",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$threadCount thread(s) would be used to download DASH/HLS native video concurrently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { selectedIndex = it.roundToInt() },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(steps[selectedIndex]) }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
