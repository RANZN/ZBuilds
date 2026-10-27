package com.ranzan.zbuilds.domain.model

/**
 * Result of validating a GitHub token.
 */
data class GithubTokenStatus(
    val login: String?,
    val remainingRequests: String?,
    val requestLimit: String?
)
