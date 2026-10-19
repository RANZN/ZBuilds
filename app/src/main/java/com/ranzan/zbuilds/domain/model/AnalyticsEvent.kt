package com.ranzan.zbuilds.domain.model

/**
 * Represents a discrete user interaction, system event, or telemetry checkpoint.
 */
data class AnalyticsEvent(
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parameters: Map<String, String> = emptyMap()
)
