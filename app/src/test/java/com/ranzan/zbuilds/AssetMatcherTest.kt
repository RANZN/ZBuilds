package com.ranzan.zbuilds

import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.BuildVariant
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.rules.AssetMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetMatcherTest {

    private fun asset(name: String, tag: String, arch: String = "all", type: AppFileType = AppFileType.APK) =
        ReleaseAsset(
            name = name,
            appName = name,
            downloadUrl = "https://example.com/$tag/$name",
            sizeBytes = 1L,
            fileType = type,
            arch = arch,
            variant = BuildVariant.RELEASE,
            releaseTag = tag
        )

    private fun match(trackedName: String, trackedTag: String, arch: String, assets: List<ReleaseAsset>) =
        AssetMatcher.findMatchingAsset(
            trackedAssetName = trackedName,
            trackedPattern = AssetMatcher.toPattern(trackedName, trackedTag),
            trackedVersion = trackedTag,
            fileType = AppFileType.APK,
            arch = arch,
            assets = assets
        )

    @Test
    fun `toPattern removes version numbers but keeps arch and variant`() {
        assertEquals("myapp-{v}-arm64-v8a-release.apk", AssetMatcher.toPattern("MyApp-v1.2.3-arm64-v8a-release.apk", "v1.2.3"))
        assertEquals("youtube-morphe-{v}.apk", AssetMatcher.toPattern("YouTube-Morphe-21.20.405.apk"))
        assertEquals("app-{v}.apk", AssetMatcher.toPattern("app-1234.apk"))
    }

    @Test
    fun `picks the tracked app when release ships several apps`() {
        val release = listOf(
            asset("YouTube-Music-Morphe-8.40.1.apk", "v2.0"),
            asset("YouTube-Morphe-21.30.100.apk", "v2.0"),
            asset("Reddit-Morphe-2025.10.0.apk", "v2.0")
        )
        val matched = match("YouTube-Morphe-21.20.405.apk", "v1.0", "all", release)
        assertEquals("YouTube-Morphe-21.30.100.apk", matched?.name)
    }

    @Test
    fun `keeps arch and variant of the tracked file`() {
        val release = listOf(
            asset("app-v1.1.0-arm64-v8a-debug.apk", "v1.1.0", "arm64-v8a"),
            asset("app-v1.1.0-armeabi-v7a-release.apk", "v1.1.0", "armeabi-v7a"),
            asset("app-v1.1.0-arm64-v8a-release.apk", "v1.1.0", "arm64-v8a"),
            asset("app-v1.1.0-universal-release.apk", "v1.1.0")
        )
        val matched = match("app-v1.0.0-arm64-v8a-release.apk", "v1.0.0", "arm64-v8a", release)
        assertEquals("app-v1.1.0-arm64-v8a-release.apk", matched?.name)

        val debugMatched = match("app-v1.0.0-arm64-v8a-debug.apk", "v1.0.0", "arm64-v8a", release)
        assertEquals("app-v1.1.0-arm64-v8a-debug.apk", debugMatched?.name)
    }

    @Test
    fun `exact name match wins for unversioned assets`() {
        val release = listOf(
            asset("app-debug.apk", "v3"),
            asset("app-release.apk", "v3")
        )
        assertEquals("app-release.apk", match("app-release.apk", "v2", "all", release)?.name)
    }

    @Test
    fun `single asset of the same type is always matched`() {
        val release = listOf(
            asset("renamed-app-2.0.apk", "v2.0"),
            asset("module-2.0.zip", "v2.0", type = AppFileType.ZIP)
        )
        assertEquals("renamed-app-2.0.apk", match("old-app-1.0.apk", "v1.0", "all", release)?.name)
    }

    @Test
    fun `returns null instead of an unrelated file`() {
        val release = listOf(
            asset("Spotify-Patched-9.0.apk", "v2.0"),
            asset("Instagram-Patched-300.0.apk", "v2.0")
        )
        assertNull(match("YouTube-Morphe-21.20.405.apk", "v1.0", "all", release))
    }

    private fun release(tag: String, vararg names: String) =
        SourceRelease(tag = tag, title = tag, publishedAt = "", body = "", assets = names.map { asset(it, tag) })

    @Test
    fun `filterNewAssets keeps only files not already listed`() {
        val latest = release("v3.0", "YouTube-Morphe-3.0.apk", "Music-Morphe-3.0.apk")
        val older = listOf(
            release("v2.0", "YouTube-Morphe-2.0.apk", "Reddit-Morphe-2.0.apk"),
            release("v1.0", "Music-Morphe-1.0.apk", "Reddit-Morphe-1.0.apk", "Twitch-Morphe-1.0.apk")
        )

        val result = AssetMatcher.filterNewAssets(older, AssetMatcher.patternsOf(listOf(latest)))

        assertEquals(listOf("v2.0", "v1.0"), result.map { it.tag })
        assertEquals(listOf("Reddit-Morphe-2.0.apk"), result[0].assets.map { it.name })
        assertEquals(listOf("Twitch-Morphe-1.0.apk"), result[1].assets.map { it.name })
    }

    @Test
    fun `filterNewAssets drops releases with nothing new`() {
        val latest = release("v2.0", "app-2.0.apk")
        val older = listOf(release("v1.0", "app-1.0.apk"))
        assertEquals(emptyList<SourceRelease>(), AssetMatcher.filterNewAssets(older, AssetMatcher.patternsOf(listOf(latest))))
    }
}
