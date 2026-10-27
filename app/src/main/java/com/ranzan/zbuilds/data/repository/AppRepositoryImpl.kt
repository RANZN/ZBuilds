package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.core.utils.AssetMatcher
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.core.utils.downloadedBuildVersion
import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.remote.SourceUrlParser
import com.ranzan.zbuilds.domain.model.OlderReleasesPage
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.AppRepository
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepositoryImpl(
    private val gitHubApiService: GitHubApiService,
    private val trackedAppsDataStore: TrackedAppsDataStore,
    private val packageInspector: PackageInspectorRepository
) : AppRepository {

    override fun getTrackedApps(): Flow<List<TrackedApp>> =
        trackedAppsDataStore.getTrackedApps()

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

    override suspend fun fetchSourceReleases(sourceUrl: String): Result<SourceRelease> {
        return gitHubApiService.fetchReleases(sourceUrl)
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
            val releases = result.getOrElse { return Result.failure(it) }
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
                val releaseResult = gitHubApiService.fetchReleases(sourceUrl)
                if (releaseResult.isSuccess) {
                    val release = releaseResult.getOrThrow()
                    for (app in appsInSource) {
                        updatedList.add(applyRelease(app, release))
                    }
                } else {
                    // Retain current app info with updated package inspector status
                    for (app in appsInSource) {
                        var isInstalled = app.isInstalled
                        var installedVersion = app.installedVersion
                        if (!app.packageName.isNullOrBlank()) {
                            isInstalled = packageInspector.isPackageInstalled(app.packageName)
                            installedVersion = packageInspector.getInstalledVersionName(app.packageName)
                        }
                        updatedList.add(
                            app.copy(
                                isInstalled = isInstalled,
                                installedVersion = installedVersion,
                                lastCheckedTimestamp = System.currentTimeMillis()
                            )
                        )
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
            val releaseResult = gitHubApiService.fetchReleases(app.sourceUrl)
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
    private fun applyRelease(app: TrackedApp, release: SourceRelease): TrackedApp {
        val matchedAsset = findMatchingAsset(app, release)

        var isInstalled = app.isInstalled
        var installedVersion = app.installedVersion
        if (!app.packageName.isNullOrBlank()) {
            isInstalled = packageInspector.isPackageInstalled(app.packageName)
            installedVersion = packageInspector.getInstalledVersionName(app.packageName)
        }

        // Only flag an update when the tracked file exists in the new release,
        // otherwise the download would fetch a stale or unrelated file
        val hasUpdate = matchedAsset != null && run {
            val versionFromFile = FormatHelper.versionFromAssetName(matchedAsset.name)
                ?: FormatHelper.versionFromAssetName(release.tag)
            val cleanLatest = FormatHelper.cleanVersionString(versionFromFile ?: release.tag)
            val cleanDownloaded = FormatHelper.cleanVersionString(app.downloadedBuildVersion)
            val cleanInstalled = installedVersion?.let { FormatHelper.cleanVersionString(it) }

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
            isInstalled = isInstalled,
            installedVersion = installedVersion,
            hasUpdate = hasUpdate,
            lastCheckedTimestamp = System.currentTimeMillis()
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
