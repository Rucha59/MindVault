package com.mindvault.domain.model

/** A category name with the number of saved items in it. Items without a category are "Uncategorized". */
data class CategoryCount(
    val name: String,
    val count: Int,
)

/** A deck with cards to review today: due reviews plus the new cards still allowed by the daily limit. */
data class DeckDue(
    val deck: Deck,
    val cardsToReview: Int,
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
    /** Decks with cards to review today, for "Due for review". */
    val dueDecks: List<DeckDue> = emptyList(),
) {
    /** The flashcard section is shown only when it has something real to say. */
    val hasStudyStatus: Boolean
        get() = inProgressDecks.isNotEmpty() || hasUnstartedDeck || decksCompletedToday > 0
}