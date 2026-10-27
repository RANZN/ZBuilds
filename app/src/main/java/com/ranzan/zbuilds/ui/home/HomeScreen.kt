package com.ranzan.zbuilds.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.ui.components.AddAppSourceScreen
import com.ranzan.zbuilds.ui.components.TopSearchBar
import com.ranzan.zbuilds.ui.components.TrackedAppCard
import com.ranzan.zbuilds.ui.components.UpdateBannerCard
import com.ranzan.zbuilds.ui.theme.DarkBackground
import com.ranzan.zbuilds.ui.theme.DarkSurface
import com.ranzan.zbuilds.ui.theme.GreenPrimary
import com.ranzan.zbuilds.ui.theme.TextMuted
import com.ranzan.zbuilds.ui.theme.TextPrimary
import com.ranzan.zbuilds.ui.theme.TextSecondary
import com.ranzan.zbuilds.ui.theme.ZBuildsTheme
import com.ranzan.zbuilds.ui.util.FileIntentHelper
import org.koin.androidx.compose.koinViewModel

/**
 * Host composable for HomeScreen responsible for ViewModel injection,
 * collecting state, handling one-shot effects (navigation, intents, snackbars),
 * and managing Android lifecycle events.
 */
@Composable
fun HomeScreenHost(
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeUiAction.OnResume)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeUiEffect.NavigateToSettings -> onSettingsClick()
                is HomeUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is HomeUiEffect.InstallApk -> FileIntentHelper.installApk(context, effect.filePath)
                is HomeUiEffect.OpenApp -> FileIntentHelper.openApp(context, effect.packageName)
                is HomeUiEffect.UninstallApp -> FileIntentHelper.uninstallApp(context, effect.packageName)
                is HomeUiEffect.OpenOrShareZip -> FileIntentHelper.openOrShareZip(context, effect.filePath)
            }
        }
    }

    // Kept here so the home list keeps its scroll position while Add App Source is open
    val listState = rememberLazyListState()

    if (state.isAddSheetOpen) {
        AddAppSourceScreen(
            sourceUrl = state.addSourceUrl,
            isFetching = state.isFetchingSource,
            fetchedRelease = state.fetchedRelease,
            errorMessage = state.addSourceError,
            olderReleases = state.olderReleases.releases,
            isLoadingOlder = state.olderReleases.isLoading,
            hasMoreOlder = state.olderReleases.hasMore,
            olderMessage = state.olderReleases.message,
            suggestions = state.suggestions,
            showApks = state.showApks,
            showModules = state.showModules,
            onUrlChange = { viewModel.onAction(HomeUiAction.OnSourceUrlChange(it)) },
            onFetchClick = { viewModel.onAction(HomeUiAction.OnFetchSourceReleases) },
            onLoadOlderClick = { viewModel.onAction(HomeUiAction.OnLoadOlderReleases) },
            onSelectAsset = { asset, release ->
                viewModel.onAction(HomeUiAction.OnSelectAssetToTrack(asset, release))
            },
            onNavigateBack = { viewModel.onAction(HomeUiAction.OnCloseAddSheet) }
        )
    } else {
        HomeScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            listState = listState,
            onAction = viewModel::onAction
        )
    }
}

