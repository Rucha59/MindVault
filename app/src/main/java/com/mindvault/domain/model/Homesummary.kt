package com.mindvault.domain.model

/** A category name with the number of saved items in it. Items without a category are "Uncategorized". */
data class CategoryCount(
    val name: String,
    val count: Int,
)

/** Everything the Home dashboard shows, derived from saved items and decks. No separate copies. */
data class HomeSummary(
    val totalItems: Int,
    val categoryCounts: List<CategoryCount>,
    val recentItems: List<VaultEntry>,
    /** Started but not complete (at most two), for "Continue studying". */
    val inProgressDecks: List<DeckSummary>,
    /** True when some deck has cards but none reviewed yet. */
    val hasUnstartedDeck: Boolean,
    val decksCompletedToday: Int,
) {
    /** The flashcard section is shown only when it has something real to say. */
    val hasStudyStatus: Boolean
        get() = inProgressDecks.isNotEmpty() || hasUnstartedDeck || decksCompletedToday > 0
}