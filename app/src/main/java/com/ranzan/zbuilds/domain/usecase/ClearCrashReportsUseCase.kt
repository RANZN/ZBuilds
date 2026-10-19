package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.AnalyticsTracker

/**
 * UseCase for deleting all cached crash reports from disk.
 */
class ClearCrashReportsUseCase(
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke() {
        analyticsTracker.clearCrashReports()
    }
}
