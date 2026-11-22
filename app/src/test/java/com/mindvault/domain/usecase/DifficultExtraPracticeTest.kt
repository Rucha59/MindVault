package com.mindvault.domain.usecase

import com.mindvault.domain.model.Flashcard
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewMode
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewSession
import com.mindvault.domain.model.ReviewState
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultExtraPracticeTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    private val startOfToday = LocalDate.of(2026, 11, 18).atStartOfDay(zone).toInstant().toEpochMilli()
    private val startOfTomorrow = startOfToday + 86_400_000L

    private fun card(id: String, reviewCount: Int = 3, nextReviewAt: Long, ease: Double) = FlashcardDetail(
        flashcard = Flashcard(id = id, knowledgeItemId = "k$id", deckId = "deck", createdAt = 0L),
        knowledgeItem = KnowledgeItem(
            id = "k$id", title = "t$id", type = KnowledgeType.WORD, definition = "d",
            personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
        ),
        reviewState = ReviewState(
            flashcardId = id, lastReviewedAt = now, nextReviewAt = nextReviewAt, reviewCount = reviewCount,
            easeFactor = ease, intervalDays = 1,
        ),
    )

    private fun ids(list: List<FlashcardDetail>) = list.map { it.flashcard.id }

    @Test
    fun difficultCardsThatAreNotDueAreExtraPractice() {
        val cards = listOf(
            card("due", nextReviewAt = startOfToday, ease = 1.8),
            card("extra", nextReviewAt = startOfTomorrow, ease = 1.4),
            card("easy", nextReviewAt = startOfTomorrow, ease = 2.5), // not difficult at all
        )
        assertEquals(setOf("extra"), ReviewQueueBuilder.extraCardIds(ReviewMode.DIFFICULT, cards, now, zone))
    }

    @Test
    fun otherModesHaveNoExtraCards() {
        val cards = listOf(card("extra", nextReviewAt = startOfTomorrow, ease = 1.4))
        assertTrue(ReviewQueueBuilder.extraCardIds(ReviewMode.DAILY, cards, now, zone).isEmpty())
        assertTrue(ReviewQueueBuilder.extraCardIds(ReviewMode.NEW_CARDS, cards, now, zone).isEmpty())
    }

    @Test
    fun difficultQueueListsDueCardsFirstThenExtraOnes_hardestFirstInEachGroup() {
        val cards = listOf(
            card("extraHard", nextReviewAt = startOfTomorrow, ease = 1.3),
            card("dueEasier", nextReviewAt = startOfToday, ease = 1.9),
            card("dueHarder", nextReviewAt = startOfToday - 1, ease = 1.5),
            card("extraEasier", nextReviewAt = startOfTomorrow, ease = 1.8),
        )
        val queue = ReviewQueueBuilder.queue(ReviewMode.DIFFICULT, cards, 20, now, zone)
        assertEquals(listOf("dueHarder", "dueEasier", "extraHard", "extraEasier"), ids(queue))
    }

    @Test
    fun anExtraCardIsNeverRated() {
        val x = card("x", nextReviewAt = startOfTomorrow, ease = 1.4)
        val s = ReviewSession.start(listOf(x), setOf("x")).flip()
        assertTrue(s.isExtra)
        assertFalse(s.isRepeat)
        val after = s.answer(ReviewRating.GOOD)
        assertEquals(0, after.index)
        assertEquals(0, after.reviewedCount)
    }

    @Test
    fun stillHardLoopsTheCardAndGotItFinishesIt() {
        val x = card("x", nextReviewAt = startOfTomorrow, ease = 1.4)
        var s = ReviewSession.start(listOf(x), setOf("x"))
        s = s.flip().answerExtra(gotIt = false)
        assertFalse(s.isFinished)
        assertEquals("x", s.current?.flashcard?.id)
        assertFalse(s.isRevealed)
        assertEquals(0, s.extraCount)

        s = s.flip().answerExtra(gotIt = true)
        assertTrue(s.isFinished)
        assertEquals(1, s.extraCount)
        assertEquals(0, s.reviewedCount)
        assertEquals(1f, s.progress, 0f)
    }

    @Test
    fun scheduledAndExtraCardsCanShareASession() {
        val due = card("due", nextReviewAt = startOfToday, ease = 1.8)
        val extra = card("extra", nextReviewAt = startOfTomorrow, ease = 1.4)
        var s = ReviewSession.start(listOf(due, extra), setOf("extra"))

        assertFalse(s.isExtra)
        s = s.flip().answer(ReviewRating.AGAIN) // due card marked Again: loops as a repeat
        assertTrue(s.isExtra)                   // next card is the extra one
        s = s.flip().answerExtra(gotIt = true)
        assertTrue(s.isRepeat)                  // the due card is back, as a repeat (not extra)
        assertFalse(s.isExtra)
        s = s.flip().answer(ReviewRating.GOOD)
        assertTrue(s.isFinished)
        assertEquals(1, s.reviewedCount)
        assertEquals(1, s.extraCount)
    }
}