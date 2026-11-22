package com.mindvault.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    private fun card(id: String) = FlashcardDetail(
        flashcard = Flashcard(id = id, knowledgeItemId = "k$id", deckId = "deck", createdAt = 0L),
        knowledgeItem = KnowledgeItem(
            id = "k$id", title = "t$id", type = KnowledgeType.WORD, definition = "d",
            personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
        ),
        reviewState = ReviewState.initial(id, 0L),
    )

    @Test
    fun emptySessionIsFinished() {
        assertTrue(ReviewSession.start(emptyList()).isFinished)
    }

    @Test
    fun ratingBeforeRevealDoesNothing() {
        val session = ReviewSession.start(listOf(card("a")))
        val after = session.answer(ReviewRating.GOOD)
        assertEquals(0, after.index)
        assertEquals(0, after.reviewedCount)
    }

    @Test
    fun againSendsTheCardToTheEndAndItComesBackAsARepeat() {
        var s = ReviewSession.start(listOf(card("a"), card("b")))
        s = s.flip().answer(ReviewRating.AGAIN)
        assertEquals("b", s.current?.flashcard?.id)
        assertFalse(s.isRepeat)
        s = s.flip().answer(ReviewRating.GOOD)
        assertEquals("a", s.current?.flashcard?.id)
        assertTrue(s.isRepeat)
        s = s.flip().answer(ReviewRating.GOOD) // "Got it"
        assertTrue(s.isFinished)
        assertEquals(2, s.reviewedCount)
        assertEquals(1, s.countOf(ReviewRating.AGAIN))
        assertEquals(1, s.countOf(ReviewRating.GOOD))
    }

    @Test
    fun aRepeatNeverReplacesTheFirstRating() {
        var s = ReviewSession.start(listOf(card("a")))
        s = s.flip().answer(ReviewRating.AGAIN)
        s = s.flip().answer(ReviewRating.EASY)
        assertTrue(s.isFinished)
        assertEquals(1, s.countOf(ReviewRating.AGAIN))
        assertEquals(0, s.countOf(ReviewRating.EASY))
    }

    @Test
    fun againOnARepeatLoopsAgain() {
        var s = ReviewSession.start(listOf(card("a")))
        s = s.flip().answer(ReviewRating.AGAIN)
        s = s.flip().answer(ReviewRating.AGAIN)
        assertFalse(s.isFinished)
        assertTrue(s.isRepeat)
        assertEquals(1, s.reviewedCount)
    }
}