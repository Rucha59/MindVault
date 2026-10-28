package com.mindvault.domain.model

import kotlin.random.Random

/**
 * Pure study-session logic: card order, flipping, navigation, shuffle and progress. No Android,
 * no database. The ViewModel persists reviews; this class only decides what happens next.
 *
 * - The deck is snapshotted when the session starts, so cards never reshuffle as they get reviewed.
 * - Default order: unreviewed cards first, then oldest first.
 * - A card counts as reviewed the first time its answer is revealed in a run.
 */
class StudySession private constructor(
    private val baseOrder: List<FlashcardDetail>,
    val cards: List<FlashcardDetail>,
    val index: Int,
    val isRevealed: Boolean,
    val isShuffled: Boolean,
    /** Cards revealed in the current pass (reset by [restart]). */
    val reviewedThisRun: Set<String>,
    /** Cards revealed since the snapshot, across passes. Used for deck completion. */
    private val everReviewed: Set<String>,
    val isFinished: Boolean,
) {
    val total: Int get() = cards.size
    val current: FlashcardDetail? get() = if (isFinished) null else cards.getOrNull(index)

    /** 1-based position of the current card. */
    val position: Int get() = index + 1
    val isLastCard: Boolean get() = index == cards.lastIndex
    val hasPrevious: Boolean get() = isFinished || index > 0

    /** Cards in the deck that have been reviewed at least once, including earlier sessions. */
    val reviewedInDeck: Int
        get() = cards.count { it.reviewState.isReviewed || it.flashcard.id in everReviewed }
    val isDeckComplete: Boolean get() = cards.isNotEmpty() && reviewedInDeck == cards.size

    /** Turns the card over. Revealing the answer marks the card as reviewed for this run. */
    fun flip(): StudySession {
        val card = current ?: return this
        return if (isRevealed) {
            copy(isRevealed = false)
        } else {
            val id = card.flashcard.id
            copy(isRevealed = true, reviewedThisRun = reviewedThisRun + id, everReviewed = everReviewed + id)
        }
    }

    /** Moves on; after the last card the session is finished. */
    fun next(): StudySession = when {
        isFinished || cards.isEmpty() -> this
        index < cards.lastIndex -> copy(index = index + 1, isRevealed = false)
        else -> copy(isRevealed = false, isFinished = true)
    }

    /** Goes back one card; from the finished screen it returns to the last card. */
    fun previous(): StudySession = when {
        isFinished -> copy(isFinished = false, isRevealed = false)
        index > 0 -> copy(index = index - 1, isRevealed = false)
        else -> this
    }

    /**
     * Turns shuffle on or off without losing your place: the current card and the ones already
     * passed stay where they are, only the cards still ahead are reordered.
     */
    fun withShuffle(enabled: Boolean, random: Random): StudySession {
        if (enabled == isShuffled || cards.isEmpty()) return this
        if (isFinished) return copy(isShuffled = enabled)
        val fixed = cards.take(index + 1)
        val ahead = cards.drop(index + 1)
        val reordered = if (enabled) {
            ahead.shuffled(random)
        } else {
            val position = baseOrder.withIndex().associate { (i, card) -> card.flashcard.id to i }
            ahead.sortedBy { position[it.flashcard.id] ?: Int.MAX_VALUE }
        }
        return copy(cards = fixed + reordered, isShuffled = enabled)
    }

    /** Starts another pass from the first card. Saved progress is not erased. */
    fun restart(shuffle: Boolean, random: Random): StudySession = StudySession(
        baseOrder = baseOrder,
        cards = if (shuffle) baseOrder.shuffled(random) else baseOrder,
        index = 0,
        isRevealed = false,
        isShuffled = shuffle,
        reviewedThisRun = emptySet(),
        everReviewed = everReviewed,
        isFinished = false,
    )

    private fun copy(
        cards: List<FlashcardDetail> = this.cards,
        index: Int = this.index,
        isRevealed: Boolean = this.isRevealed,
        isShuffled: Boolean = this.isShuffled,
        reviewedThisRun: Set<String> = this.reviewedThisRun,
        everReviewed: Set<String> = this.everReviewed,
        isFinished: Boolean = this.isFinished,
    ) = StudySession(baseOrder, cards, index, isRevealed, isShuffled, reviewedThisRun, everReviewed, isFinished)

    companion object {
        fun start(cards: List<FlashcardDetail>, shuffle: Boolean, random: Random): StudySession {
            val base = cards.sortedWith(
                compareBy<FlashcardDetail> { it.reviewState.isReviewed }
                    .thenBy { it.flashcard.createdAt }
                    .thenBy { it.flashcard.id },
            )
            return StudySession(
                baseOrder = base,
                cards = if (shuffle) base.shuffled(random) else base,
                index = 0,
                isRevealed = false,
                isShuffled = shuffle,
                reviewedThisRun = emptySet(),
                everReviewed = emptySet(),
                isFinished = false,
            )
        }
    }
}