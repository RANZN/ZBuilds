package com.ranzan.zbuilds

import com.ranzan.zbuilds.data.local.model.TrackedAppEntity
import com.ranzan.zbuilds.data.local.model.toDomain
import com.ranzan.zbuilds.data.local.model.toEntity
import com.ranzan.zbuilds.data.remote.mapper.toDomain
import com.ranzan.zbuilds.data.remote.model.GitHubAssetDto
import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.BuildVariant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DataMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `GitHub release maps to domain with only APK and ZIP files`() {
        val dto = GitHubReleaseDto(
            tagName = "v2.0",
            name = null,
            assets = listOf(
                GitHubAssetDto(name = "reddit-morphe-v2026.24.0-arm64-v8a-debug.apk", size = 10, browserDownloadUrl = "https://x/a.apk"),
                GitHubAssetDto(name = "module-v1.0.zip", size = 20, browserDownloadUrl = "https://x/m.zip"),
                GitHubAssetDto(name = "checksums.txt", size = 1, browserDownloadUrl = "https://x/c.txt")
            )
        )

        val release = dto.toDomain()

        assertEquals("v2.0", release.title)
        assertEquals(listOf("reddit-morphe-v2026.24.0-arm64-v8a-debug.apk", "module-v1.0.zip"), release.assets.map { it.name })
        val apk = release.assets.first()
        assertEquals(AppFileType.APK, apk.fileType)
        assertEquals("arm64-v8a", apk.arch)
        assertEquals(BuildVariant.DEBUG, apk.variant)
        assertEquals("v2.0", apk.releaseTag)
        assertEquals(AppFileType.ZIP, release.assets[1].fileType)
    }

    @Test
    fun `tracked app JSON already stored on devices still loads`() {
        // Same shape the previous version wrote to DataStore
        val stored = """[{"id":"1","name":"Reddit Morphe","sourceUrl":"https://github.com/j-hc/revanced-magisk-module",
            "packageName":"com.reddit.frontpage","fileType":"APK","arch":"all",
            "assetName":"reddit-morphe-v2026.24.0-all.apk","assetPattern":"reddit-morphe-{v}-all.apk",
            "downloadedVersion":"20221118","latestVersion":"20221119","latestAssetName":"",
            "latestDownloadUrl":"https://x/r.apk","latestSizeBytes":97252460,"localFilePath":"/files/r.apk",
            "installedVersion":"2026.24.0","isInstalled":true,"hasUpdate":false,"lastCheckedTimestamp":1793048101000}]"""

        val app = json.decodeFromString<List<TrackedAppEntity>>(stored).single().toDomain()

        assertEquals("Reddit Morphe", app.name)
        assertEquals(AppFileType.APK, app.fileType)
        assertEquals("2026.24.0", app.installedVersion)
        assertEquals(97252460L, app.latestSizeBytes)
        assertEquals(app, app.toEntity().toDomain())
    }
}
