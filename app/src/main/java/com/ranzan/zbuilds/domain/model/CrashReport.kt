package com.ranzan.zbuilds.domain.model

/**
 * Encapsulates structured diagnostic data regarding a runtime crash or exception.
 * Pure domain model with no platform framework dependencies.
 */
data class CrashReport(
    val id: String,
    val timestamp: Long,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: String,
    val threadName: String,
    val deviceInfo: String,
    val appVersion: String,
    val isFatal: Boolean,
    val breadcrumbs: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
