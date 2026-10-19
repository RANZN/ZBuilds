package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateGithubToken(token: String?)
    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean)
    suspend fun updateStorageFolder(uri: String?, path: String?)
    suspend fun updateAutoClean(enabled: Boolean)
    suspend fun updateCleanupRetentionDays(days: Int)
    suspend fun clearDownloadCache(): Result<Long>
    suspend fun validateGithubToken(token: String): Result<String>
}
