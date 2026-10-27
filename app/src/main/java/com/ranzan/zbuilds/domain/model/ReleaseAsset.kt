package com.ranzan.zbuilds.domain.model

data class ReleaseAsset(
    val name: String,
    val appName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val fileType: AppFileType,
    val arch: String,
    val variant: BuildVariant,
    val releaseTag: String
)
