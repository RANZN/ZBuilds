package com.ranzan.zbuilds.domain.model

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
)
