package com.ranzan.zbuilds.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceRelease(
    val tag: String,
    val title: String,
    val publishedAt: String,
    val body: String,
    val assets: List<ReleaseAsset>
)
