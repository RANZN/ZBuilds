package com.ranzan.zbuilds.domain.rules

import com.ranzan.zbuilds.domain.model.TrackedApp

/**
 * Rules for reading and comparing build versions.
 */
object VersionRules {

    private val assetVersionRegex = Regex("""(?<![a-z0-9])v?(\d+(?:\.\d+)+)""", RegexOption.IGNORE_CASE)

    fun cleanVersionString(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Extracts the build version from an asset file name or tag,
     * e.g. "reddit-morphe-v2026.24.0-all.apk" -> "2026.24.0". Returns null if none is present.
     */
    fun versionFromAssetName(name: String): String? =
        assetVersionRegex.find(name)?.groupValues?.get(1)
}

/**
 * Version of the newest available build of this file, taken from its file name
 * (falls back to the release tag when the name has no version).
 */
val TrackedApp.latestBuildVersion: String
    get() = VersionRules.versionFromAssetName(latestAssetName.ifBlank { assetName }) ?: latestVersion

/**
 * Version of the downloaded build, or blank if nothing has been downloaded yet.
 */
val TrackedApp.downloadedBuildVersion: String
    get() = if (downloadedVersion.isBlank()) "" else VersionRules.versionFromAssetName(assetName) ?: downloadedVersion

/**
 * True when the installed app already matches the newest build on the server,
 * so installing the APK again would change nothing.
 */
val TrackedApp.isLatestBuildInstalled: Boolean
    get() = isInstalled && !installedVersion.isNullOrBlank() &&
        VersionRules.cleanVersionString(installedVersion) == VersionRules.cleanVersionString(latestBuildVersion)
