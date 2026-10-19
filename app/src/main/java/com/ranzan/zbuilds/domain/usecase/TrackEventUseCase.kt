package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.AnalyticsTracker

/**
 * UseCase for recording user actions, navigation checkpoints, or feature usage events.
 */
class TrackEventUseCase(
    private val analyticsTracker: AnalyticsTracker
) {
    operator fun invoke(name: String, parameters: Map<String, String> = emptyMap()) {
        analyticsTracker.trackEvent(name, parameters)
    }
}
