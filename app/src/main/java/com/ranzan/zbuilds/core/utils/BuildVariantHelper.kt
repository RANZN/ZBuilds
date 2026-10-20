package com.ranzan.zbuilds.core.utils

enum class BuildVariant(val displayName: String) {
    DEBUG("Debug"),
    RELEASE("Release")
}

object BuildVariantHelper {
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
     * Inspects installed package, local APK archive, or asset/filename to detect variant.
     * Returns null if data is not available.
     */
    fun detectAppVariant(
        context: android.content.Context?,
        packageName: String?,
        isInstalled: Boolean,
        localFilePath: String?,
        assetName: String?,
        downloadUrl: String? = null
    ): BuildVariant? {
        // 1. If installed on device, inspect ApplicationInfo.FLAG_DEBUGGABLE
        if (context != null && !packageName.isNullOrBlank() && isInstalled) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val isDebug = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                return if (isDebug) BuildVariant.DEBUG else BuildVariant.RELEASE
            } catch (_: Exception) {
            }
        }

        // 2. If downloaded local APK exists, inspect archive ApplicationInfo.FLAG_DEBUGGABLE
        if (context != null && !localFilePath.isNullOrBlank() && localFilePath.endsWith(".apk", ignoreCase = true)) {
            try {
                val file = java.io.File(localFilePath)
                if (file.exists()) {
                    val archiveInfo = context.packageManager.getPackageArchiveInfo(localFilePath, 0)
                    val flags = archiveInfo?.applicationInfo?.flags
                    if (flags != null) {
                        val isDebug = (flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                        return if (isDebug) BuildVariant.DEBUG else BuildVariant.RELEASE
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback to asset name, local file name, or download URL
        return detectVariant(assetName)
            ?: detectVariant(localFilePath?.let { java.io.File(it).name })
            ?: detectVariant(downloadUrl)
    }
}
