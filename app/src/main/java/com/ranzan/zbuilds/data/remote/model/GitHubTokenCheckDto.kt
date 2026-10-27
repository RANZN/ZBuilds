package com.ranzan.zbuilds.data.remote.model

/**
 * Result of calling GitHub's /user endpoint with a token, including rate-limit headers.
 */
data class GitHubTokenCheckDto(
    val login: String?,
    val rateLimitRemaining: String?,
    val rateLimit: String?
)
