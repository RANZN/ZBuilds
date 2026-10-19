package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.SettingsRepository

class ClearDownloadCacheUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Long> {
        return settingsRepository.clearDownloadCache()
    }
}
