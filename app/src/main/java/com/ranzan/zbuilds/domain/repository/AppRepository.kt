package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.TrackedApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getTrackedApps(): Flow<List<TrackedApp>>
    suspend fun addTrackedApp(app: TrackedApp)
    suspend fun updateTrackedApp(app: TrackedApp)
    suspend fun deleteTrackedApp(id: String)
    suspend fun fetchSourceReleases(sourceUrl: String): Result<SourceRelease>
    suspend fun fetchReleasePage(sourceUrl: String, page: Int, perPage: Int): Result<List<SourceRelease>>
    suspend fun checkForUpdates(): Result<List<TrackedApp>>
    suspend fun checkAppUpdate(app: TrackedApp): Result<TrackedApp>
}
