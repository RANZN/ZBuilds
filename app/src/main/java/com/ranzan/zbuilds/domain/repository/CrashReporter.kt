package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.CrashReport
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface contract for recording uncaught crash telemetry
 * and managing historical on-device crash reports (100% FOSS & local).
 */
interface CrashReporter {
    fun trackCrash(throwable: Throwable, isFatal: Boolean = true, metadata: Map<String, String> = emptyMap())
    fun getCrashReports(): Flow<List<CrashReport>>
    suspend fun clearCrashReports()
}
