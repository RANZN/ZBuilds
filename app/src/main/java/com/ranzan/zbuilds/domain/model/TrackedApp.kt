package com.ranzan.zbuilds.domain.model

import com.ranzan.zbuilds.core.utils.FormatHelper
import kotlinx.serialization.Serializable

@Serializable
data class TrackedApp(
    val id: String,
    val name: String,
    val sourceUrl: String,
    val packageName: String? = null,
    val fileType: AppFileType,
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
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Version of the newest available build of this file, taken from its file name
     * (falls back to the release tag when the name has no version).
     */
    val latestBuildVersion: String
        get() = FormatHelper.versionFromAssetName(latestAssetName.ifBlank { assetName }) ?: latestVersion

    /**
     * Version of the downloaded build, or blank if nothing has been downloaded yet.
     */
    val downloadedBuildVersion: String
        get() = if (downloadedVersion.isBlank()) "" else FormatHelper.versionFromAssetName(assetName) ?: downloadedVersion
}
