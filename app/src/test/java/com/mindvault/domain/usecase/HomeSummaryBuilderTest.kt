package com.mindvault.domain.usecase

import com.mindvault.domain.model.Flashcard
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.model.VaultEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSummaryBuilderTest {

    private fun item(id: String, createdAt: Long = 0L) = KnowledgeItem(
        id = id, title = "t$id", type = KnowledgeType.WORD, definition = "d",
        personalNotes = "", example = null, categoryId = null,
        createdAt = createdAt, updatedAt = createdAt,
    )

    private fun entry(id: String, category: String? = null, createdAt: Long = 0L) =
        VaultEntry(item(id, createdAt), category)

    private fun card(id: String, lastReviewedAt: Long?) = FlashcardDetail(
        flashcard = Flashcard(id = id, knowledgeItemId = "k$id", deckId = "deck", createdAt = 0L),
        knowledgeItem = item("k$id"),
        reviewState = ReviewState(
            flashcardId = id, lastReviewedAt = lastReviewedAt, nextReviewAt = 0L,
            reviewCount = if (lastReviewedAt == null) 0 else 1,
            easeFactor = ReviewState.DEFAULT_EASE_FACTOR, intervalDays = 0,
        ),
    )

    @Test
    fun emptyVaultHasNoCountsOrRecent() {
        val summary = HomeSummaryBuilder.build(emptyList(), emptyList(), 0)
        assertEquals(0, summary.totalItems)
        assertTrue(summary.categoryCounts.isEmpty())
        assertTrue(summary.recentItems.isEmpty())
        assertFalse(summary.hasStudyStatus)
    }

    @Test
    fun categoryCountsGroupSortAndLabelUncategorized() {
        val entries = listOf(
            entry("1", "Science"), entry("2", "Language"), entry("3", "Science"),
            entry("4", null), entry("5", null), entry("6", null),
        )
        val counts = HomeSummaryBuilder.build(entries, emptyList(), 0).categoryCounts
        assertEquals(
            listOf("Uncategorized" to 3, "Science" to 2, "Language" to 1),
            counts.map { it.name to it.count },
        )
    }

    @Test
    fun recentItemsAreNewestFirstAndLimited() {
        val entries = (1..8).map { entry("$it", createdAt = it.toLong()) }
        val recent = HomeSummaryBuilder.build(entries, emptyList(), 0).recentItems
        assertEquals(HomeSummaryBuilder.RECENT_LIMIT, recent.size)
        assertEquals(listOf("8", "7", "6", "5", "4"), recent.map { it.item.id })
    }

    @Test
    fun completedTodayNeedsEveryCardReviewedSinceStartOfDay() {
        val start = 1_000L
        assertTrue(HomeSummaryBuilder.isCompletedToday(listOf(card("a", 1_000L), card("b", 5_000L)), start))
        assertFalse(HomeSummaryBuilder.isCompletedToday(listOf(card("a", 999L), card("b", 5_000L)), start))
        assertFalse(HomeSummaryBuilder.isCompletedToday(listOf(card("a", null)), start))
        assertFalse(HomeSummaryBuilder.isCompletedToday(emptyList(), start))
    }

    @Test
    fun completedTodayCountAloneShowsStudySection() {
        val summary = HomeSummaryBuilder.build(listOf(entry("1")), emptyList(), 1)
        assertTrue(summary.hasStudyStatus)
    }
}