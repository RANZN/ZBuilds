package com.ranzan.zbuilds.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Display formatting for sizes and dates.
 */
object FormatHelper {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    /**
     * Formats a timestamp as a date, or returns null when it is not set.
     */
    fun formatDate(timestampMs: Long): String? {
        if (timestampMs <= 0) return null
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
