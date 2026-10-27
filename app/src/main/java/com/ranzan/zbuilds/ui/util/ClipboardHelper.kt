package com.ranzan.zbuilds.ui.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    /**
     * Returns the trimmed text currently on the clipboard, or null when there is none.
     */
    fun readText(context: Context): String? = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
