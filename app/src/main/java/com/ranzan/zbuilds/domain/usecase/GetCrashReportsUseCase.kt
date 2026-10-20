package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.CrashReporter
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for observing recorded crash reports stored locally on the device.
 */
class GetCrashReportsUseCase(
    private val crashReporter: CrashReporter
) {
    operator fun invoke(): Flow<List<CrashReport>> {
        return crashReporter.getCrashReports()
    }
}
