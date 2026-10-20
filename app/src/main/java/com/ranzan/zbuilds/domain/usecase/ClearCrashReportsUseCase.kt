package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.CrashReporter

/**
 * UseCase for deleting all cached crash reports from disk.
 */
class ClearCrashReportsUseCase(
    private val crashReporter: CrashReporter
) {
    suspend operator fun invoke() {
        crashReporter.clearCrashReports()
    }
}
