package com.ranzan.zbuilds

import com.ranzan.zbuilds.core.utils.BuildVariant
import com.ranzan.zbuilds.core.utils.BuildVariantHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildVariantHelperTest {

    @Test
    fun `detectVariant identifies debug builds correctly`() {
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.detectVariant("app-debug.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.detectVariant("zbuilds_v1.0.0_debug.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.detectVariant("MyApp.Debug.zip"))
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.detectVariant("debug-release-debug.apk"))
    }

    @Test
    fun `detectVariant identifies release builds correctly`() {
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.detectVariant("app-release.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.detectVariant("app-release-unsigned.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.detectVariant("zbuilds_release_arm64.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.detectVariant("MyApp-Release.zip"))
    }

    @Test
    fun `detectVariant returns null when no variant information is present`() {
        assertNull(BuildVariantHelper.detectVariant("app.apk"))
        assertNull(BuildVariantHelper.detectVariant("magisk-v27.0.zip"))
        assertNull(BuildVariantHelper.detectVariant("termux-v0.118.apk"))
        assertNull(BuildVariantHelper.detectVariant(""))
        assertNull(BuildVariantHelper.detectVariant(null))
    }

    @Test
    fun `detectAppVariant falls back to asset name or url when context is null`() {
        val debugVariant = BuildVariantHelper.detectAppVariant(
            context = null,
            packageName = null,
            isInstalled = false,
            localFilePath = null,
            assetName = "my-awesome-app-debug.apk"
        )
        assertEquals(BuildVariant.DEBUG, debugVariant)

        val releaseVariant = BuildVariantHelper.detectAppVariant(
            context = null,
            packageName = null,
            isInstalled = false,
            localFilePath = null,
            assetName = "my-awesome-app-release.apk"
        )
        assertEquals(BuildVariant.RELEASE, releaseVariant)

        val unknownVariant = BuildVariantHelper.detectAppVariant(
            context = null,
            packageName = null,
            isInstalled = false,
            localFilePath = null,
            assetName = "my-awesome-app.apk"
        )
        assertNull(unknownVariant)
    }

    @Test
    fun `resolveAssetVariant resolves debug and release builds accurately`() {
        // Explicit asset names
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.resolveAssetVariant("app-debug.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("app-release.apk"))

        // Release tag with debug
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.resolveAssetVariant("app.apk", "v1.0.0-debug"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("app.apk", "v1.0.0-release"))

        // Generic GitHub release names without debug keyword default to RELEASE
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("YouTube-Morphe-21.20.405.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("Spotube-android-all.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("NewPipe_v0.27.0.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant("revanced-magisk-module-v21.20.405.zip"))
        assertEquals(BuildVariant.RELEASE, BuildVariantHelper.resolveAssetVariant(null, null))

        // Contains debug or dev in asset name
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.resolveAssetVariant("termux-app_v0.118.1+github-debug_arm64-v8a.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantHelper.resolveAssetVariant("app-dev-arm64.apk"))
    }
}
