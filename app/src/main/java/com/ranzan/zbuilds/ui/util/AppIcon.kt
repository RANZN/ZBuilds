package com.ranzan.zbuilds.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
 * Loads the launcher icon of an installed app for display, like an image loader would.
 * Returns null when the app is not installed or has no icon.
 */
@Composable
fun rememberInstalledAppIcon(packageName: String?): ImageBitmap? {
    val packageManager = LocalContext.current.packageManager
    return remember(packageName) {
        packageName?.takeIf { it.isNotBlank() }?.let { pkg ->
            runCatching { packageManager.getApplicationIcon(pkg).toBitmap().asImageBitmap() }.getOrNull()
        }
    }
}
