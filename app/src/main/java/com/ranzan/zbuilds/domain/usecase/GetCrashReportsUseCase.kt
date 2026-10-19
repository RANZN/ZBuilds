package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for observing recorded crash reports stored locally on the device.
 */
class GetCrashReportsUseCase(
    private val analyticsTracker: AnalyticsTracker
) {
    operator fun invoke(): Flow<List<CrashReport>> {
        return analyticsTracker.getCrashReports()
    }
}
