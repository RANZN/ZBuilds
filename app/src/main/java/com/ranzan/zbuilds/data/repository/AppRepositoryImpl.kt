package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.core.utils.ArchitectureHelper
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.remote.SourceUrlParser
import com.ranzan.zbuilds.domain.model.AppFileType
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
            installedVersion = installedVersion
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
                        val matchedAsset = findMatchingAsset(app, release)
                        val latestTag = release.tag

                        // Clean version strings for reliable comparison
                        val cleanLatest = FormatHelper.cleanVersionString(latestTag)
                        val cleanDownloaded = FormatHelper.cleanVersionString(app.downloadedVersion)

                        var isInstalled = app.isInstalled
                        var installedVersion = app.installedVersion
                        if (!app.packageName.isNullOrBlank()) {
                            isInstalled = packageInspector.isPackageInstalled(app.packageName)
                            installedVersion = packageInspector.getInstalledVersionName(app.packageName)
                        }

                        val cleanInstalled = installedVersion?.let { FormatHelper.cleanVersionString(it) }

                        // Has update if latest is different from downloaded, or different from installed
                        val hasUpdate = (cleanLatest != cleanDownloaded && cleanDownloaded.isNotBlank()) ||
                                (cleanInstalled != null && cleanLatest != cleanInstalled)

                        val updatedApp = app.copy(
                            latestVersion = latestTag,
                            latestDownloadUrl = matchedAsset?.downloadUrl ?: app.latestDownloadUrl,
                            latestSizeBytes = matchedAsset?.sizeBytes ?: app.latestSizeBytes,
                            isInstalled = isInstalled,
                            installedVersion = installedVersion,
                            hasUpdate = hasUpdate,
                            lastCheckedTimestamp = System.currentTimeMillis()
                        )
                        updatedList.add(updatedApp)
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
                return Result.failure(releaseResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val release = releaseResult.getOrThrow()
            val matchedAsset = findMatchingAsset(app, release)
            val latestTag = release.tag

            val cleanLatest = FormatHelper.cleanVersionString(latestTag)
            val cleanDownloaded = FormatHelper.cleanVersionString(app.downloadedVersion)

            var isInstalled = app.isInstalled
            var installedVersion = app.installedVersion
            if (!app.packageName.isNullOrBlank()) {
                isInstalled = packageInspector.isPackageInstalled(app.packageName)
                installedVersion = packageInspector.getInstalledVersionName(app.packageName)
            }

            val cleanInstalled = installedVersion?.let { FormatHelper.cleanVersionString(it) }

            val hasUpdate = (cleanLatest != cleanDownloaded && cleanDownloaded.isNotBlank()) ||
                    (cleanInstalled != null && cleanLatest != cleanInstalled)

            val updatedApp = app.copy(
                latestVersion = latestTag,
                latestDownloadUrl = matchedAsset?.downloadUrl ?: app.latestDownloadUrl,
                latestSizeBytes = matchedAsset?.sizeBytes ?: app.latestSizeBytes,
                isInstalled = isInstalled,
                installedVersion = installedVersion,
                hasUpdate = hasUpdate,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            trackedAppsDataStore.updateTrackedApp(updatedApp)
            Result.success(updatedApp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findMatchingAsset(app: TrackedApp, release: SourceRelease): com.ranzan.zbuilds.domain.model.ReleaseAsset? {
        val sameTypeAssets = release.assets.filter { it.fileType == app.fileType }
        if (sameTypeAssets.isEmpty()) return null

        // 1. Try matching by exact asset name if filename doesn't contain version
        val exactMatch = sameTypeAssets.firstOrNull { it.name.equals(app.assetName, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        // 2. Try matching by normalized base name and architecture
        val appBaseName = FormatHelper.deriveAppNameFromAsset(app.assetName).lowercase()
        val archMatches = sameTypeAssets.filter { asset ->
            val assetBaseName = FormatHelper.deriveAppNameFromAsset(asset.name).lowercase()
            assetBaseName == appBaseName && asset.arch.equals(app.arch, ignoreCase = true)
        }
        if (archMatches.isNotEmpty()) return archMatches.first()

        // 3. Match by architecture among matching file type
        val archOnlyMatches = sameTypeAssets.filter { it.arch.equals(app.arch, ignoreCase = true) }
        if (archOnlyMatches.isNotEmpty()) return archOnlyMatches.first()

        // 4. Fallback to first compatible asset
        return sameTypeAssets.firstOrNull { ArchitectureHelper.isArchCompatible(it.arch) } ?: sameTypeAssets.first()
    }
}
