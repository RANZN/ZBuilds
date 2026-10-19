package com.ranzan.zbuilds.domain.model

/**
 * Domain model representing a quick source suggestion for discovering builds.
 * Prepared for Firebase Remote Config / remote payload mapping.
 */
data class SourceSuggestion(
    val id: String,
    val label: String,
    val url: String,
    val description: String? = null
)
