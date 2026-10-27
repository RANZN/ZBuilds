package com.ranzan.zbuilds.core.utils

import com.ranzan.zbuilds.domain.model.TrackedApp

/**
 * Version of the newest available build of this file, taken from its file name
 * (falls back to the release tag when the name has no version).
 */
val TrackedApp.latestBuildVersion: String
    get() = FormatHelper.versionFromAssetName(latestAssetName.ifBlank { assetName }) ?: latestVersion

/**
 * Version of the downloaded build, or blank if nothing has been downloaded yet.
 */
val TrackedApp.downloadedBuildVersion: String
    get() = if (downloadedVersion.isBlank()) "" else FormatHelper.versionFromAssetName(assetName) ?: downloadedVersion

/**
 * True when the installed app already matches the newest build on the server,
 * so installing the APK again would change nothing.
 */
val TrackedApp.isLatestBuildInstalled: Boolean
    get() = isInstalled && !installedVersion.isNullOrBlank() &&
        FormatHelper.cleanVersionString(installedVersion) == FormatHelper.cleanVersionString(latestBuildVersion)
