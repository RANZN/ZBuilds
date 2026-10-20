package com.ranzan.zbuilds.core.utils

import android.os.Build
import com.ranzan.zbuilds.domain.model.AppFileType

object ArchitectureHelper {

    fun detectFileType(fileName: String): AppFileType? {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".apk") -> AppFileType.APK
            lower.endsWith(".zip") -> AppFileType.ZIP
            else -> null
        }
    }

    fun detectArchitecture(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("arm64-v8a") || lower.contains("arm64") || lower.contains("aarch64") -> "arm64-v8a"
            lower.contains("armeabi-v7a") || lower.contains("armv7") || lower.contains("arm") -> "armeabi-v7a"
            lower.contains("x86_64") || lower.contains("x64") -> "x86_64"
            lower.contains("x86") -> "x86"
            else -> "all"
        }
    }

    fun getDeviceArchitecture(): String {
        val abis = Build.SUPPORTED_ABIS
        if (!abis.isNullOrEmpty()) {
            return abis[0]
        }
        return "arm64-v8a"
    }

    /**
     * Checks if asset architecture is compatible with the device
     */
    fun isArchCompatible(arch: String): Boolean {
        if (arch.equals("all", ignoreCase = true) || arch.equals("universal", ignoreCase = true)) {
            return true
        }
        val supported = Build.SUPPORTED_ABIS ?: emptyArray()
        return supported.any { it.equals(arch, ignoreCase = true) }
    }

    /**
     * Checks if a filename/URL explicitly specifies debug or release build variant.
     * Returns "Debug", "Release", or null if not indicated.
     */
    fun detectBuildTypeFromFileName(fileName: String): String? {
        val lower = fileName.lowercase()
        val hasDebug = Regex("""(^|[^a-z0-9])debug([^a-z0-9]|$)""").containsMatchIn(lower)
        val hasRelease = Regex("""(^|[^a-z0-9])release([^a-z0-9]|$)""").containsMatchIn(lower)
        return when {
            hasDebug -> "Debug"
            hasRelease -> "Release"
            else -> null
        }
    }

    /**
     * Detects build type ("Debug" or "Release") from installed package, local APK file, or asset name/URL.
     * Returns null if data is not available.
     */
    fun detectAppBuildType(
        context: android.content.Context?,
        packageName: String?,
        isInstalled: Boolean,
        localFilePath: String?,
        assetName: String,
        downloadUrl: String = ""
    ): String? {
        // 1. If installed on device, inspect ApplicationInfo.FLAG_DEBUGGABLE
        if (context != null && !packageName.isNullOrBlank() && isInstalled) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val isDebug = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                return if (isDebug) "Debug" else "Release"
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
                        return if (isDebug) "Debug" else "Release"
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback to asset name, local file name, or download URL keywords
        return detectBuildTypeFromFileName(assetName.ifBlank { localFilePath.orEmpty() })
            ?: detectBuildTypeFromFileName(downloadUrl)
    }
}
