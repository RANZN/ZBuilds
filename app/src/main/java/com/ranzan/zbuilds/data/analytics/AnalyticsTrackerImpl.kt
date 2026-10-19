package com.ranzan.zbuilds.data.analytics

import android.content.Context
import android.os.Build
import android.util.Log
import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Concrete implementation of AnalyticsTracker providing in-app event tracking,
 * breadcrumb logging, device environment telemetry, and crash logging.
 */
class AnalyticsTrackerImpl(
    private val context: Context,
    private val crashStorage: CrashStorageDataSource
) : AnalyticsTracker {

    private val maxBreadcrumbs = 30
    private val breadcrumbsList: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun trackEvent(name: String, parameters: Map<String, String>) {
        val paramString = if (parameters.isNotEmpty()) " -> $parameters" else ""
        Log.i(TAG, "📊 Event: $name$paramString")
        logBreadcrumb("Event: $name$paramString")
    }

    override fun logBreadcrumb(message: String) {
        val time = dateFormat.format(Date())
        val entry = "[$time] $message"
        breadcrumbsList.add(entry)
        if (breadcrumbsList.size > maxBreadcrumbs) {
            breadcrumbsList.removeAt(0)
        }
    }

    override fun trackCrash(
        throwable: Throwable,
        isFatal: Boolean,
        metadata: Map<String, String>
    ) {
        val timestamp = System.currentTimeMillis()
        val exceptionClass = throwable::class.qualifiedName ?: throwable::class.java.name
        val message = throwable.message
        val stackTrace = Log.getStackTraceString(throwable)
        val threadName = Thread.currentThread().name

        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} " +
                "(Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}, " +
                "ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"})"

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        val report = CrashReport(
            id = UUID.randomUUID().toString().take(8),
            timestamp = timestamp,
            exceptionClass = exceptionClass,
            message = message,
            stackTrace = stackTrace,
            threadName = threadName,
            deviceInfo = deviceInfo,
            appVersion = appVersion,
            isFatal = isFatal,
            breadcrumbs = ArrayList(breadcrumbsList),
            metadata = metadata
        )

        Log.e(TAG, "💥 Crash Captured [${if (isFatal) "FATAL" else "NON-FATAL"}]: $exceptionClass: $message", throwable)
        crashStorage.saveReportSync(report)
    }

    override fun getCrashReports(): Flow<List<CrashReport>> {
        return crashStorage.crashReportsFlow
    }

    override suspend fun clearCrashReports() {
        crashStorage.clearAll()
        breadcrumbsList.clear()
        Log.i(TAG, "🧹 Cleared all crash reports and breadcrumbs.")
    }

    companion object {
        private const val TAG = "ZBuildsAnalytics"
    }
}
