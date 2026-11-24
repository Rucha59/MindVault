package com.mindvault.domain.model

/** The external sources Search can query. Words come from Wiktionary only, concepts from Wikipedia only. */
enum class SearchSource { WORDS, CONCEPTS }

/** The filter above the results. [ALL] queries both sources at the same time. */
enum class SearchScope(val sources: Set<SearchSource>) {
    ALL(setOf(SearchSource.WORDS, SearchSource.CONCEPTS)),
    WORDS(setOf(SearchSource.WORDS)),
    CONCEPTS(setOf(SearchSource.CONCEPTS)),
}