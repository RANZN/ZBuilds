package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.data.local.AppDownloader
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.DownloaderRepository
import kotlinx.coroutines.flow.Flow

class DownloaderRepositoryImpl(
    private val appDownloader: AppDownloader
) : DownloaderRepository {

    override fun observeDownloadProgress(appId: String): Flow<DownloadProgress> {
        return appDownloader.observeProgress(appId)
    }

    override suspend fun download(app: TrackedApp): Result<String> {
        return appDownloader.download(app)
    }

    override suspend fun cancelDownload(appId: String) {
        appDownloader.cancel(appId)
    }
}
