package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.remote.mapper.toDomain
import com.ranzan.zbuilds.domain.model.AppError
import com.ranzan.zbuilds.domain.model.OlderReleasesPage
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.AppRepository
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import com.ranzan.zbuilds.domain.rules.AssetMatcher
import com.ranzan.zbuilds.domain.rules.VersionRules
import com.ranzan.zbuilds.domain.rules.downloadedBuildVersion
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val gitHubApiService: GitHubApiService,
    private val trackedAppsDataStore: TrackedAppsDataStore,
    private val packageInspector: PackageInspectorRepository
) : AppRepository {

    override fun getTrackedApps(): Flow<List<TrackedApp>> =
        trackedAppsDataStore.getTrackedApps()
            .map { apps ->
                // Drop file paths whose downloaded file has been deleted, so the UI only reads state
                apps.map { app ->
                    if (app.localFilePath != null && !File(app.localFilePath).exists()) app.copy(localFilePath = null) else app
                }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun addTrackedApp(app: TrackedApp) {
        // Check if package is already installed on device
        var isInstalled = app.isInstalled
        var installedVersion = app.installedVersion

        if (!app.packageName.isNullOrBlank()) {
            isInstalled = packageInspector.isPackageInstalled(app.packageName)
            installedVersion = packageInspector.getInstalledVersionName(app.packageName)
        }

        val enrichedApp = app.copy(
            isInstalled = isInstalled,
            installedVersion = installedVersion,
            // Version-independent file name pattern used to find this file in future releases
            assetPattern = app.assetPattern.ifBlank { AssetMatcher.toPattern(app.assetName, app.latestVersion) }
        )
        trackedAppsDataStore.addTrackedApp(enrichedApp)
    }

    override suspend fun updateTrackedApp(app: TrackedApp) {
        trackedAppsDataStore.updateTrackedApp(app)
    }

    override suspend fun deleteTrackedApp(id: String) {
        trackedAppsDataStore.deleteTrackedApp(id)
    }

    override suspend fun fetchSourceReleases(sourceUrl: String): Result<SourceRelease> =
        fetchActiveRelease(sourceUrl)

    /**
     * Latest release that contains APK or ZIP files (or the newest release when none does).
     */
    private suspend fun fetchActiveRelease(sourceUrl: String): Result<SourceRelease> {
        val releases = gitHubApiService.fetchReleases(sourceUrl).getOrElse { return Result.failure(it) }
            .map { it.toDomain() }
        if (releases.isEmpty()) return Result.failure(AppError.NoReleases())
        return Result.success(releases.firstOrNull { it.assets.isNotEmpty() } ?: releases.first())
    }

    /**
     * Pages through older releases and keeps only files not already in [listedReleases].
     * Checks up to [MAX_PAGES_PER_LOAD] pages so one call stops once something new is found.
     */
    override suspend fun fetchOlderReleases(
        sourceUrl: String,
        listedReleases: List<SourceRelease>,
        afterPage: Int
    ): Result<OlderReleasesPage> {
        val listedTags = listedReleases.map { it.tag }.toSet()
        val knownPatterns = AssetMatcher.patternsOf(listedReleases).toMutableSet()
        val newReleases = mutableListOf<SourceRelease>()
        var page = afterPage
        var hasMore = true

        while (hasMore && newReleases.isEmpty() && page - afterPage < MAX_PAGES_PER_LOAD) {
            val result = gitHubApiService.fetchReleasePage(sourceUrl, page + 1, RELEASES_PER_PAGE)
            val releases = result.getOrElse { return Result.failure(it) }.map { it.toDomain() }
            page++
            hasMore = releases.size == RELEASES_PER_PAGE

            val filtered = AssetMatcher.filterNewAssets(releases.filter { it.tag !in listedTags }, knownPatterns)
            knownPatterns += AssetMatcher.patternsOf(filtered)
            newReleases += filtered
        }

        return Result.success(
            OlderReleasesPage(
                releases = newReleases,
                lastLoadedPage = page,
                pagesChecked = page - afterPage,
                hasMore = hasMore
            )
        )
    }

    override suspend fun checkForUpdates(): Result<List<TrackedApp>> {
        return try {
            val currentApps = trackedAppsDataStore.getTrackedApps().first()
            if (currentApps.isEmpty()) {
                return Result.success(emptyList())
            }

            // Group by sourceUrl to minimize duplicate network requests for same repository
            val sourceGroups = currentApps.groupBy { it.sourceUrl }
            val updatedList = mutableListOf<TrackedApp>()

            for ((sourceUrl, appsInSource) in sourceGroups) {
                val releaseResult = fetchActiveRelease(sourceUrl)
                if (releaseResult.isSuccess) {
                    val release = releaseResult.getOrThrow()
                    for (app in appsInSource) {
                        updatedList.add(applyRelease(app, release))
                    }
                } else {
                    // Retain current app info with updated package inspector status
                    for (app in appsInSource) {
                        updatedList.add(refreshInstallStatus(app).copy(lastCheckedTimestamp = System.currentTimeMillis()))
                    }
                }
            }

            trackedAppsDataStore.saveTrackedApps(updatedList)
            Result.success(updatedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkAppUpdate(app: TrackedApp): Result<TrackedApp> {
        return try {
            val releaseResult = fetchActiveRelease(app.sourceUrl)
            if (releaseResult.isFailure) {
                return Result.failure(releaseResult.exceptionOrNull() ?: IllegalStateException())
            }

            val updatedApp = applyRelease(app, releaseResult.getOrThrow())
            trackedAppsDataStore.updateTrackedApp(updatedApp)
            Result.success(updatedApp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes install status and the latest build info for [app] from [release].
     * Versions are compared per build (from each file name), not by the shared release tag,
     * because one release can ship several apps with different versions.
     */
    private fun applyRelease(trackedApp: TrackedApp, release: SourceRelease): TrackedApp {
        val app = refreshInstallStatus(trackedApp)
        val matchedAsset = findMatchingAsset(app, release)
        val installedVersion = app.installedVersion

        // Only flag an update when the tracked file exists in the new release,
        // otherwise the download would fetch a stale or unrelated file
        val hasUpdate = matchedAsset != null && run {
            val versionFromFile = VersionRules.versionFromAssetName(matchedAsset.name)
                ?: VersionRules.versionFromAssetName(release.tag)
            val cleanLatest = VersionRules.cleanVersionString(versionFromFile ?: release.tag)
            val cleanDownloaded = VersionRules.cleanVersionString(app.downloadedBuildVersion)
            val cleanInstalled = installedVersion?.let { VersionRules.cleanVersionString(it) }

            (cleanDownloaded.isNotBlank() && cleanLatest != cleanDownloaded) ||
                // Installed version names can only be compared with a real version number
                (versionFromFile != null && cleanInstalled != null && cleanLatest != cleanInstalled)
        }

        return app.copy(
            latestVersion = if (matchedAsset != null) release.tag else app.latestVersion,
            latestAssetName = matchedAsset?.name ?: app.latestAssetName,
            assetPattern = matchedAsset?.let { AssetMatcher.toPattern(it.name, it.releaseTag) } ?: app.assetPattern,
            latestDownloadUrl = matchedAsset?.downloadUrl ?: app.latestDownloadUrl,
            latestSizeBytes = matchedAsset?.sizeBytes ?: app.latestSizeBytes,
            hasUpdate = hasUpdate,
            lastCheckedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Updates the package name (read from the downloaded APK when missing) and install status.
     */
    private fun refreshInstallStatus(app: TrackedApp): TrackedApp {
        val packageName = app.packageName?.takeIf { it.isNotBlank() }
            ?: app.localFilePath
                ?.takeIf { it.endsWith(".apk", ignoreCase = true) && File(it).exists() }
                ?.let { packageInspector.inspectApkPackageName(it) }
        if (packageName.isNullOrBlank()) return app

        return app.copy(
            packageName = packageName,
            isInstalled = packageInspector.isPackageInstalled(packageName),
            installedVersion = packageInspector.getInstalledVersionName(packageName)
        )
    }

    private fun findMatchingAsset(app: TrackedApp, release: SourceRelease): ReleaseAsset? =
        AssetMatcher.findMatchingAsset(
            trackedAssetName = app.assetName,
            trackedPattern = app.assetPattern,
            trackedVersion = app.downloadedVersion.ifBlank { null },
            fileType = app.fileType,
            arch = app.arch,
            assets = release.assets
        )

    private companion object {
        const val RELEASES_PER_PAGE = 10
        const val MAX_PAGES_PER_LOAD = 3
    }
}
