package com.ranzan.zbuilds.domain.rules

import com.ranzan.zbuilds.domain.model.BuildVariant

object BuildVariantRules {
    /**
     * Inspects a filename, asset name, or title to detect if it explicitly represents
     * a Debug or Release build.
     * Returns null if no explicit variant information is found.
     */
    fun detectVariant(text: String?): BuildVariant? {
        if (text.isNullOrBlank()) return null
        val lower = text.lowercase()

        // Strip file extensions for clean tokenization
        val clean = lower.removeSuffix(".apk").removeSuffix(".zip")

        // Tokenize by standard naming delimiters
        val tokens = clean.split('-', '_', '.', ' ', '+')

        val isDebug = tokens.any { it.equals("debug", ignoreCase = true) } ||
                clean.contains("-debug") || clean.contains("_debug") || clean.contains(".debug")
        val isRelease = tokens.any { it.equals("release", ignoreCase = true) } ||
                clean.contains("-release") || clean.contains("_release") || clean.contains(".release")

        return when {
            isDebug && !isRelease -> BuildVariant.DEBUG
            isRelease && !isDebug -> BuildVariant.RELEASE
            isDebug && isRelease -> {
                // If both appear, pick the one that occurs later in the filename
                if (clean.lastIndexOf("debug") > clean.lastIndexOf("release")) {
                    BuildVariant.DEBUG
                } else {
                    BuildVariant.RELEASE
                }
            }
            else -> null
        }
    }

    /**
     * Resolves the build variant of a release asset from its name and release tag.
     * Defaults to RELEASE for standard release binaries that do not explicitly designate debug.
     */
    fun resolveAssetVariant(assetName: String?, releaseTag: String? = null): BuildVariant {
        val fromAsset = detectVariant(assetName)
        if (fromAsset != null) return fromAsset

        val fromTag = detectVariant(releaseTag)
        if (fromTag != null) return fromTag

        val lowerAsset = assetName?.lowercase().orEmpty()
        val lowerTag = releaseTag?.lowercase().orEmpty()
        if (lowerAsset.contains("debug") || lowerTag.contains("debug") ||
            lowerAsset.contains("-dev") || lowerAsset.contains("_dev") ||
            lowerTag.contains("-dev") || lowerTag.contains("_dev")
        ) {
            return BuildVariant.DEBUG
        }

        return BuildVariant.RELEASE
    }
}
