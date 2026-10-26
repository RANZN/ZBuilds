package com.ranzan.zbuilds.ui.home

import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.core.base.BaseViewModel
import com.ranzan.zbuilds.core.utils.AssetMatcher
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
    private val getSettingsUseCase: GetSettingsUseCase
) : BaseViewModel<HomeUiState, HomeUiAction, HomeUiEffect>(HomeUiState()) {

    init {
        // Collect tracked apps from DataStore
        viewModelScope.launch {
            getTrackedAppsUseCase().collect { apps ->
                setState {
                    copy(
                        trackedApps = apps,
                        filteredApps = filterApps(apps, searchQuery, showApks, showModules)
                    )
                }
            }
        }

        // Collect user settings to apply build format filtering dynamically
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                setState {
                    copy(
                        showApks = settings.showApks,
                        showModules = settings.showModules,
                        filteredApps = filterApps(trackedApps, searchQuery, settings.showApks, settings.showModules)
                    )
                }
            }
        }

        // Collect source suggestions from repository
        viewModelScope.launch {
            getSourceSuggestionsUseCase().collect { suggestions ->
                setState { copy(suggestions = suggestions) }
            }
        }

        // Automatic update check on app launch
        checkForUpdates(silent = true)
    }

    override fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.OnSearchQueryChange -> {
                setState {
                    copy(
                        searchQuery = action.query,
                        filteredApps = filterApps(trackedApps, action.query, showApks, showModules)
                    )
                }
            }
            is HomeUiAction.OnRefresh -> {
                checkForUpdates(silent = false)
            }
            is HomeUiAction.OnCheckAllUpdates, is HomeUiAction.OnUpdateAll -> {
                downloadAllAvailableUpdates()
            }
            is HomeUiAction.OnResume -> {
                checkForUpdates(silent = true)
            }
            is HomeUiAction.OnOpenAddSheet -> {
                setState {
                    copy(
                        isAddSheetOpen = true,
                        addSourceError = null,
                        fetchedRelease = null
                    )
                }
            }
            is HomeUiAction.OnCloseAddSheet -> {
                setState {
                    copy(
                        isAddSheetOpen = false,
                        addSourceUrl = "",
                        isFetchingSource = false,
                        fetchedRelease = null,
                        addSourceError = null
                    )
                }
            }
            is HomeUiAction.OnSourceUrlChange -> {
                setState {
                    copy(
                        addSourceUrl = action.url,
                        addSourceError = null
                    )
                }
            }
            is HomeUiAction.OnFetchSourceReleases -> {
                fetchSourceReleases()
            }
            is HomeUiAction.OnSelectAssetToTrack -> {
                trackAndDownloadAsset(action.asset, action.release, action.customName)
            }
            is HomeUiAction.OnDownloadApp -> {
                startDownload(action.app)
            }
            is HomeUiAction.OnCancelDownload -> {
                viewModelScope.launch {
                    downloadAppUseCase.cancelDownload(action.appId)
                }
            }
            is HomeUiAction.OnDeleteTrackedApp -> {
                viewModelScope.launch {
                    deleteTrackedAppUseCase(action.appId)
                    val msg = "App removed from tracked list"
                    setState { copy(statusMessage = msg) }
                    sendEffect(HomeUiEffect.ShowSnackbar(msg))
                }
            }
            is HomeUiAction.OnClearStatusMessage -> {
                setState { copy(statusMessage = null) }
            }
            is HomeUiAction.OnSettingsClick -> {
                sendEffect(HomeUiEffect.NavigateToSettings)
            }
            is HomeUiAction.OnInstallApp -> {
                sendEffect(HomeUiEffect.InstallApk(action.localFilePath))
            }
            is HomeUiAction.OnOpenApp -> {
                sendEffect(HomeUiEffect.OpenApp(action.packageName))
            }
            is HomeUiAction.OnUninstallApp -> {
                sendEffect(HomeUiEffect.UninstallApp(action.packageName))
            }
            is HomeUiAction.OnShareZip -> {
                sendEffect(HomeUiEffect.OpenOrShareZip(action.localFilePath))
            }
        }
    }

    fun onEvent(event: HomeUiEvent) = onAction(event)

    private fun checkForUpdates(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) {
                setState { copy(isRefreshing = true, statusMessage = "Checking for updates...") }
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

                setState {
                    copy(
                        isRefreshing = false,
                        statusMessage = message
                    )
                }
                if (message != null && !silent) {
                    sendEffect(HomeUiEffect.ShowSnackbar(message))
                }
            } else {
                val errorMsg = if (!silent) "Failed to check updates: ${result.exceptionOrNull()?.message}" else null
                setState {
                    copy(
                        isRefreshing = false,
                        statusMessage = errorMsg
                    )
                }
                if (errorMsg != null) {
                    sendEffect(HomeUiEffect.ShowSnackbar(errorMsg))
                }
            }
        }
    }

    private fun downloadAllAvailableUpdates() {
        val appsToUpdate = currentState.trackedApps.filter { it.hasUpdate }
        if (appsToUpdate.isNotEmpty()) {
            val msg = "Starting download for ${appsToUpdate.size} update(s)..."
            setState { copy(statusMessage = msg) }
            sendEffect(HomeUiEffect.ShowSnackbar(msg))
            appsToUpdate.forEach { app ->
                if (currentState.downloadProgressMap[app.id] !is DownloadProgress.Downloading) {
                    startDownload(app)
                }
            }
        } else {
            viewModelScope.launch {
                setState { copy(isRefreshing = true, statusMessage = "Checking for available updates...") }
                val result = checkAppUpdatesUseCase()
                if (result.isSuccess) {
                    val updatedApps = result.getOrNull().orEmpty()
                    val newUpdates = updatedApps.filter { it.hasUpdate }
                    if (newUpdates.isNotEmpty()) {
                        val msg = "Found ${newUpdates.size} update(s). Starting downloads..."
                        setState {
                            copy(
                                isRefreshing = false,
                                statusMessage = msg
                            )
                        }
                        sendEffect(HomeUiEffect.ShowSnackbar(msg))
                        newUpdates.forEach { app ->
                            if (currentState.downloadProgressMap[app.id] !is DownloadProgress.Downloading) {
                                startDownload(app)
                            }
                        }
                    } else {
                        val msg = "All tracked apps are up to date"
                        setState {
                            copy(
                                isRefreshing = false,
                                statusMessage = msg
                            )
                        }
                        sendEffect(HomeUiEffect.ShowSnackbar(msg))
                    }
                } else {
                    val errorMsg = "Failed to check updates: ${result.exceptionOrNull()?.message}"
                    setState {
                        copy(
                            isRefreshing = false,
                            statusMessage = errorMsg
                        )
                    }
                    sendEffect(HomeUiEffect.ShowSnackbar(errorMsg))
                }
            }
        }
    }

    private fun fetchSourceReleases() {
        val url = currentState.addSourceUrl.trim()
        if (url.isBlank()) {
            setState { copy(addSourceError = "Please enter a GitHub repository URL") }
            return
        }

        viewModelScope.launch {
            setState { copy(isFetchingSource = true, addSourceError = null) }

            val result = fetchSourceReleasesUseCase(url)
            if (result.isSuccess) {
                val release = result.getOrNull()
                setState {
                    copy(
                        isFetchingSource = false,
                        fetchedRelease = release,
                        addSourceError = if (release?.assets.isNullOrEmpty()) "No APK or ZIP assets found in this release" else null
                    )
                }
            } else {
                setState {
                    copy(
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
            sourceUrl = currentState.addSourceUrl.trim(),
            packageName = null,
            fileType = asset.fileType,
            arch = asset.arch,
            assetName = asset.name,
            assetPattern = AssetMatcher.toPattern(asset.name, release.tag),
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

            val msg = "Added $appName to tracked apps"
            // Close sheet and give feedback
            setState {
                copy(
                    isAddSheetOpen = false,
                    addSourceUrl = "",
                    fetchedRelease = null,
                    statusMessage = msg
                )
            }
            sendEffect(HomeUiEffect.ShowSnackbar(msg))

            // Immediately start downloading the selected asset
            startDownload(newApp)
        }
    }

    private fun startDownload(app: TrackedApp) {
        viewModelScope.launch {
            // Observe progress flow
            val progressJob = launch {
                downloadAppUseCase.observeProgress(app.id).collect { progress ->
                    setState {
                        copy(
                            downloadProgressMap = downloadProgressMap + (app.id to progress)
                        )
                    }
                }
            }

            // Trigger download execution
            val result = downloadAppUseCase(app)

            progressJob.cancel()

            val msg = if (result.isSuccess) {
                "Downloaded ${app.name} (${app.latestVersion})"
            } else {
                "Download failed: ${result.exceptionOrNull()?.message}"
            }

            setState {
                copy(
                    downloadProgressMap = downloadProgressMap - app.id,
                    statusMessage = msg
                )
            }
            sendEffect(HomeUiEffect.ShowSnackbar(msg))
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
