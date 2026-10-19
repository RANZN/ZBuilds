package com.ranzan.zbuilds.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ranzan.zbuilds.domain.model.TrackedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.trackedAppsDataStore: DataStore<Preferences> by preferencesDataStore(name = "zbuilds_tracked_apps")

class TrackedAppsDataStore(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val KEY_TRACKED_APPS = stringPreferencesKey("key_tracked_apps_json")

    fun getTrackedApps(): Flow<List<TrackedApp>> {
        return context.trackedAppsDataStore.data.map { preferences ->
            val jsonString = preferences[KEY_TRACKED_APPS]
            if (jsonString.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<TrackedApp>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    suspend fun saveTrackedApps(apps: List<TrackedApp>) {
        context.trackedAppsDataStore.edit { preferences ->
            val jsonString = json.encodeToString(apps)
            preferences[KEY_TRACKED_APPS] = jsonString
        }
    }

    suspend fun addTrackedApp(app: TrackedApp) {
        context.trackedAppsDataStore.edit { preferences ->
            val jsonString = preferences[KEY_TRACKED_APPS]
            val currentList = if (jsonString.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString<List<TrackedApp>>(jsonString).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            }

            // Remove existing app if matching ID or matching (sourceUrl + assetName)
            currentList.removeAll { it.id == app.id || (it.sourceUrl == app.sourceUrl && it.assetName == app.assetName) }
            currentList.add(0, app) // Add to top

            preferences[KEY_TRACKED_APPS] = json.encodeToString(currentList.toList())
        }
    }

    suspend fun updateTrackedApp(app: TrackedApp) {
        context.trackedAppsDataStore.edit { preferences ->
            val jsonString = preferences[KEY_TRACKED_APPS] ?: return@edit
            val currentList = try {
                json.decodeFromString<List<TrackedApp>>(jsonString).toMutableList()
            } catch (e: Exception) {
                return@edit
            }

            val index = currentList.indexOfFirst { it.id == app.id }
            if (index != -1) {
                currentList[index] = app
                preferences[KEY_TRACKED_APPS] = json.encodeToString(currentList.toList())
            }
        }
    }

    suspend fun deleteTrackedApp(id: String) {
        context.trackedAppsDataStore.edit { preferences ->
            val jsonString = preferences[KEY_TRACKED_APPS] ?: return@edit
            val currentList = try {
                json.decodeFromString<List<TrackedApp>>(jsonString).toMutableList()
            } catch (e: Exception) {
                return@edit
            }

            currentList.removeAll { it.id == id }
            preferences[KEY_TRACKED_APPS] = json.encodeToString(currentList.toList())
        }
    }
}
