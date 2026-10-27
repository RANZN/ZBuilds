package com.ranzan.zbuilds.data.repository

import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.utils.StringProvider
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
class SuggestionRepositoryImpl(
    private val strings: StringProvider
) : SuggestionRepository {

    // Names and descriptions come from string resources (read on each call so they follow
    // the current language); ids and URLs are fixed
    private fun defaultSuggestions() = listOf(
        SourceSuggestion(
            id = "revanced_magisk_module",
            label = strings.getString(R.string.suggestion_revanced_label),
            url = "https://github.com/j-hc/revanced-magisk-module",
            description = strings.getString(R.string.suggestion_revanced_desc)
        ),
        SourceSuggestion(
            id = "microg_re",
            label = strings.getString(R.string.suggestion_microg_label),
            url = "https://github.com/MorpheApp/MicroG-RE",
            description = strings.getString(R.string.suggestion_microg_desc)
        ),
        SourceSuggestion(
            id = "spotube",
            label = strings.getString(R.string.suggestion_spotube_label),
            url = "https://github.com/KRTirtho/Spotube",
            description = strings.getString(R.string.suggestion_spotube_desc)
        ),
        SourceSuggestion(
            id = "newpipe",
            label = strings.getString(R.string.suggestion_newpipe_label),
            url = "https://github.com/TeamNewPipe/NewPipe",
            description = strings.getString(R.string.suggestion_newpipe_desc)
        )
    )

    override fun getSourceSuggestions(): Flow<List<SourceSuggestion>> {
        // Simple Flow for now; will observe Firebase Remote Config updates when added.
        return flowOf(defaultSuggestions())
    }
}
