package com.ranzan.zbuilds.data.repository

import android.content.Context
import android.os.Environment
import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class SettingsRepositoryImpl(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val gitHubApiService: GitHubApiService
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = settingsDataStore.settingsFlow

    override suspend fun updateGithubToken(token: String?) {
        settingsDataStore.updateGithubToken(token)
    }

    override suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) {
        settingsDataStore.updateBuildFormats(showApks, showModules)
    }

    override suspend fun updateStorageFolder(uri: String?, path: String?) {
        settingsDataStore.updateStorageFolder(uri, path)
    }

    override suspend fun updateAutoClean(enabled: Boolean) {
        settingsDataStore.updateAutoClean(enabled)
    }

    override suspend fun updateCleanupRetentionDays(days: Int) {
        settingsDataStore.updateCleanupRetentionDays(days)
    }

    override suspend fun clearDownloadCache(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L

            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.endsWith(".apk") || file.name.endsWith(".zip") || file.name.endsWith(".tmp"))) {
                        freedBytes += file.length()
                        file.delete()
                    }
                }
            }

            val internalFilesDir = context.filesDir
            internalFilesDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".apk") || file.name.endsWith(".zip") || file.name.endsWith(".tmp"))) {
                    freedBytes += file.length()
                    file.delete()
                }
            }

            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        freedBytes += file.length()
                        file.delete()
                    }
                }
            }

            Result.success(freedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateGithubToken(token: String): Result<String> {
        return gitHubApiService.validateToken(token)
    }
}
