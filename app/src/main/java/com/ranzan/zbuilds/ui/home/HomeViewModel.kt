package com.ranzan.zbuilds.ui.home

import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.base.BaseViewModel
import com.ranzan.zbuilds.core.base.UiText
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.rules.latestBuildVersion
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.ui.util.toUiText
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    private var olderReleasesJob: Job? = null

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
            is HomeUiAction.OnUpdateAll -> {
                downloadAllAvailableUpdates()
            }
            is HomeUiAction.OnResume -> {
                checkForUpdates(silent = true)
            }
            is HomeUiAction.OnOpenAddSource -> {
                setState {
                    copy(
                        isAddSourceOpen = true,
                        addSourceError = null,
                        fetchedRelease = null,
                        olderReleases = OlderReleasesState()
                    )
                }
            }
            is HomeUiAction.OnCloseAddSource -> {
                olderReleasesJob?.cancel()
                setState {
                    copy(
                        isAddSourceOpen = false,
                        addSourceUrl = "",
                        isFetchingSource = false,
                        fetchedRelease = null,
                        olderReleases = OlderReleasesState(),
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
            is HomeUiAction.OnPasteSourceUrlClick -> {
                sendEffect(HomeUiEffect.RequestPasteSourceUrl)
            }
            is HomeUiAction.OnFetchSourceReleases -> {
                fetchSourceReleases()
            }
            is HomeUiAction.OnLoadOlderReleases -> {
                loadOlderReleases()
            }
            is HomeUiAction.OnSelectAssetToTrack -> {
                trackAndDownloadAsset(action.asset, action.release, action.customName)
            }
            is HomeUiAction.OnDownloadApp -> {
                startDownload(action.app)
            }
            is HomeUiAction.OnCheckAppUpdate -> {
                checkSingleAppUpdate(action.app)
            }
            is HomeUiAction.OnCancelDownload -> {
                viewModelScope.launch {
                    downloadAppUseCase.cancelDownload(action.appId)
                }
            }
            is HomeUiAction.OnDeleteTrackedApp -> {
                viewModelScope.launch {
                    deleteTrackedAppUseCase(action.appId)
                    val msg = UiText.res(R.string.home_msg_app_removed)
                    sendEffect(HomeUiEffect.ShowSnackbar(msg))
                }
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
                setState { copy(isRefreshing = true) }
            }

            val result = checkAppUpdatesUseCase()
            if (result.isSuccess) {
                val updatedApps = result.getOrNull().orEmpty()
                val newUpdatesCount = updatedApps.count { it.hasUpdate }
                val message = if (newUpdatesCount > 0) {
                    UiText.plural(R.plurals.home_msg_updates_available, newUpdatesCount, newUpdatesCount)
                } else if (!silent) {
                    UiText.res(R.string.home_msg_all_up_to_date)
                } else {
                    null
                }

                setState { copy(isRefreshing = false) }
                if (message != null && !silent) {
                    sendEffect(HomeUiEffect.ShowSnackbar(message))
                }
            } else {
                val errorMsg = if (!silent) UiText.res(R.string.home_msg_check_failed, result.exceptionOrNull().toUiText()) else null
                setState { copy(isRefreshing = false) }
                if (errorMsg != null) {
                    sendEffect(HomeUiEffect.ShowSnackbar(errorMsg))
                }
            }
        }
    }

    /**
     * Checks one tracked app for a newer release without starting any download.
     */
    private fun checkSingleAppUpdate(app: TrackedApp) {
        viewModelScope.launch {
            setState { copy(isRefreshing = true) }

            val result = checkAppUpdatesUseCase.checkSingleApp(app)
            val message = if (result.isSuccess) {
                val updatedApp = result.getOrThrow()
                if (updatedApp.hasUpdate) {
                    UiText.res(R.string.home_msg_app_update_available, updatedApp.name, updatedApp.latestBuildVersion)
                } else {
                    UiText.res(R.string.home_msg_app_up_to_date, updatedApp.name)
                }
            } else {
                UiText.res(R.string.home_msg_check_failed, result.exceptionOrNull().toUiText())
            }

            setState { copy(isRefreshing = false) }
            sendEffect(HomeUiEffect.ShowSnackbar(message))
        }
    }

    private fun downloadAllAvailableUpdates() {
        val appsToUpdate = currentState.trackedApps.filter { it.hasUpdate }
        if (appsToUpdate.isNotEmpty()) {
            val msg = UiText.plural(R.plurals.home_msg_starting_downloads, appsToUpdate.size, appsToUpdate.size)
            sendEffect(HomeUiEffect.ShowSnackbar(msg))
            appsToUpdate.forEach { app ->
                if (currentState.downloadProgressMap[app.id] !is DownloadProgress.Downloading) {
                    startDownload(app)
                }
            }
        } else {
            viewModelScope.launch {
                setState { copy(isRefreshing = true) }
                val result = checkAppUpdatesUseCase()
                if (result.isSuccess) {
                    val updatedApps = result.getOrNull().orEmpty()
                    val newUpdates = updatedApps.filter { it.hasUpdate }
                    if (newUpdates.isNotEmpty()) {
                        val msg = UiText.plural(R.plurals.home_msg_found_updates, newUpdates.size, newUpdates.size)
                        setState { copy(isRefreshing = false) }
                        sendEffect(HomeUiEffect.ShowSnackbar(msg))
                        newUpdates.forEach { app ->
                            if (currentState.downloadProgressMap[app.id] !is DownloadProgress.Downloading) {
                                startDownload(app)
                            }
                        }
                    } else {
                        val msg = UiText.res(R.string.home_msg_all_up_to_date)
                        setState { copy(isRefreshing = false) }
                        sendEffect(HomeUiEffect.ShowSnackbar(msg))
                    }
                } else {
                    val errorMsg = UiText.res(R.string.home_msg_check_failed, result.exceptionOrNull().toUiText())
                    setState { copy(isRefreshing = false) }
                    sendEffect(HomeUiEffect.ShowSnackbar(errorMsg))
                }
            }
        }
    }

    /**
     * Loads older releases and appends only files that are not already listed.
     */
    private fun loadOlderReleases() {
        val latestRelease = currentState.fetchedRelease ?: return
        val sourceUrl = currentState.fetchedSourceUrl
        val older = currentState.olderReleases
        if (sourceUrl.isBlank() || older.isLoading || !older.hasMore) return

        olderReleasesJob = viewModelScope.launch {
            setState { copy(olderReleases = olderReleases.copy(isLoading = true, message = null)) }

            val listedReleases = listOf(latestRelease) + older.releases
            val result = fetchSourceReleasesUseCase.fetchOlderReleases(sourceUrl, listedReleases, older.lastLoadedPage)
            val page = result.getOrNull()

            val message = when {
                page == null -> result.exceptionOrNull().toUiText()
                page.releases.isNotEmpty() -> null
                page.hasMore -> UiText.plural(R.plurals.add_source_no_new_files, page.pagesChecked, page.pagesChecked)
                else -> UiText.res(R.string.add_source_no_other_files)
            }

            setState {
                copy(
                    olderReleases = olderReleases.copy(
                        releases = olderReleases.releases + page?.releases.orEmpty(),
                        lastLoadedPage = page?.lastLoadedPage ?: olderReleases.lastLoadedPage,
                        hasMore = page?.hasMore ?: olderReleases.hasMore,
                        isLoading = false,
                        message = message
                    )
                )
            }
        }
    }

    private fun fetchSourceReleases() {
        val url = currentState.addSourceUrl.trim()
        if (url.isBlank()) {
            setState { copy(addSourceError = UiText.res(R.string.add_source_error_empty_url)) }
            return
        }

        olderReleasesJob?.cancel()
        viewModelScope.launch {
            setState { copy(isFetchingSource = true, addSourceError = null, olderReleases = OlderReleasesState()) }

            val result = fetchSourceReleasesUseCase(url)
            if (result.isSuccess) {
                val release = result.getOrNull()
                setState {
                    copy(
                        isFetchingSource = false,
                        fetchedRelease = release,
                        fetchedSourceUrl = url,
                        addSourceError = if (release?.assets.isNullOrEmpty()) UiText.res(R.string.add_source_error_no_assets) else null
                    )
                }
            } else {
                setState {
                    copy(
                        isFetchingSource = false,
                        addSourceError = result.exceptionOrNull().toUiText()
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
            ?: asset.appName

        val newApp = TrackedApp(
            id = UUID.randomUUID().toString(),
            name = appName,
            sourceUrl = currentState.addSourceUrl.trim(),
            packageName = null,
            fileType = asset.fileType,
            arch = asset.arch,
            assetName = asset.name,
            latestVersion = release.tag,
            latestAssetName = asset.name,
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

            val msg = UiText.res(R.string.home_msg_app_added, appName)
            // Close Add App Source and give feedback
            setState {
                copy(
                    isAddSourceOpen = false,
                    addSourceUrl = "",
                    fetchedRelease = null
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
                    setState { copy(downloadProgressMap = downloadProgressMap + (app.id to progress)) }
                }
            }

            // Trigger download execution
            val result = downloadAppUseCase(app)

            progressJob.cancel()

            val msg = if (result.isSuccess) {
                UiText.res(R.string.home_msg_downloaded, app.name, app.latestBuildVersion)
            } else {
                UiText.res(R.string.home_msg_download_failed, result.exceptionOrNull().toUiText())
            }

            setState { copy(downloadProgressMap = downloadProgressMap - app.id) }
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
                    app.latestVersion.contains(query, ignoreCase = true) ||
                    app.latestBuildVersion.contains(query, ignoreCase = true)

            val matchesFormat = when (app.fileType) {
                AppFileType.APK -> showApks
                AppFileType.ZIP -> showModules
            }

            matchesQuery && matchesFormat
        }
    }
}
