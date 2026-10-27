package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.repository.AppRepository

class FetchSourceReleasesUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(sourceUrl: String): Result<SourceRelease> =
        appRepository.fetchSourceReleases(sourceUrl)

    suspend fun fetchReleasePage(sourceUrl: String, page: Int, perPage: Int): Result<List<SourceRelease>> =
        appRepository.fetchReleasePage(sourceUrl, page, perPage)
}
