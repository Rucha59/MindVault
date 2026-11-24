package com.mindvault.domain.usecase

import com.mindvault.domain.model.SearchOutcome
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.repository.SearchRepository
import javax.inject.Inject

/** Loads the full summary of a concept card that was shown with only its title and description. */
class LoadConceptSummaryUseCase @Inject constructor(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(result: SearchResult): SearchOutcome =
        if (result.needsSummary) repository.loadSummary(result) else SearchOutcome.Success(listOf(result))
}