package com.ranzan.zbuilds.domain.model

sealed interface DownloadProgress {
    val appId: String

    data class Idle(override val appId: String) : DownloadProgress

    data class Downloading(
        override val appId: String,
        val progressPercent: Float, // 0.0f to 1.0f
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadProgress

    data class Completed(
        override val appId: String,
        val filePath: String,
        val fileType: AppFileType
    ) : DownloadProgress

    data class Failed(
        override val appId: String,
        val error: Throwable
    ) : DownloadProgress
}
