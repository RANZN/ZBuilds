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
}
