package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.CrashReport
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface contract for dispatching analytics events, recording crash telemetry,
 * and managing historical crash reports.
 */
interface AnalyticsTracker {
    fun trackEvent(name: String, parameters: Map<String, String> = emptyMap())
    fun trackCrash(throwable: Throwable, isFatal: Boolean = true, metadata: Map<String, String> = emptyMap())
    fun logBreadcrumb(message: String)
    fun getCrashReports(): Flow<List<CrashReport>>
    suspend fun clearCrashReports()
}
