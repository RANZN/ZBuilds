package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow

class GetTrackedAppsUseCase(
    private val appRepository: AppRepository
) {
    operator fun invoke(): Flow<List<TrackedApp>> = appRepository.getTrackedApps()
}
