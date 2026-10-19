package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.AppRepository

class CheckAppUpdatesUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): Result<List<TrackedApp>> =
        appRepository.checkForUpdates()

    suspend fun checkSingleApp(app: TrackedApp): Result<TrackedApp> =
        appRepository.checkAppUpdate(app)
}
