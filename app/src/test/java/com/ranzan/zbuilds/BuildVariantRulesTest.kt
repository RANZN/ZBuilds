package com.ranzan.zbuilds

import com.ranzan.zbuilds.domain.model.BuildVariant
import com.ranzan.zbuilds.domain.rules.BuildVariantRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildVariantRulesTest {

    @Test
    fun `detectVariant identifies debug builds correctly`() {
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.detectVariant("app-debug.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.detectVariant("zbuilds_v1.0.0_debug.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.detectVariant("MyApp.Debug.zip"))
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.detectVariant("debug-release-debug.apk"))
    }

    @Test
    fun `detectVariant identifies release builds correctly`() {
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.detectVariant("app-release.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.detectVariant("app-release-unsigned.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.detectVariant("zbuilds_release_arm64.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.detectVariant("MyApp-Release.zip"))
    }

    @Test
    fun `detectVariant returns null when no variant information is present`() {
        assertNull(BuildVariantRules.detectVariant("app.apk"))
        assertNull(BuildVariantRules.detectVariant("magisk-v27.0.zip"))
        assertNull(BuildVariantRules.detectVariant("termux-v0.118.apk"))
        assertNull(BuildVariantRules.detectVariant(""))
        assertNull(BuildVariantRules.detectVariant(null))
    }

    @Test
    fun `resolveAssetVariant resolves debug and release builds accurately`() {
        // Explicit asset names
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.resolveAssetVariant("app-debug.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("app-release.apk"))

        // Release tag with debug
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.resolveAssetVariant("app.apk", "v1.0.0-debug"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("app.apk", "v1.0.0-release"))

        // Generic GitHub release names without debug keyword default to RELEASE
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("YouTube-Morphe-21.20.405.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("Spotube-android-all.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("NewPipe_v0.27.0.apk"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant("revanced-magisk-module-v21.20.405.zip"))
        assertEquals(BuildVariant.RELEASE, BuildVariantRules.resolveAssetVariant(null, null))

        // Contains debug or dev in asset name
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.resolveAssetVariant("termux-app_v0.118.1+github-debug_arm64-v8a.apk"))
        assertEquals(BuildVariant.DEBUG, BuildVariantRules.resolveAssetVariant("app-dev-arm64.apk"))
    }
}
