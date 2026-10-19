package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()
}
