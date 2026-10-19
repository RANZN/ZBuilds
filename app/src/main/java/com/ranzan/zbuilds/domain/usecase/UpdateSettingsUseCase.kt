package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.SettingsRepository

class UpdateSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend fun updateGithubToken(token: String?) {
        settingsRepository.updateGithubToken(token)
    }

    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) {
        settingsRepository.updateBuildFormats(showApks, showModules)
    }

    suspend fun updateStorageFolder(uri: String?, path: String?) {
        settingsRepository.updateStorageFolder(uri, path)
    }

    suspend fun updateAutoClean(enabled: Boolean) {
        settingsRepository.updateAutoClean(enabled)
    }

    suspend fun updateCleanupRetentionDays(days: Int) {
        settingsRepository.updateCleanupRetentionDays(days)
    }
}
