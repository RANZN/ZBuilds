package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.AppRepository

class AddTrackedAppUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(app: TrackedApp) = appRepository.addTrackedApp(app)
}
