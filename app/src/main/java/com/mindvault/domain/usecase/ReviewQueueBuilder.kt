package com.mindvault.domain.usecase

import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.ReviewCounts
import com.mindvault.domain.model.ReviewMode
import java.time.Instant
import java.time.ZoneId

/** Pure logic (no Android, no I/O): which cards are due, new or difficult today. Easy to unit test. */
object ReviewQueueBuilder {
    const val DEFAULT_NEW_CARDS_PER_DAY = 20
    val NEW_CARDS_PER_DAY_RANGE = 1..100
    val NEW_CARDS_PER_DAY_OPTIONS = listOf(5, 10, 20, 30, 50)

    /** A reviewed card with an ease factor below this is "difficult". */
    const val DIFFICULT_EASE_BELOW = 2.0

    fun counts(
        cards: List<FlashcardDetail>,
        newCardsPerDay: Int,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ) = ReviewCounts(
        dueReviews = dueReviews(cards, now, zone).size,
        newCards = newCards(cards, newCardsPerDay, now, zone).size,
        difficult = difficult(cards, now, zone).size,
    )

    fun queue(
        mode: ReviewMode,
        cards: List<FlashcardDetail>,
        newCardsPerDay: Int,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<FlashcardDetail> = when (mode) {
        ReviewMode.DAILY -> dueReviews(cards, now, zone) + newCards(cards, newCardsPerDay, now, zone)
        ReviewMode.NEW_CARDS -> newCards(cards, newCardsPerDay, now, zone)
        ReviewMode.DIFFICULT -> difficult(cards, now, zone)
    }

    /**
     * Cards in a session that are only extra practice: in Difficult cards, the ones that are not due today (for
     * example because they were just reviewed). Rating them would move their schedule as if a full gap had passed,
     * so they never change it. Every other mode has none.
     */
    fun extraCardIds(
        mode: ReviewMode,
        cards: List<FlashcardDetail>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Set<String> =
        if (mode == ReviewMode.DIFFICULT) {
            difficult(cards, now, zone).filterNot { isDueToday(it, now, zone) }.map { it.flashcard.id }.toSet()
        } else {
            emptySet()
        }

    private fun isDueToday(card: FlashcardDetail, now: Long, zone: ZoneId): Boolean =
        card.reviewState.isReviewed && card.reviewState.nextReviewAt < startOfDay(now, zone, plusDays = 1)

    /** Reviewed cards due today or overdue, most overdue first. Due is by calendar day. */
    private fun dueReviews(cards: List<FlashcardDetail>, now: Long, zone: ZoneId): List<FlashcardDetail> =
        cards
            .filter { isDueToday(it, now, zone) }
            .sortedWith(
                compareBy<FlashcardDetail>(
                    { it.reviewState.nextReviewAt },
                    { it.flashcard.createdAt },
                    { it.flashcard.id },
                ),
            )

    /**
     * New cards still allowed today. A card was introduced today when it has exactly one review and that review
     * is today (a card's review count rises only on its first rating per session), so no extra storage is needed.
     */
    private fun newCards(
        cards: List<FlashcardDetail>,
        limit: Int,
        now: Long,
        zone: ZoneId,
    ): List<FlashcardDetail> {
        val startOfToday = startOfDay(now, zone, plusDays = 0)
        val introducedToday = cards.count {
            it.reviewState.reviewCount == 1 && (it.reviewState.lastReviewedAt ?: 0L) >= startOfToday
        }
        val remaining = (limit - introducedToday).coerceAtLeast(0)
        return cards
            .filter { !it.reviewState.isReviewed }
            .sortedWith(compareBy<FlashcardDetail>({ it.flashcard.createdAt }, { it.flashcard.id }))
            .take(remaining)
    }

    /** Reviewed cards with a low ease factor: the ones due today first, then the rest, each hardest first. */
    private fun difficult(cards: List<FlashcardDetail>, now: Long, zone: ZoneId): List<FlashcardDetail> =
        cards
            .filter { it.reviewState.isReviewed && it.reviewState.easeFactor < DIFFICULT_EASE_BELOW }
            .sortedWith(
                compareBy<FlashcardDetail>(
                    { if (isDueToday(it, now, zone)) 0 else 1 },
                    { it.reviewState.easeFactor },
                    { it.flashcard.id },
                ),
            )

    private fun startOfDay(now: Long, zone: ZoneId, plusDays: Long): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(plusDays)
            .atStartOfDay(zone).toInstant().toEpochMilli()
}