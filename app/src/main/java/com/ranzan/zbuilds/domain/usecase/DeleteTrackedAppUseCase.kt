package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.AppRepository

class DeleteTrackedAppUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(id: String) = appRepository.deleteTrackedApp(id)
}
