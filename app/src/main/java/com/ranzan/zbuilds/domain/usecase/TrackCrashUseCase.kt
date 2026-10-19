package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.AnalyticsTracker

/**
 * UseCase for recording handled or unhandled exceptions with associated diagnostic metadata.
 */
class TrackCrashUseCase(
    private val analyticsTracker: AnalyticsTracker
) {
    operator fun invoke(
        throwable: Throwable,
        isFatal: Boolean = true,
        metadata: Map<String, String> = emptyMap()
    ) {
        analyticsTracker.trackCrash(throwable, isFatal, metadata)
    }
}
