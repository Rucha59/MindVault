package com.mindvault.domain.usecase

import com.mindvault.domain.model.CategoryCount
import com.mindvault.domain.model.DeckSummary
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.HomeSummary
import com.mindvault.domain.model.VaultEntry

/** Pure Home dashboard logic (no Android, no I/O), so it is easy to unit test. */
object HomeSummaryBuilder {
    const val UNCATEGORIZED = "Uncategorized"
    const val RECENT_LIMIT = 5
    const val IN_PROGRESS_LIMIT = 2

    fun build(
        entries: List<VaultEntry>,
        decks: List<DeckSummary>,
        decksCompletedToday: Int,
    ): HomeSummary {
        val categoryCounts = entries
            .groupingBy { it.categoryName ?: UNCATEGORIZED }
            .eachCount()
            .map { (name, count) -> CategoryCount(name, count) }
            .sortedWith(compareByDescending<CategoryCount> { it.count }.thenBy { it.name.lowercase() })

        return HomeSummary(
            totalItems = entries.size,
            categoryCounts = categoryCounts,
            recentItems = entries.sortedByDescending { it.item.createdAt }.take(RECENT_LIMIT),
            inProgressDecks = decks
                .filter { it.reviewedCards > 0 && !it.isComplete }
                .take(IN_PROGRESS_LIMIT),
            hasUnstartedDeck = decks.any { it.totalCards > 0 && it.reviewedCards == 0 },
            decksCompletedToday = decksCompletedToday,
        )
    }

    /**
     * Approximation (no completion timestamp is stored): a deck counts as completed today when it
     * has cards and every card's last review is at or after [startOfDayMillis].
     * Known limit: a finished deck that is fully re-studied on a later day counts again.
     */
    fun isCompletedToday(cards: List<FlashcardDetail>, startOfDayMillis: Long): Boolean =
        cards.isNotEmpty() && cards.all { card ->
            val last = card.reviewState.lastReviewedAt
            last != null && last >= startOfDayMillis
        }
}