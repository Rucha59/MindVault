package com.mindvault.feature.search

import com.mindvault.domain.model.SearchResult

enum class SearchStatus { Idle, Loading, Results, NoResults, Offline, Error }

/** A result plus the ID of the saved item with the same title, if there is one. */
data class SearchResultUi(
    val result: SearchResult,
    val savedItemId: String?,
)

data class SearchUiState(
    val status: SearchStatus = SearchStatus.Idle,
    /** The query the shown results belong to (the text field may have been edited since). */
    val submittedQuery: String = "",
    val results: List<SearchResultUi> = emptyList(),
    /** Title keys currently being saved, so a second tap can't double-save. */
    val savingKeys: Set<String> = emptySet(),
    val saveError: String? = null,
)