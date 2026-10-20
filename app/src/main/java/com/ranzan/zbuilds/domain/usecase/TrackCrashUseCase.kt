package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.CrashReporter

/**
 * UseCase for recording handled or unhandled exceptions with associated diagnostic metadata.
 */
class TrackCrashUseCase(
    private val crashReporter: CrashReporter
) {
    operator fun invoke(
        throwable: Throwable,
        isFatal: Boolean = true,
        metadata: Map<String, String> = emptyMap()
    ) {
        crashReporter.trackCrash(throwable, isFatal, metadata)
    }
}
