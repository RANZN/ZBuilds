package com.ranzan.zbuilds.ui.home

import com.ranzan.zbuilds.core.base.UiAction
import com.ranzan.zbuilds.core.base.UiEffect
import com.ranzan.zbuilds.core.base.UiState
import com.ranzan.zbuilds.core.base.UiText
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.SourceSuggestion
import com.ranzan.zbuilds.domain.model.TrackedApp

data class HomeUiState(
    val searchQuery: String = "",
    val trackedApps: List<TrackedApp> = emptyList(),
    val filteredApps: List<TrackedApp> = emptyList(),
    val showApks: Boolean = true,
    val showModules: Boolean = true,
    val isRefreshing: Boolean = false,
    val downloadProgressMap: Map<String, DownloadProgress> = emptyMap(),
    // Suggestions from repository / Remote Config
    val suggestions: List<SourceSuggestion> = emptyList(),
    // Add App Source screen state
    val isAddSourceOpen: Boolean = false,
    val addSourceUrl: String = "",
    val isFetchingSource: Boolean = false,
    val fetchedRelease: SourceRelease? = null,
    val fetchedSourceUrl: String = "",
    val olderReleases: OlderReleasesState = OlderReleasesState(),
    val addSourceError: UiText? = null,
    val statusMessage: UiText? = null
) : UiState {
    val updateCount: Int
        get() = trackedApps.count { it.hasUpdate }
}

/**
 * Older releases loaded on demand on the Add App Source screen.
 * [releases] only hold files whose name pattern is not already listed.
 */
data class OlderReleasesState(
    val releases: List<SourceRelease> = emptyList(),
    val lastLoadedPage: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val message: UiText? = null
)

sealed interface HomeUiAction : UiAction {
    data class OnSearchQueryChange(val query: String) : HomeUiAction
    data object OnRefresh : HomeUiAction
    data object OnOpenAddSource : HomeUiAction
    data object OnCloseAddSource : HomeUiAction
    data class OnSourceUrlChange(val url: String) : HomeUiAction
    data object OnFetchSourceReleases : HomeUiAction
    data object OnLoadOlderReleases : HomeUiAction
    data class OnSelectAssetToTrack(val asset: ReleaseAsset, val release: SourceRelease, val customName: String? = null) : HomeUiAction
    data class OnDownloadApp(val app: TrackedApp) : HomeUiAction
    data class OnCancelDownload(val appId: String) : HomeUiAction
    data class OnDeleteTrackedApp(val appId: String) : HomeUiAction
    data object OnCheckAllUpdates : HomeUiAction
    data class OnCheckAppUpdate(val app: TrackedApp) : HomeUiAction
    data object OnUpdateAll : HomeUiAction
    data object OnResume : HomeUiAction
    data object OnClearStatusMessage : HomeUiAction
    data object OnSettingsClick : HomeUiAction

    // App Actions (delegated to effect handler in host)
    data class OnInstallApp(val localFilePath: String) : HomeUiAction
    data class OnOpenApp(val packageName: String) : HomeUiAction
    data class OnUninstallApp(val packageName: String) : HomeUiAction
    data class OnShareZip(val localFilePath: String) : HomeUiAction
}

typealias HomeUiEvent = HomeUiAction

sealed interface HomeUiEffect : UiEffect {
    data object NavigateToSettings : HomeUiEffect
    data class ShowSnackbar(val message: UiText) : HomeUiEffect
    data class InstallApk(val filePath: String) : HomeUiEffect
    data class OpenApp(val packageName: String) : HomeUiEffect
    data class UninstallApp(val packageName: String) : HomeUiEffect
    data class OpenOrShareZip(val filePath: String) : HomeUiEffect
}
