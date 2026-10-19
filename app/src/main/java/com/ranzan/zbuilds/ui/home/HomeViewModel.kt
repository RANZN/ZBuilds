package com.ranzan.zbuilds.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val getTrackedAppsUseCase: GetTrackedAppsUseCase,
    private val addTrackedAppUseCase: AddTrackedAppUseCase,
    private val deleteTrackedAppUseCase: DeleteTrackedAppUseCase,
    private val fetchSourceReleasesUseCase: FetchSourceReleasesUseCase,
    private val checkAppUpdatesUseCase: CheckAppUpdatesUseCase,
    private val downloadAppUseCase: DownloadAppUseCase,
    private val getSourceSuggestionsUseCase: GetSourceSuggestionsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val trackEventUseCase: TrackEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        trackEventUseCase("home_screen_opened")

        // Collect tracked apps from DataStore
        viewModelScope.launch {
            getTrackedAppsUseCase().collect { apps ->
                _uiState.update { state ->
                    state.copy(
                        trackedApps = apps,
                        filteredApps = filterApps(apps, state.searchQuery, state.showApks, state.showModules)
                    )
                }
            }
        }

        // Collect user settings to apply build format filtering dynamically
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        showApks = settings.showApks,
                        showModules = settings.showModules,
                        filteredApps = filterApps(state.trackedApps, state.searchQuery, settings.showApks, settings.showModules)
                    )
                }
            }
        }

        // Collect source suggestions from repository (configured for Remote Config)
        viewModelScope.launch {
            getSourceSuggestionsUseCase().collect { suggestions ->
                _uiState.update { it.copy(suggestions = suggestions) }
            }
        }

        // Automatic update check on app launch
        checkForUpdates(silent = true)
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnSearchQueryChange -> {
                _uiState.update { state ->
                    state.copy(
                        searchQuery = event.query,
                        filteredApps = filterApps(state.trackedApps, event.query, state.showApks, state.showModules)
                    )
                }
            }
            is HomeUiEvent.OnRefresh -> {
                trackEventUseCase("manual_refresh_triggered")
                checkForUpdates(silent = false)
            }
            is HomeUiEvent.OnCheckAllUpdates -> {
                trackEventUseCase("check_all_updates_clicked")
                checkForUpdates(silent = false)
            }
            is HomeUiEvent.OnOpenAddSheet -> {
                trackEventUseCase("add_app_sheet_opened")
                _uiState.update {
                    it.copy(
                        isAddSheetOpen = true,
                        addSourceError = null,
                        fetchedRelease = null
                    )
                }
            }
            is HomeUiEvent.OnCloseAddSheet -> {
                _uiState.update {
                    it.copy(
                        isAddSheetOpen = false,
                        addSourceUrl = "",
                        isFetchingSource = false,
                        fetchedRelease = null,
                        addSourceError = null
                    )
                }
            }
            is HomeUiEvent.OnSourceUrlChange -> {
                _uiState.update {
                    it.copy(
                        addSourceUrl = event.url,
                        addSourceError = null
                    )
                }
            }
            is HomeUiEvent.OnFetchSourceReleases -> {
                fetchSourceReleases()
            }
            is HomeUiEvent.OnSelectAssetToTrack -> {
                trackAndDownloadAsset(event.asset, event.release, event.customName)
            }
            is HomeUiEvent.OnDownloadApp -> {
                startDownload(event.app)
            }
            is HomeUiEvent.OnCancelDownload -> {
                viewModelScope.launch {
                    downloadAppUseCase.cancelDownload(event.appId)
                    trackEventUseCase("download_cancelled", mapOf("app_id" to event.appId))
                }
            }
            is HomeUiEvent.OnDeleteTrackedApp -> {
                viewModelScope.launch {
                    deleteTrackedAppUseCase(event.appId)
                    trackEventUseCase("app_deleted", mapOf("app_id" to event.appId))
                    _uiState.update { it.copy(statusMessage = "App removed from tracked list") }
                }
            }
            is HomeUiEvent.OnClearStatusMessage -> {
                _uiState.update { it.copy(statusMessage = null) }
            }
            is HomeUiEvent.OnSettingsClick -> {
                // Handled in UI navigation
            }
        }
    }

    private fun checkForUpdates(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isRefreshing = true, statusMessage = "Checking for updates...") }
            }

            val result = checkAppUpdatesUseCase()
            if (result.isSuccess) {
                val updatedApps = result.getOrNull().orEmpty()
                val newUpdatesCount = updatedApps.count { it.hasUpdate }
                val message = if (newUpdatesCount > 0) {
                    "$newUpdatesCount update(s) available!"
                } else if (!silent) {
                    "All tracked apps are up to date"
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        statusMessage = message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        statusMessage = if (!silent) "Failed to check updates: ${result.exceptionOrNull()?.message}" else null
                    )
                }
            }
        }
    }

    private fun fetchSourceReleases() {
        val url = _uiState.value.addSourceUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(addSourceError = "Please enter a GitHub repository URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingSource = true, addSourceError = null) }
            trackEventUseCase("fetch_source_releases", mapOf("url" to url))

            val result = fetchSourceReleasesUseCase(url)
            if (result.isSuccess) {
                val release = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isFetchingSource = false,
                        fetchedRelease = release,
                        addSourceError = if (release?.assets.isNullOrEmpty()) "No APK or ZIP assets found in this release" else null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingSource = false,
                        addSourceError = result.exceptionOrNull()?.message ?: "Failed to fetch repository release"
                    )
                }
            }
        }
    }

    private fun trackAndDownloadAsset(
        asset: ReleaseAsset,
        release: SourceRelease,
        customName: String?
    ) {
        val appName = customName?.ifBlank { null }
            ?: FormatHelper.deriveAppNameFromAsset(asset.name)

        val newApp = TrackedApp(
            id = UUID.randomUUID().toString(),
            name = appName,
            sourceUrl = _uiState.value.addSourceUrl.trim(),
            packageName = null,
            fileType = asset.fileType,
            arch = asset.arch,
            assetName = asset.name,
            latestVersion = release.tag,
            latestDownloadUrl = asset.downloadUrl,
            latestSizeBytes = asset.sizeBytes,
            downloadedVersion = "",
            installedVersion = null,
            isInstalled = false,
            localFilePath = null,
            hasUpdate = false,
            lastCheckedTimestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            addTrackedAppUseCase(newApp)
            trackEventUseCase(
                "app_tracked",
                mapOf(
                    "name" to appName,
                    "file_type" to asset.fileType.name,
                    "version" to release.tag
                )
            )

            // Close sheet and give feedback
            _uiState.update {
                it.copy(
                    isAddSheetOpen = false,
                    addSourceUrl = "",
                    fetchedRelease = null,
                    statusMessage = "Added $appName to tracked apps"
                )
            }

            // Immediately start downloading the selected asset
            startDownload(newApp)
        }
    }

    private fun startDownload(app: TrackedApp) {
        viewModelScope.launch {
            trackEventUseCase("download_started", mapOf("app_id" to app.id, "app_name" to app.name))

            // Observe progress flow
            val progressJob = launch {
                downloadAppUseCase.observeProgress(app.id).collect { progress ->
                    _uiState.update { state ->
                        state.copy(
                            downloadProgressMap = state.downloadProgressMap + (app.id to progress)
                        )
                    }
                }
            }

            // Trigger download execution
            val result = downloadAppUseCase(app)

            progressJob.cancel()

            _uiState.update { state ->
                state.copy(
                    downloadProgressMap = state.downloadProgressMap - app.id,
                    statusMessage = if (result.isSuccess) {
                        trackEventUseCase("download_completed", mapOf("app_id" to app.id, "app_name" to app.name))
                        "Downloaded ${app.name} (${app.latestVersion})"
                    } else {
                        trackEventUseCase("download_failed", mapOf("app_id" to app.id, "error" to (result.exceptionOrNull()?.message ?: "unknown")))
                        "Download failed: ${result.exceptionOrNull()?.message}"
                    }
                )
            }
        }
    }

    private fun filterApps(
        apps: List<TrackedApp>,
        query: String,
        showApks: Boolean,
        showModules: Boolean
    ): List<TrackedApp> {
        return apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.name.contains(query, ignoreCase = true) ||
                    app.assetName.contains(query, ignoreCase = true) ||
                    app.latestVersion.contains(query, ignoreCase = true)

            val matchesFormat = when (app.fileType) {
                AppFileType.APK -> showApks
                AppFileType.ZIP -> showModules
            }

            matchesQuery && matchesFormat
        }
    }
}
