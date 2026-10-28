package com.mindvault.domain.model

/** A deck with its card counts. Reviewed = total minus unreviewed. */
data class DeckSummary(
    val deck: Deck,
    val totalCards: Int,
    val unreviewedCards: Int,
) {
    val reviewedCards: Int get() = (totalCards - unreviewedCards).coerceAtLeast(0)

    /** Complete when the deck has cards and every card has been reviewed at least once. */
    val isComplete: Boolean get() = totalCards > 0 && unreviewedCards == 0

    val progress: Float get() = if (totalCards == 0) 0f else reviewedCards.toFloat() / totalCards
}

/** A deck, its summary and its cards (each card references its Vault item, never a copy). */
data class DeckDetail(
    val summary: DeckSummary,
    val cards: List<FlashcardDetail>,
) {
    val deck: Deck get() = summary.deck
}

/** A deck as an option when adding an item: [containsItem] is true when the item already has a card there. */
data class DeckOption(
    val deck: Deck,
    val containsItem: Boolean,
)