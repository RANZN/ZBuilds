package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.model.SourceSuggestion
import com.ranzan.zbuilds.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve quick source suggestions for the UI.
 */
class GetSourceSuggestionsUseCase(
    private val suggestionRepository: SuggestionRepository
) {
    operator fun invoke(): Flow<List<SourceSuggestion>> =
        suggestionRepository.getSourceSuggestions()
}
