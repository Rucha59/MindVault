package com.mindvault.domain.usecase

import com.mindvault.domain.model.Flashcard
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewMode
import com.mindvault.domain.model.ReviewState
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewQueueBuilderTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    private val startOfToday = LocalDate.of(2026, 11, 18).atStartOfDay(zone).toInstant().toEpochMilli()
    private val day = 86_400_000L
    private val startOfTomorrow = startOfToday + day

    private fun card(
        id: String,
        reviewCount: Int = 0,
        lastReviewedAt: Long? = null,
        nextReviewAt: Long = now,
        ease: Double = 2.5,
        createdAt: Long = 0L,
    ) = FlashcardDetail(
        flashcard = Flashcard(id = id, knowledgeItemId = "k$id", deckId = "deck", createdAt = createdAt),
        knowledgeItem = KnowledgeItem(
            id = "k$id", title = "t$id", type = KnowledgeType.WORD, definition = "d",
            personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
        ),
        reviewState = ReviewState(
            flashcardId = id, lastReviewedAt = lastReviewedAt, nextReviewAt = nextReviewAt,
            reviewCount = reviewCount, easeFactor = ease, intervalDays = if (reviewCount == 0) 0 else 3,
        ),
    )

    private fun ids(list: List<FlashcardDetail>) = list.map { it.flashcard.id }

    @Test
    fun dueMeansAnyTimeTodayOrOverdue_mostOverdueFirst() {
        val cards = listOf(
            card("today", reviewCount = 2, nextReviewAt = startOfTomorrow - 1),
            card("tomorrow", reviewCount = 2, nextReviewAt = startOfTomorrow),
            card("overdue", reviewCount = 2, nextReviewAt = startOfToday - day),
        )
        val queue = ReviewQueueBuilder.queue(ReviewMode.DAILY, cards, 0, now, zone)
        assertEquals(listOf("overdue", "today"), ids(queue))
        assertEquals(2, ReviewQueueBuilder.counts(cards, 0, now, zone).dueReviews)
    }

    @Test
    fun newCardsStopAtTheDailyLimit_oldestFirst() {
        val cards = (1..5).map { card("n$it", createdAt = it.toLong()) }
        val queue = ReviewQueueBuilder.queue(ReviewMode.NEW_CARDS, cards, 3, now, zone)
        assertEquals(listOf("n1", "n2", "n3"), ids(queue))
        assertEquals(3, ReviewQueueBuilder.counts(cards, 3, now, zone).newCards)
    }

    @Test
    fun cardsIntroducedTodayUseUpTheLimit() {
        val cards = listOf(
            card("intro1", reviewCount = 1, lastReviewedAt = startOfToday + 1, nextReviewAt = startOfTomorrow + day),
            card("intro2", reviewCount = 1, lastReviewedAt = startOfToday + 2, nextReviewAt = startOfTomorrow + day),
            // Not introduced today: first review was yesterday, or it already has a second review.
            card("yesterday", reviewCount = 1, lastReviewedAt = startOfToday - 1, nextReviewAt = startOfTomorrow + day),
            card("second", reviewCount = 2, lastReviewedAt = startOfToday + 3, nextReviewAt = startOfTomorrow + day),
            card("n1", createdAt = 1), card("n2", createdAt = 2), card("n3", createdAt = 3),
        )
        val queue = ReviewQueueBuilder.queue(ReviewMode.NEW_CARDS, cards, 3, now, zone)
        assertEquals(listOf("n1"), ids(queue)) // 3 allowed - 2 introduced today
        assertEquals(emptyList<String>(), ids(ReviewQueueBuilder.queue(ReviewMode.NEW_CARDS, cards, 2, now, zone)))
    }

    @Test
    fun dailyQueueListsDueReviewsBeforeNewCards() {
        val cards = listOf(
            card("new", createdAt = 1),
            card("due", reviewCount = 2, nextReviewAt = startOfToday),
        )
        assertEquals(listOf("due", "new"), ids(ReviewQueueBuilder.queue(ReviewMode.DAILY, cards, 20, now, zone)))
        assertEquals(2, ReviewQueueBuilder.counts(cards, 20, now, zone).dailyTotal)
    }

    @Test
    fun difficultIsReviewedCardsBelowTheEaseThreshold_hardestFirst() {
        val cards = listOf(
            card("a", reviewCount = 3, ease = 1.9),
            card("b", reviewCount = 3, ease = 1.5),
            card("c", reviewCount = 3, ease = 2.0), // not below 2.0
            card("d", reviewCount = 0, ease = 1.3), // never reviewed
        )
        assertEquals(listOf("b", "a"), ids(ReviewQueueBuilder.queue(ReviewMode.DIFFICULT, cards, 20, now, zone)))
        assertEquals(2, ReviewQueueBuilder.counts(cards, 20, now, zone).difficult)
    }

    @Test
    fun newCardsModeIgnoresDueReviews() {
        val cards = listOf(card("due", reviewCount = 2, nextReviewAt = startOfToday), card("new"))
        assertEquals(listOf("new"), ids(ReviewQueueBuilder.queue(ReviewMode.NEW_CARDS, cards, 20, now, zone)))
    }
}