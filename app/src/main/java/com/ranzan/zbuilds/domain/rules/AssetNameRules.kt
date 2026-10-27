package com.ranzan.zbuilds.domain.rules

import com.ranzan.zbuilds.domain.model.AppFileType

/**
 * Rules for reading file type, architecture and a display name from a release file name.
 */
object AssetNameRules {

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

    /**
     * Determines a clean display name from an APK or ZIP filename
     */
    fun deriveAppName(fileName: String): String {
        var base = fileName
            .removeSuffix(".apk")
            .removeSuffix(".zip")
            .replace("-release", "", ignoreCase = true)
            .replace("-unsigned", "", ignoreCase = true)
            .replace("-signed", "", ignoreCase = true)
            .replace("_release", "", ignoreCase = true)
            .replace("_unsigned", "", ignoreCase = true)

        // Replace architecture markers
        listOf("arm64-v8a", "armeabi-v7a", "arm64", "armv7", "x86_64", "x86", "all", "universal").forEach {
            base = base.replace("-$it", "", ignoreCase = true)
                .replace("_$it", "", ignoreCase = true)
                .replace(it, "", ignoreCase = true)
        }

        // Clean trailing version numbers (e.g. -v21.20.405)
        base = base.replace(Regex("[-_]v?\\d+(\\.\\d+)+.*$"), "")
        base = base.replace(Regex("[-_]"), " ").trim()

        if (base.isBlank()) {
            base = fileName.removeSuffix(".apk").removeSuffix(".zip")
        }

        return base.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
