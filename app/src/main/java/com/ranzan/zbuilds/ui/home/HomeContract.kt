package com.ranzan.zbuilds.ui.home

import android.content.Context
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.SourceSuggestion
import com.ranzan.zbuilds.domain.model.TrackedApp

data class HomeUiState(
    val searchQuery: String = "",
    val trackedApps: List<TrackedApp> = emptyList(),
    val filteredApps: List<TrackedApp> = emptyList(),
    val isRefreshing: Boolean = false,
    val downloadProgressMap: Map<String, DownloadProgress> = emptyMap(),
    // Suggestions from repository / Remote Config
    val suggestions: List<SourceSuggestion> = emptyList(),
    // Add App Sheet State
    val isAddSheetOpen: Boolean = false,
    val addSourceUrl: String = "",
    val isFetchingSource: Boolean = false,
    val fetchedRelease: SourceRelease? = null,
    val addSourceError: String? = null,
    val statusMessage: String? = null
) {
    val updateCount: Int
        get() = trackedApps.count { it.hasUpdate }
}

sealed interface HomeUiEvent {
    data class OnSearchQueryChange(val query: String) : HomeUiEvent
    data object OnRefresh : HomeUiEvent
    data object OnOpenAddSheet : HomeUiEvent
    data object OnCloseAddSheet : HomeUiEvent
    data class OnSourceUrlChange(val url: String) : HomeUiEvent
    data object OnFetchSourceReleases : HomeUiEvent
    data class OnSelectAssetToTrack(val asset: ReleaseAsset, val release: SourceRelease, val customName: String? = null) : HomeUiEvent
    data class OnDownloadApp(val app: TrackedApp) : HomeUiEvent
    data class OnCancelDownload(val appId: String) : HomeUiEvent
    data class OnInstallApk(val context: Context, val app: TrackedApp) : HomeUiEvent
    data class OnOpenApp(val context: Context, val app: TrackedApp) : HomeUiEvent
    data class OnShareZip(val context: Context, val app: TrackedApp) : HomeUiEvent
    data class OnDeleteTrackedApp(val appId: String) : HomeUiEvent
    data object OnCheckAllUpdates : HomeUiEvent
    data object OnClearStatusMessage : HomeUiEvent
    data object OnSettingsClick : HomeUiEvent
}
