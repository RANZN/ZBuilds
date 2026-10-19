package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.SettingsRepository

class ValidateGithubTokenUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(token: String): Result<String> {
        return settingsRepository.validateGithubToken(token)
    }
}
