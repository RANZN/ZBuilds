package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.DownloaderRepository
import kotlinx.coroutines.flow.Flow

class DownloadAppUseCase(
    private val downloaderRepository: DownloaderRepository
) {
    suspend fun startDownload(app: TrackedApp): Result<String> =
        downloaderRepository.download(app)

    fun observeProgress(appId: String): Flow<DownloadProgress> =
        downloaderRepository.observeDownloadProgress(appId)

    suspend fun cancelDownload(appId: String) =
        downloaderRepository.cancelDownload(appId)
}
