package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.domain.model.SourceSuggestion
import com.ranzan.zbuilds.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Concrete repository providing source suggestions.
 * Configured with default suggestions and designed to be backed by Firebase Remote Config.
 *
 * When Firebase Remote Config is connected:
 * 1. Inject FirebaseRemoteConfig or RemoteConfigManager
 * 2. Fetch/activate remote keys (e.g., "source_suggestions_json")
 * 3. Emit parsed JSON items or fallback to defaultSuggestions.
 */
class SuggestionRepositoryImpl : SuggestionRepository {

    private val defaultSuggestions = listOf(
        SourceSuggestion(
            id = "revanced_magisk_module",
            label = "ReVanced Modules",
            url = "https://github.com/j-hc/revanced-magisk-module",
            description = "Pre-built ReVanced Magisk / KernelSU modules"
        ),
        SourceSuggestion(
            id = "microg_re",
            label = "MicroG-RE",
            url = "https://github.com/MorpheApp/MicroG-RE",
            description = "MicroG Revanced Edition APK releases"
        ),
        SourceSuggestion(
            id = "spotube",
            label = "Spotube",
            url = "https://github.com/KRTirtho/Spotube",
            description = "Open-source Spotify client"
        ),
        SourceSuggestion(
            id = "newpipe",
            label = "NewPipe",
            url = "https://github.com/TeamNewPipe/NewPipe",
            description = "Lightweight YouTube frontend"
        )
    )

    override fun getSourceSuggestions(): Flow<List<SourceSuggestion>> {
        // Simple Flow for now; will observe Firebase Remote Config updates when added.
        return flowOf(defaultSuggestions)
    }
}
