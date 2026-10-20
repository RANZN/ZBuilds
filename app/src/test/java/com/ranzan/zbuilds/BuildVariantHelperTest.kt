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
}
