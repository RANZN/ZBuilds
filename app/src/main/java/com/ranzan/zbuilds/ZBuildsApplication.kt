package com.ranzan.zbuilds

import android.app.Application
import com.ranzan.zbuilds.data.analytics.ZBuildsCrashHandler
import com.ranzan.zbuilds.di.appModules
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ZBuildsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ZBuildsApplication)
            modules(appModules)
        }

        // Initialize and install global uncaught exception crash handler
        val crashHandler = get<ZBuildsCrashHandler>()
        crashHandler.install()

        // Dispatch initial lifecycle telemetry event
        val analyticsTracker = get<AnalyticsTracker>()
        analyticsTracker.trackEvent(
            "app_started",
            mapOf("version" to AppSettings.APP_VERSION)
        )
    }
}
