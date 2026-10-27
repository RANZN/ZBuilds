package com.ranzan.zbuilds.domain.model

/**
 * Result of loading older releases for the Add App Source screen.
 * [releases] only contain files that were not already listed.
 */
data class OlderReleasesPage(
    val releases: List<SourceRelease>,
    val lastLoadedPage: Int,
    val pagesChecked: Int,
    val hasMore: Boolean
)
