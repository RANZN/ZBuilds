package com.ranzan.zbuilds.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatHelper {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun formatDate(timestampMs: Long): String {
        if (timestampMs <= 0) return "Unknown"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun cleanVersionString(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Determines a clean display name from an APK or ZIP filename
     */
    fun deriveAppNameFromAsset(fileName: String): String {
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
