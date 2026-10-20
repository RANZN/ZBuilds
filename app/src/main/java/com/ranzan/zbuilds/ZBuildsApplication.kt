package com.ranzan.zbuilds

import android.app.Application
import com.ranzan.zbuilds.data.analytics.ZBuildsCrashHandler
import com.ranzan.zbuilds.di.appModules
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

        // Initialize and install global uncaught exception crash handler (100% FOSS & local)
        val crashHandler = get<ZBuildsCrashHandler>()
        crashHandler.install()
    }
}
