package com.mindvault.feature.search

import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchSaveContent
import com.mindvault.domain.model.SearchScope
import com.mindvault.domain.model.SearchSource

/**
 * Idle: nothing searched yet. Offline / Error: every searched source failed (a single full-screen message).
 * When only some sources fail, the status is Results or NoResults and the failures are shown as [SourceNotice]s.
 */
enum class SearchStatus { Idle, Loading, Results, NoResults, Offline, Error }

enum class SourceFailureReason { OFFLINE, BUSY, ERROR }

/** A source that failed while another one answered; offers a Retry for that source only. */
data class SourceNotice(
    val source: SearchSource,
    val reason: SourceFailureReason,
    val isRetrying: Boolean,
)

/** A result plus the ID of the saved item with the same title and type, if there is one, and its card state. */
data class SearchResultUi(
    val result: SearchResult,
    val savedItemId: String?,
    val isExpanded: Boolean = false,
    val isLoadingSummary: Boolean = false,
    val summaryFailed: Boolean = false,
)

data class SearchUiState(
    val status: SearchStatus = SearchStatus.Idle,
    val scope: SearchScope = SearchScope.ALL,
    /** The query the shown results belong to (the text field may have been edited since). */
    val submittedQuery: String = "",
    val results: List<SearchResultUi> = emptyList(),
    val notices: List<SourceNotice> = emptyList(),
    /** Autocomplete suggestions for what is typed; empty when off, too short, or after a search. */
    val suggestions: List<String> = emptyList(),
    /** The last few searches, newest first; shown while the field is focused and empty. */
    val recentSearches: List<String> = emptyList(),
    /** Keys (type:title) currently being saved, so a second tap can't double-save. */
    val savingKeys: Set<String> = emptySet(),
    val saveError: String? = null,
)

/** One-off events for the screen. */
sealed interface SearchEvent {
    /** The result (with its summary loaded) is ready to open in the Add form. */
    data class OpenEditForm(val content: SearchSaveContent) : SearchEvent
}