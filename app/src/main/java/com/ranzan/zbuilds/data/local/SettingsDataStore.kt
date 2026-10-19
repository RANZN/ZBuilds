package com.ranzan.zbuilds.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ranzan.zbuilds.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "zbuilds_settings")

class SettingsDataStore(
    private val context: Context
) {
    private object PreferencesKeys {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val SHOW_APKS = booleanPreferencesKey("show_apks")
        val SHOW_MODULES = booleanPreferencesKey("show_modules")
        val DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        val DOWNLOAD_FOLDER_PATH = stringPreferencesKey("download_folder_path")
        val AUTO_CLEAN = booleanPreferencesKey("auto_clean_old_versions")
        val CLEANUP_RETENTION_DAYS = intPreferencesKey("cleanup_retention_days")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                githubToken = preferences[PreferencesKeys.GITHUB_TOKEN],
                showApks = preferences[PreferencesKeys.SHOW_APKS] ?: true,
                showModules = preferences[PreferencesKeys.SHOW_MODULES] ?: true,
                downloadFolderUri = preferences[PreferencesKeys.DOWNLOAD_FOLDER_URI],
                downloadFolderPath = preferences[PreferencesKeys.DOWNLOAD_FOLDER_PATH],
                autoCleanOldVersions = preferences[PreferencesKeys.AUTO_CLEAN] ?: true,
                cleanupRetentionDays = preferences[PreferencesKeys.CLEANUP_RETENTION_DAYS] ?: 7
            )
        }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun updateGithubToken(token: String?) {
        context.settingsDataStore.edit { preferences ->
            if (token.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.GITHUB_TOKEN)
            } else {
                preferences[PreferencesKeys.GITHUB_TOKEN] = token.trim()
            }
        }
    }

    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_APKS] = showApks
            preferences[PreferencesKeys.SHOW_MODULES] = showModules
        }
    }

    suspend fun updateStorageFolder(uri: String?, path: String?) {
        context.settingsDataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.DOWNLOAD_FOLDER_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.DOWNLOAD_FOLDER_URI)
            }

            if (path != null) {
                preferences[PreferencesKeys.DOWNLOAD_FOLDER_PATH] = path
            } else {
                preferences.remove(PreferencesKeys.DOWNLOAD_FOLDER_PATH)
            }
        }
    }

    suspend fun updateAutoClean(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLEAN] = enabled
        }
    }

    suspend fun updateCleanupRetentionDays(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEANUP_RETENTION_DAYS] = days
        }
    }
}
