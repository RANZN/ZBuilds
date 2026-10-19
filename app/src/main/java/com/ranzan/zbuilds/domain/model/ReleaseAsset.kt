package com.ranzan.zbuilds.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val fileType: AppFileType,
    val arch: String,
    val releaseTag: String
)
