package com.ranzan.zbuilds.data.analytics

import android.content.Context
import android.os.Build
import android.util.Log
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.CrashReporter
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of CrashReporter capturing structured runtime exception
 * diagnostics strictly on-device (100% FOSS, zero analytics or telemetry).
 */
class CrashReporterImpl(
    private val context: Context,
    private val crashStorage: CrashStorageDataSource
) : CrashReporter {

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

        val deviceInfo = context.getString(
            R.string.crash_device_info,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            Build.SUPPORTED_ABIS.firstOrNull() ?: context.getString(R.string.common_unknown)
        )

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: context.getString(R.string.common_unknown)
        } catch (_: Exception) {
            context.getString(R.string.common_unknown)
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
            breadcrumbs = emptyList(),
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
        Log.i(TAG, "🧹 Cleared all local crash reports.")
    }

    companion object {
        private const val TAG = "ZBuildsCrashReporter"
    }
}