/**
 * Pure UI Composable for Home Screen.
 * Contains zero ViewModel or Android intent dependencies to guarantee seamless Compose Previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    onAction: (HomeUiAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Any interaction other than typing in the search field hides the keyboard
    val handleAction: (HomeUiAction) -> Unit = { action ->
        if (action !is HomeUiAction.OnSearchQueryChange) focusManager.clearFocus()
        onAction(action)
    }

    val isAnyUpdateDownloading = remember(state.downloadProgressMap, state.trackedApps) {
        state.trackedApps.any { app ->
            app.hasUpdate && state.downloadProgressMap[app.id] is DownloadProgress.Downloading
        }
    }

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { handleAction(HomeUiAction.OnOpenAddSheet) },
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_action_add_desc)) },
                text = { Text(stringResource(R.string.home_btn_add), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = GreenPrimary,
                contentColor = DarkBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { handleAction(HomeUiAction.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with App Name, Status Indicator, and Settings Button
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .background(GreenPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { handleAction(HomeUiAction.OnSettingsClick) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.home_settings_desc),
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Top Search Bar
                item(key = "search_bar") {
                    TopSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { handleAction(HomeUiAction.OnSearchQueryChange(it)) }
                    )
                }

                // Full Width Install/Update Apps Action Card (shown only when apps are tracked)
                if (state.trackedApps.isNotEmpty()) {
                    item(key = "update_banner") {
                        UpdateBannerCard(
                            updateCount = state.updateCount,
                            totalAppsCount = state.trackedApps.size,
                            isRefreshing = state.isRefreshing,
                            isDownloading = isAnyUpdateDownloading,
                            onUpdateClick = { handleAction(HomeUiAction.OnUpdateAll) }
                        )
                    }
                }

                // Section Title
                item(key = "section_title") {
                    Text(
                        text = if (state.searchQuery.isNotBlank()) {
                            stringResource(R.string.home_search_results_title, state.filteredApps.size)
                        } else {
                            stringResource(R.string.home_added_apps_title, state.trackedApps.size)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Added Apps List
                if (state.filteredApps.isEmpty()) {
                    item(key = "empty_state") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) {
                                        stringResource(R.string.home_empty_search_title, state.searchQuery)
                                    } else {
                                        stringResource(R.string.home_empty_apps_title)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) {
                                        stringResource(R.string.home_empty_search_subtitle)
                                    } else {
                                        stringResource(R.string.home_empty_apps_subtitle)
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(state.filteredApps, key = { it.id }) { app ->
                        TrackedAppCard(
                            app = app,
                            downloadProgress = state.downloadProgressMap[app.id],
                            onDownloadClick = { handleAction(HomeUiAction.OnDownloadApp(app)) },
                            onCheckUpdateClick = { handleAction(HomeUiAction.OnCheckAppUpdate(app)) },
                            onCancelDownloadClick = { handleAction(HomeUiAction.OnCancelDownload(app.id)) },
                            onInstallClick = {
                                app.localFilePath?.let { path ->
                                    handleAction(HomeUiAction.OnInstallApp(path))
                                }
                            },
                            onOpenClick = {
                                app.packageName?.let { pkg ->
                                    handleAction(HomeUiAction.OnOpenApp(pkg))
                                }
                            },
                            onUninstallClick = { pkg ->
                                handleAction(HomeUiAction.OnUninstallApp(pkg))
                            },
                            onShareZipClick = {
                                app.localFilePath?.let { path ->
                                    handleAction(HomeUiAction.OnShareZip(path))
                                }
                            },
                            onDeleteClick = { handleAction(HomeUiAction.OnDeleteTrackedApp(app.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ZBuildsTheme {
        HomeScreen(
            state = HomeUiState(
                trackedApps = listOf(
                    TrackedApp(
                        id = "1",
                        name = "ViMusic",
                        sourceUrl = "https://github.com/vfsfitvnm/ViMusic",
                        packageName = "it.vfsfitvnm.vimusic",
                        fileType = AppFileType.APK,
                        arch = "arm64-v8a",
                        assetName = "app-release.apk",
                        latestVersion = "v0.5.4",
                        latestDownloadUrl = "https://example.com/app.apk",
                        latestSizeBytes = 15_000_000L,
                        downloadedVersion = "v0.5.4",
                        installedVersion = "v0.5.4",
                        isInstalled = true,
                        localFilePath = "/data/data/com.ranzan.zbuilds/files/app.apk",
                        hasUpdate = false,
                        lastCheckedTimestamp = System.currentTimeMillis()
                    )
                ),
                filteredApps = listOf(
                    TrackedApp(
                        id = "1",
                        name = "ViMusic",
                        sourceUrl = "https://github.com/vfsfitvnm/ViMusic",
                        packageName = "it.vfsfitvnm.vimusic",
                        fileType = AppFileType.APK,
                        arch = "arm64-v8a",
                        assetName = "app-release.apk",
                        latestVersion = "v0.5.4",
                        latestDownloadUrl = "https://example.com/app.apk",
                        latestSizeBytes = 15_000_000L,
                        downloadedVersion = "v0.5.4",
                        installedVersion = "v0.5.4",
                        isInstalled = true,
                        localFilePath = "/data/data/com.ranzan.zbuilds/files/app.apk",
                        hasUpdate = false,
                        lastCheckedTimestamp = System.currentTimeMillis()
                    )
                )
            )
        )
    }
}
