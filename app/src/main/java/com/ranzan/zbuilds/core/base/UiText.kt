package com.ranzan.zbuilds.core.base

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Text a ViewModel hands to the UI without needing a Context.
 * App text always comes from string resources; [Raw] is only for text from outside the app,
 * such as a system or network error message.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    fun asString(context: Context): String = when (this) {
        is Raw -> value
        is Resource -> context.getString(id, *args.resolve(context))
        is Plural -> context.resources.getQuantityString(id, count, *args.resolve(context))
    }

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
        fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText = Plural(id, count, args.toList())
    }
}

/**
 * Resolves the text in Compose, following configuration (e.g. language) changes.
 */
@Composable
fun UiText.asString(): String {
    LocalConfiguration.current
    return asString(LocalContext.current)
}

private fun List<Any>.resolve(context: Context): Array<Any> =
    map { arg -> if (arg is UiText) arg.asString(context) else arg }.toTypedArray()
