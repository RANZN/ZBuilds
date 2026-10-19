package com.ranzan.zbuilds.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.ui.util.FileIntentHelper
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
    private val packageInspector: PackageInspectorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Collect tracked apps from DataStore
        viewModelScope.launch {
            getTrackedAppsUseCase().collect { apps ->
                _uiState.update { state ->
                    state.copy(
                        trackedApps = apps,
                        filteredApps = filterApps(apps, state.searchQuery)
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
                        filteredApps = filterApps(state.trackedApps, event.query)
                    )
                }
            }
            is HomeUiEvent.OnRefresh -> {
                checkForUpdates(silent = false)
            }
            is HomeUiEvent.OnCheckAllUpdates -> {
                checkForUpdates(silent = false)
            }
            is HomeUiEvent.OnOpenAddSheet -> {
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
                }
            }
            is HomeUiEvent.OnInstallApk -> {
                event.app.localFilePath?.let { path ->
                    FileIntentHelper.installApk(event.context, path)
                }
            }
            is HomeUiEvent.OnOpenApp -> {
                event.app.packageName?.let { pkg ->
                    packageInspector.openApp(pkg)
                }
            }
            is HomeUiEvent.OnShareZip -> {
                event.app.localFilePath?.let { path ->
                    FileIntentHelper.openOrShareZip(event.context, path)
                }
            }
            is HomeUiEvent.OnDeleteTrackedApp -> {
                viewModelScope.launch {
                    deleteTrackedAppUseCase(event.appId)
                    _uiState.update { it.copy(statusMessage = "App removed from tracked list") }
                }
            }
            is HomeUiEvent.OnClearStatusMessage -> {
                _uiState.update { it.copy(statusMessage = null) }
            }
            is HomeUiEvent.OnSettingsClick -> {
                _uiState.update { it.copy(statusMessage = "Settings screen coming soon") }
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
                val updateCount = updatedApps.count { it.hasUpdate }
                val msg = if (updateCount > 0) {
                    "$updateCount update(s) available!"
                } else if (!silent) {
                    "All apps are up to date"
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        statusMessage = msg
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
            _uiState.update { it.copy(addSourceError = "Please enter a valid GitHub repository URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingSource = true,
                    addSourceError = null,
                    fetchedRelease = null
                )
            }

            val result = fetchSourceReleasesUseCase(url)
            if (result.isSuccess) {
                val release = result.getOrThrow()
                if (release.assets.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isFetchingSource = false,
                            addSourceError = "No APK or ZIP assets found in release ${release.tag}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isFetchingSource = false,
                            fetchedRelease = release,
                            addSourceError = null
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingSource = false,
                        addSourceError = result.exceptionOrNull()?.message ?: "Failed to fetch releases"
                    )
                }
            }
        }
    }

    private fun trackAndDownloadAsset(asset: ReleaseAsset, release: SourceRelease, customName: String?) {
        val appName = customName?.takeIf { it.isNotBlank() }
            ?: FormatHelper.deriveAppNameFromAsset(asset.name)

        val newApp = TrackedApp(
            id = UUID.randomUUID().toString(),
            name = appName,
            sourceUrl = _uiState.value.addSourceUrl.trim(),
            packageName = null,
            fileType = asset.fileType,
            arch = asset.arch,
            assetName = asset.name,
            assetPattern = asset.name,
            downloadedVersion = release.tag,
            latestVersion = release.tag,
            latestDownloadUrl = asset.downloadUrl,
            latestSizeBytes = asset.sizeBytes,
            localFilePath = null,
            hasUpdate = false,
            lastCheckedTimestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            addTrackedAppUseCase(newApp)

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

            val result = downloadAppUseCase.startDownload(app)
            progressJob.cancel()

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(statusMessage = "Downloaded ${app.name} (${app.latestVersion})")
                }
            } else {
                _uiState.update {
                    it.copy(statusMessage = "Download failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun filterApps(apps: List<TrackedApp>, query: String): List<TrackedApp> {
        if (query.isBlank()) return apps
        val cleanQuery = query.trim().lowercase()
        return apps.filter { app ->
            app.name.lowercase().contains(cleanQuery) ||
                    app.latestVersion.lowercase().contains(cleanQuery) ||
                    app.downloadedVersion.lowercase().contains(cleanQuery) ||
                    app.arch.lowercase().contains(cleanQuery) ||
                    app.fileType.name.lowercase().contains(cleanQuery) ||
                    app.assetName.lowercase().contains(cleanQuery) ||
                    app.sourceUrl.lowercase().contains(cleanQuery)
        }
    }
}
