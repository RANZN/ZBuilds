package com.ranzan.zbuilds.data.analytics

import android.util.Log
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker

/**
 * Global UncaughtExceptionHandler that intercepts crashes across all threads,
 * records structured crash diagnostics through AnalyticsTracker, and safely hands
 * off to the platform's default handler.
 */
class ZBuildsCrashHandler(
    private val analyticsTracker: AnalyticsTracker
) : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i("ZBuildsCrashHandler", "🛡️ ZBuildsCrashHandler initialized as global default uncaught exception handler.")
    }

    @Suppress("DEPRECATION")
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            analyticsTracker.trackCrash(
                throwable = throwable,
                isFatal = true,
                metadata = mapOf(
                    "thread_name" to thread.name,
                    "thread_id" to thread.id.toString(),
                    "uncaught" to "true"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
