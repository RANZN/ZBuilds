package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.repository.AppRepository

class FetchSourceReleasesUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(sourceUrl: String): Result<SourceRelease> =
        appRepository.fetchSourceReleases(sourceUrl)
}
