package com.ranzan.zbuilds.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranzan.zbuilds.ui.components.AddAppSourceSheet
import com.ranzan.zbuilds.ui.components.TopSearchBar
import com.ranzan.zbuilds.ui.components.TrackedAppCard
import com.ranzan.zbuilds.ui.components.UpdateBannerCard
import com.ranzan.zbuilds.ui.theme.DarkBackground
import com.ranzan.zbuilds.ui.theme.DarkSurface
import com.ranzan.zbuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.zbuilds.ui.theme.GreenPrimary
import com.ranzan.zbuilds.ui.theme.TextMuted
import com.ranzan.zbuilds.ui.theme.TextPrimary
import com.ranzan.zbuilds.ui.theme.TextSecondary
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Notify user of status messages
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(HomeUiEvent.OnClearStatusMessage)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onEvent(HomeUiEvent.OnOpenAddSheet) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Source") },
                text = { Text("Add", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = GreenPrimary,
                contentColor = DarkBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onEvent(HomeUiEvent.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
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
                            text = "ZBuilds",
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
                            onClick = {
                                viewModel.onEvent(HomeUiEvent.OnSettingsClick)
                                onSettingsClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Top Search Bar
                item(key = "search_bar") {
                    TopSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onEvent(HomeUiEvent.OnSearchQueryChange(it)) }
                    )
                }

                // Full Width Install/Update Apps Action Card (shown only when apps are tracked)
                if (state.trackedApps.isNotEmpty()) {
                    item(key = "update_banner") {
                        UpdateBannerCard(
                            updateCount = state.updateCount,
                            totalAppsCount = state.trackedApps.size,
                            isRefreshing = state.isRefreshing,
                            onUpdateClick = { viewModel.onEvent(HomeUiEvent.OnCheckAllUpdates) }
                        )
                    }
                }

                // Section Title
                item(key = "section_title") {
                    Text(
                        text = if (state.searchQuery.isNotBlank()) "SEARCH RESULTS (${state.filteredApps.size})" else "ADDED APPS (${state.trackedApps.size})",
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
                                    text = if (state.searchQuery.isNotBlank()) "No apps matching \"${state.searchQuery}\"" else "No Apps Added Yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) "Try searching for a different keyword or clear the search filter." else "Tap the (+ Add) button below to paste any GitHub repository link and choose the APK or ZIP build to track.",
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
                            onDownloadClick = { viewModel.onEvent(HomeUiEvent.OnDownloadApp(app)) },
                            onCancelDownloadClick = { viewModel.onEvent(HomeUiEvent.OnCancelDownload(app.id)) },
                            onInstallClick = {
                                app.localFilePath?.let { path ->
                                    com.ranzan.zbuilds.ui.util.FileIntentHelper.installApk(context, path)
                                }
                            },
                            onOpenClick = {
                                app.packageName?.let { pkg ->
                                    com.ranzan.zbuilds.ui.util.FileIntentHelper.openApp(context, pkg)
                                }
                            },
                            onShareZipClick = {
                                app.localFilePath?.let { path ->
                                    com.ranzan.zbuilds.ui.util.FileIntentHelper.openOrShareZip(context, path)
                                }
                            },
                            onDeleteClick = { viewModel.onEvent(HomeUiEvent.OnDeleteTrackedApp(app.id)) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for adding any URL & picking APK/ZIP downloads
    AddAppSourceSheet(
        isOpen = state.isAddSheetOpen,
        sourceUrl = state.addSourceUrl,
        isFetching = state.isFetchingSource,
        fetchedRelease = state.fetchedRelease,
        errorMessage = state.addSourceError,
        sheetState = sheetState,
        suggestions = state.suggestions,
        showApks = state.showApks,
        showModules = state.showModules,
        onUrlChange = { viewModel.onEvent(HomeUiEvent.OnSourceUrlChange(it)) },
        onFetchClick = { viewModel.onEvent(HomeUiEvent.OnFetchSourceReleases) },
        onSelectAsset = { asset, release ->
            viewModel.onEvent(HomeUiEvent.OnSelectAssetToTrack(asset, release))
        },
        onDismiss = { viewModel.onEvent(HomeUiEvent.OnCloseAddSheet) }
    )
}
