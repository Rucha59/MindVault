package com.mindvault.domain.model

/**
 * Pure logic for a rated review session (no Android, no database).
 * - A card's schedule is set by its FIRST rating in the session; the ViewModel saves only that one.
 * - Again puts the card at the end of the queue. When it comes back it is a "repeat": its answer (Hard, Good or
 *   Easy) is saved through the relearn path and the card leaves the session.
 * - Extra cards ([extraCardIds]) are practice only: they get "Still hard" (back to the end of the queue) or
 *   "Got it" (leaves), and nothing about them is ever rated or saved.
 * - The answer must be revealed before an answer counts.
 */
class ReviewSession private constructor(
    val queue: List<FlashcardDetail>,
    val index: Int,
    val isRevealed: Boolean,
    private val firstRatings: Map<String, ReviewRating>,
    /** Distinct cards in the session, for progress. */
    val totalCards: Int,
    private val extraCardIds: Set<String>,
    private val extraGotIt: Set<String>,
) {
    val current: FlashcardDetail? get() = queue.getOrNull(index)
    val isFinished: Boolean get() = index >= queue.size

    /** True when the current card is extra practice (not scheduled). */
    val isExtra: Boolean get() = current?.let { it.flashcard.id in extraCardIds } ?: false

    /** True when the current scheduled card was already rated earlier in this session. */
    val isRepeat: Boolean get() = !isExtra && (current?.let { it.flashcard.id in firstRatings } ?: false)

    /** Scheduled cards rated so far. */
    val reviewedCount: Int get() = firstRatings.size

    /** Extra-practice cards finished with "Got it". */
    val extraCount: Int get() = extraGotIt.size

    val cardsLeft: Int get() = (queue.size - index).coerceAtLeast(0)
    val progress: Float
        get() = if (totalCards == 0) 0f else (firstRatings.size + extraGotIt.size).toFloat() / totalCards

    /** How many cards got [rating] as their first rating. */
    fun countOf(rating: ReviewRating): Int = firstRatings.values.count { it == rating }

    fun flip(): ReviewSession = if (current == null || isRevealed) this else copy(isRevealed = true)

    /** Answers a scheduled card. Ignored for extra cards (use [answerExtra]). */
    fun answer(rating: ReviewRating): ReviewSession {
        val card = current ?: return this
        if (!isRevealed || isExtra) return this
        val id = card.flashcard.id
        val ratings = if (id in firstRatings) firstRatings else firstRatings + (id to rating)
        val nextQueue = if (rating == ReviewRating.AGAIN) queue + card else queue
        return ReviewSession(nextQueue, index + 1, false, ratings, totalCards, extraCardIds, extraGotIt)
    }

    /** Answers an extra-practice card: "Got it" removes it, "Still hard" puts it back at the end. */
    fun answerExtra(gotIt: Boolean): ReviewSession {
        val card = current ?: return this
        if (!isRevealed || !isExtra) return this
        return ReviewSession(
            queue = if (gotIt) queue else queue + card,
            index = index + 1,
            isRevealed = false,
            firstRatings = firstRatings,
            totalCards = totalCards,
            extraCardIds = extraCardIds,
            extraGotIt = if (gotIt) extraGotIt + card.flashcard.id else extraGotIt,
        )
    }

    private fun copy(isRevealed: Boolean) =
        ReviewSession(queue, index, isRevealed, firstRatings, totalCards, extraCardIds, extraGotIt)

    companion object {
        fun start(cards: List<FlashcardDetail>, extraCardIds: Set<String> = emptySet()) =
            ReviewSession(cards, 0, false, emptyMap(), cards.size, extraCardIds, emptySet())
    }
}