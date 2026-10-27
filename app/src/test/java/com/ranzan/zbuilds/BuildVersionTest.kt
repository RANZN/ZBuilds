package com.ranzan.zbuilds

import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.rules.VersionRules
import com.ranzan.zbuilds.domain.rules.downloadedBuildVersion
import com.ranzan.zbuilds.domain.rules.isLatestBuildInstalled
import com.ranzan.zbuilds.domain.rules.latestBuildVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildVersionTest {

    private fun app(
        assetName: String,
        latestAssetName: String = "",
        latestVersion: String = "20221119",
        downloadedVersion: String = "20221118"
    ) = TrackedApp(
        id = "1",
        name = "App",
        sourceUrl = "https://github.com/owner/repo",
        fileType = AppFileType.APK,
        assetName = assetName,
        latestAssetName = latestAssetName,
        downloadedVersion = downloadedVersion,
        latestVersion = latestVersion,
        latestDownloadUrl = ""
    )

    @Test
    fun `versionFromAssetName reads the version in the file name`() {
        assertEquals("2026.24.0", VersionRules.versionFromAssetName("reddit-morphe-v2026.24.0-all.apk"))
        assertEquals("9.20.53", VersionRules.versionFromAssetName("music-morphe-v9.20.53-arm64-v8a.apk"))
        assertEquals("0.118.1", VersionRules.versionFromAssetName("termux-app_v0.118.1+github-debug_arm64-v8a.apk"))
        assertEquals("1.2.3", VersionRules.versionFromAssetName("v1.2.3"))
        assertNull(VersionRules.versionFromAssetName("app-arm64-v8a-release.apk"))
        assertNull(VersionRules.versionFromAssetName("20221119"))
    }

    @Test
    fun `build versions come from each file, not the shared release tag`() {
        val reddit = app(assetName = "reddit-morphe-v2026.24.0-all.apk")
        assertEquals("2026.24.0", reddit.latestBuildVersion)
        assertEquals("2026.24.0", reddit.downloadedBuildVersion)

        val updated = reddit.copy(latestAssetName = "reddit-morphe-v2026.30.0-all.apk")
        assertEquals("2026.30.0", updated.latestBuildVersion)
        assertEquals("2026.24.0", updated.downloadedBuildVersion)
    }

    @Test
    fun `falls back to the release tag when the file name has no version`() {
        val app = app(assetName = "app-release.apk", latestVersion = "v3.0", downloadedVersion = "v2.0")
        assertEquals("v3.0", app.latestBuildVersion)
        assertEquals("v2.0", app.downloadedBuildVersion)
        assertEquals("", app.copy(downloadedVersion = "").downloadedBuildVersion)
    }

    @Test
    fun `latest build is installed only when installed version matches the server build`() {
        val reddit = app(assetName = "reddit-morphe-v2026.24.0-all.apk")
        assertTrue(reddit.copy(isInstalled = true, installedVersion = "2026.24.0").isLatestBuildInstalled)
        assertFalse(reddit.copy(isInstalled = true, installedVersion = "2026.20.0").isLatestBuildInstalled)
        assertFalse(reddit.copy(isInstalled = false, installedVersion = "2026.24.0").isLatestBuildInstalled)

        val newerOnServer = reddit.copy(
            isInstalled = true,
            installedVersion = "2026.24.0",
            latestAssetName = "reddit-morphe-v2026.30.0-all.apk"
        )
        assertFalse(newerOnServer.isLatestBuildInstalled)
    }
}
