package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.OlderReleasesPage
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.repository.AppRepository

class FetchSourceReleasesUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(sourceUrl: String): Result<SourceRelease> =
        appRepository.fetchSourceReleases(sourceUrl)

    suspend fun fetchOlderReleases(
        sourceUrl: String,
        listedReleases: List<SourceRelease>,
        afterPage: Int
    ): Result<OlderReleasesPage> =
        appRepository.fetchOlderReleases(sourceUrl, listedReleases, afterPage)
}
