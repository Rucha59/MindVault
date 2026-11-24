package com.mindvault.domain.model

/** Everything a Search request can end in. Never carries invented content. */
sealed interface SearchOutcome {
    data class Success(val results: List<SearchResult>) : SearchOutcome
    data object NoResults : SearchOutcome

    /** The provider could not be reached (no connection, timeout). */
    data object Offline : SearchOutcome

    /** The provider is rate limiting or temporarily overloaded (HTTP 429 / 503). Trying again later can work. */
    data object Busy : SearchOutcome

    /** The provider answered badly or an unexpected error occurred. */
    data object Failure : SearchOutcome
}

sealed interface SaveSearchResult {
    data object Saved : SaveSearchResult
    data class AlreadySaved(val itemId: String) : SaveSearchResult

    /** The result broke a validation rule (for example a field too long) or has no summary yet. */
    data object Invalid : SaveSearchResult
}