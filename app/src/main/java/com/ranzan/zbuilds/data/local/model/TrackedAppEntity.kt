package com.ranzan.zbuilds.data.local.model

import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.TrackedApp
import kotlinx.serialization.Serializable

/**
 * Stored form of a tracked app. Field names match the JSON already saved on devices,
 * so existing tracked apps keep loading.
 */
@Serializable
data class TrackedAppEntity(
    val id: String,
    val name: String,
    val sourceUrl: String,
    val packageName: String? = null,
    val fileType: String,
    val arch: String = "all",
    val assetName: String,
    val assetPattern: String = "",
    val downloadedVersion: String,
    val latestVersion: String,
    val latestAssetName: String = "",
    val latestDownloadUrl: String,
    val latestSizeBytes: Long = 0L,
    val localFilePath: String? = null,
    val installedVersion: String? = null,
    val isInstalled: Boolean = false,
    val hasUpdate: Boolean = false,
    val lastCheckedTimestamp: Long = 0L
)

fun TrackedAppEntity.toDomain(): TrackedApp = TrackedApp(
    id = id,
    name = name,
    sourceUrl = sourceUrl,
    packageName = packageName,
    fileType = AppFileType.entries.firstOrNull { it.name == fileType } ?: AppFileType.APK,
    arch = arch,
    assetName = assetName,
    assetPattern = assetPattern,
    downloadedVersion = downloadedVersion,
    latestVersion = latestVersion,
    latestAssetName = latestAssetName,
    latestDownloadUrl = latestDownloadUrl,
    latestSizeBytes = latestSizeBytes,
    localFilePath = localFilePath,
    installedVersion = installedVersion,
    isInstalled = isInstalled,
    hasUpdate = hasUpdate,
    lastCheckedTimestamp = lastCheckedTimestamp
)

fun TrackedApp.toEntity(): TrackedAppEntity = TrackedAppEntity(
    id = id,
    name = name,
    sourceUrl = sourceUrl,
    packageName = packageName,
    fileType = fileType.name,
    arch = arch,
    assetName = assetName,
    assetPattern = assetPattern,
    downloadedVersion = downloadedVersion,
    latestVersion = latestVersion,
    latestAssetName = latestAssetName,
    latestDownloadUrl = latestDownloadUrl,
    latestSizeBytes = latestSizeBytes,
    localFilePath = localFilePath,
    installedVersion = installedVersion,
    isInstalled = isInstalled,
    hasUpdate = hasUpdate,
    lastCheckedTimestamp = lastCheckedTimestamp
)
