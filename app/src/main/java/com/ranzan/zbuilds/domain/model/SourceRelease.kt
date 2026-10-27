package com.ranzan.zbuilds.domain.model

data class SourceRelease(
    val tag: String,
    val title: String,
    val publishedAt: String,
    val body: String,
    val assets: List<ReleaseAsset>
)
