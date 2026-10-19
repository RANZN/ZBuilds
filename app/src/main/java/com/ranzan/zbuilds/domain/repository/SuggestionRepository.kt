package com.ranzan.zbuilds.domain.repository

import com.ranzan.zbuilds.domain.model.SourceSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for retrieving source suggestions.
 * Implementations can source this from local defaults, Firebase Remote Config, or remote REST APIs.
 */
interface SuggestionRepository {
    fun getSourceSuggestions(): Flow<List<SourceSuggestion>>
}
