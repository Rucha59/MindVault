package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.limited
import com.mindvault.domain.repository.SearchRepository
import javax.inject.Inject

class SearchDefinitionsUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(rawQuery: String): SearchOutcome {
        val query = rawQuery.trim().replace(WHITESPACE, " ")
        if (query.isEmpty() || query.length > SearchLimits.MAX_QUERY_LENGTH) return SearchOutcome.NoResults
        return when (val outcome = repository.search(query)) {
            is SearchOutcome.Success -> outcome.copy(results = outcome.results.map { it.limited() })
            else -> outcome
        }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}