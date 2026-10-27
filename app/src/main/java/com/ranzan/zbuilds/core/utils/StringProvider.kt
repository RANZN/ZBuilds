package com.ranzan.zbuilds.core.utils

import android.content.Context
import androidx.annotation.StringRes

/**
 * Reads text from string resources for classes that should not hold a Context,
 * and lets unit tests supply their own text.
 */
interface StringProvider {
    fun getString(@StringRes id: Int, vararg args: Any): String
}

class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun getString(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
}
