package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import kotlinx.coroutines.flow.Flow

interface DownloaderRepository {
    fun observeDownloadProgress(appId: String): Flow<DownloadProgress>
    suspend fun download(app: TrackedApp): Result<String>
    suspend fun cancelDownload(appId: String)
}
