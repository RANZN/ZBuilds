package com.ranzan.zbuilds.core.utils

import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease

/**
 * Picks the asset in a new release that corresponds to the file a user originally tracked.
 * Releases often ship several files (per arch, per flavour, debug/release, companion apps),
 * so matching must compare the full file name shape rather than just the file type or arch.
 */
object AssetMatcher {

    private const val VERSION_PLACEHOLDER = "{v}"
    private const val MIN_SIMILARITY = 0.6

    private val dottedVersionRegex = Regex("""v?\d+(?:\.\d+)+""")
    private val standaloneNumberRegex = Regex("""(?<=^|[-_.+ ])v?\d+(?=$|[-_.+ ])""")
    private val tokenSplitRegex = Regex("""[^a-z0-9{}]+""")

    /**
     * Builds a version-independent pattern for an asset file name, e.g.
     * "MyApp-v1.2.3-arm64-v8a-release.apk" -> "myapp-{v}-arm64-v8a-release.apk".
     */
    fun toPattern(assetName: String, releaseTag: String? = null): String {
        var pattern = assetName.trim().lowercase()

        val cleanTag = releaseTag?.let { FormatHelper.cleanVersionString(it).lowercase() }.orEmpty()
        // Short tags like "1" would corrupt unrelated digits (e.g. "arm64"), so only strip meaningful tags
        if (cleanTag.length >= 3 && cleanTag.any { it.isDigit() }) {
            pattern = pattern.replace("v$cleanTag", VERSION_PLACEHOLDER)
                .replace(cleanTag, VERSION_PLACEHOLDER)
        }

        pattern = pattern.replace(dottedVersionRegex, VERSION_PLACEHOLDER)
        pattern = pattern.replace(standaloneNumberRegex, VERSION_PLACEHOLDER)
        return pattern
    }

    /**
     * Returns the asset from [assets] that matches the tracked file, or null when no asset
     * can be matched confidently. Never falls back to an unrelated file in the release.
     */
    fun findMatchingAsset(
        trackedAssetName: String,
        trackedPattern: String,
        trackedVersion: String?,
        fileType: AppFileType,
        arch: String,
        assets: List<ReleaseAsset>
    ): ReleaseAsset? {
        val candidates = assets.filter { it.fileType == fileType }
        if (candidates.isEmpty()) return null

        // 1. Same file name (assets without a version in the name)
        candidates.firstOrNull { it.name.equals(trackedAssetName, ignoreCase = true) }?.let { return it }

        // 2. Same file name shape once version numbers are removed
        val targetPatterns = buildSet {
            if (trackedPattern.isNotBlank()) add(trackedPattern.lowercase())
            if (trackedAssetName.isNotBlank()) add(toPattern(trackedAssetName, trackedVersion))
        }
        val patternMatches = candidates.filter { toPattern(it.name, it.releaseTag) in targetPatterns }
        if (patternMatches.size == 1) return patternMatches.first()
        if (patternMatches.size > 1) {
            return patternMatches.firstOrNull { it.arch.equals(arch, ignoreCase = true) } ?: patternMatches.first()
        }

        // 3. Only one file of this type in the release - it can only be this one
        if (candidates.size == 1) return candidates.first()

        // 4. Closest name with the same arch and build variant, if similar enough
        val trackedTokens = tokensOf(targetPatterns.firstOrNull() ?: return null)
        val trackedVariant = BuildVariantHelper.detectVariant(trackedAssetName)
        return candidates
            .filter { it.arch.equals(arch, ignoreCase = true) }
            .filter { BuildVariantHelper.detectVariant(it.name) == trackedVariant }
            .map { it to similarity(trackedTokens, tokensOf(toPattern(it.name, it.releaseTag))) }
            .filter { (_, score) -> score >= MIN_SIMILARITY }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    /**
     * Keeps only assets whose version-independent pattern is not in [knownPatterns],
     * so older releases only contribute files that are not already listed.
     * Releases left without assets are dropped.
     */
    fun filterNewAssets(releases: List<SourceRelease>, knownPatterns: Set<String>): List<SourceRelease> {
        val seen = knownPatterns.toMutableSet()
        return releases.mapNotNull { release ->
            val newAssets = release.assets.filter { asset -> seen.add(toPattern(asset.name, asset.releaseTag)) }
            if (newAssets.isEmpty()) null else release.copy(assets = newAssets)
        }
    }

    /**
     * Version-independent patterns of every asset in [releases].
     */
    fun patternsOf(releases: List<SourceRelease>): Set<String> =
        releases.flatMap { release -> release.assets.map { toPattern(it.name, it.releaseTag) } }.toSet()

    private fun tokensOf(pattern: String): Set<String> =
        pattern.split(tokenSplitRegex)
            .filter { it.isNotBlank() && it != VERSION_PLACEHOLDER }
            .toSet()

    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val union = (a union b).size
        return if (union == 0) 0.0 else (a intersect b).size.toDouble() / union
    }
}
