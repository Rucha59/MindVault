package com.mindvault.domain.usecase

import com.mindvault.domain.model.CategoryCount
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.StatsRaw
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsBuilderTest {

    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 11, 26, 15, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    private val today = LocalDate.of(2026, 11, 26)
    private fun startOf(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** A review at noon, [daysAgo] days before today. */
    private fun reviewAt(daysAgo: Long) = startOf(today.minusDays(daysAgo)) + 12 * 3_600_000L

    private fun raw(
        times: List<Long> = emptyList(),
        ratings: Map<ReviewRating, Int> = emptyMap(),
        types: Map<KnowledgeType, Int> = emptyMap(),
        categories: List<CategoryCount> = emptyList(),
        uncategorized: Int = 0,
    ) = StatsRaw(
        totalItems = 0, addedRecently = 0, uncategorizedItems = uncategorized, categoryCount = categories.size,
        deckCount = 0, flashcardCount = 0, newCards = 0, dueReviews = 0, difficult = 0, learning = 0, mature = 0,
        typeCounts = types, topCategories = categories, ratingCounts = ratings, reviewTimes = times,
    )

    private fun build(raw: StatsRaw) = StatsBuilder.build(raw, now, zone)

    // ---- streak

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val stats = build(raw(times = listOf(reviewAt(0), reviewAt(1), reviewAt(2), reviewAt(4))))
        assertEquals(3, stats.streakDays) // day 3 is missing, so day 4 does not count
    }

    @Test
    fun aDayWithNoReviewYetDoesNotBreakTheStreak() {
        val stats = build(raw(times = listOf(reviewAt(1), reviewAt(2))))
        assertEquals(2, stats.streakDays)
        assertEquals(0, stats.reviewedToday)
    }

    @Test
    fun aMissedDayBreaksTheStreak() {
        val stats = build(raw(times = listOf(reviewAt(2), reviewAt(3))))
        assertEquals(0, stats.streakDays)
    }

    @Test
    fun noReviewsMeansNoStreakAndNoHistory() {
        val stats = build(raw())
        assertEquals(0, stats.streakDays)
        assertFalse(stats.hasReviewHistory)
    }

    @Test
    fun manyReviewsOnOneDayCountAsOneStreakDay() {
        val stats = build(raw(times = List(10) { reviewAt(0) + it } + reviewAt(1)))
        assertEquals(2, stats.streakDays)
        assertEquals(10, stats.reviewedToday)
    }

    @Test
    fun daysAreCalendarDaysInTheGivenZone() {
        // 23:30 UTC the day before is already "today" in UTC+2.
        val plus2 = ZoneOffset.ofHours(2)
        val late = ZonedDateTime.of(2026, 11, 25, 23, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        val stats = StatsBuilder.build(raw(times = listOf(late)), now, plus2)
        assertEquals(1, stats.reviewedToday)
    }

    // ---- activity

    @Test
    fun activityListsTheLastSevenDaysOldestFirstEndingToday() {
        val stats = build(raw(times = listOf(reviewAt(0), reviewAt(0), reviewAt(6), reviewAt(7))))
        assertEquals(7, stats.activity.size)
        assertEquals(today.minusDays(6), stats.activity.first().date)
        assertEquals(today, stats.activity.last().date)
        assertEquals(1, stats.activity.first().reviews) // day 6 is inside the window
        assertEquals(2, stats.activity.last().reviews)
        assertEquals(3, stats.activity.sumOf { it.reviews }) // day 7 is outside it
    }

    // ---- ratings, types, categories

    @Test
    fun ratingsListHardGoodAndEasyOnlyAndTheTotal() {
        val stats = build(raw(ratings = mapOf(ReviewRating.GOOD to 5, ReviewRating.HARD to 2)))
        assertEquals(7, stats.totalReviews)
        assertTrue(stats.hasReviewHistory)
        assertEquals(
            listOf(ReviewRating.HARD to 2, ReviewRating.GOOD to 5, ReviewRating.EASY to 0),
            stats.ratingCounts.map { it.rating to it.count },
        )
    }

    @Test
    fun anAgainRowThatSomehowExistsIsNotCounted() {
        val stats = build(raw(ratings = mapOf(ReviewRating.AGAIN to 4, ReviewRating.EASY to 1)))
        assertEquals(1, stats.totalReviews)
        assertTrue(stats.ratingCounts.none { it.rating == ReviewRating.AGAIN })
    }

    @Test
    fun typeCountsListEveryTypeEvenWhenZero() {
        val stats = build(raw(types = mapOf(KnowledgeType.WORD to 3)))
        assertEquals(
            listOf(KnowledgeType.WORD to 3, KnowledgeType.CONCEPT to 0, KnowledgeType.OTHER to 0),
            stats.typeCounts.map { it.type to it.count },
        )
    }

    @Test
    fun uncategorizedItemsJoinTheCategoryListAndItIsSortedAndCapped() {
        val cats = listOf(
            CategoryCount("Science", 4), CategoryCount("art", 2), CategoryCount("Math", 2),
            CategoryCount("History", 1), CategoryCount("Music", 1),
        )
        val stats = build(raw(categories = cats, uncategorized = 3))
        assertEquals(
            listOf("Science", "Uncategorized", "art", "Math", "History"),
            stats.topCategories.map { it.name },
        )
    }

    @Test
    fun noUncategorizedItemsAddsNoRow() {
        val stats = build(raw(categories = listOf(CategoryCount("Science", 1)), uncategorized = 0))
        assertEquals(listOf("Science"), stats.topCategories.map { it.name })
    }

    // ---- empty and query

    @Test
    fun isEmptyOnlyWhenThereIsNothingAtAll() {
        assertTrue(build(raw()).isEmpty)
        assertFalse(build(raw(ratings = mapOf(ReviewRating.EASY to 1))).isEmpty)
    }

    @Test
    fun queryUsesCalendarDayBoundariesAndTheQueueThresholds() {
        val query = StatsBuilder.query(now, zone)
        assertEquals(startOf(today.plusDays(1)), query.startOfTomorrow)
        assertEquals(startOf(today.minusDays(6)), query.weekStart)
        assertEquals(ReviewQueueBuilder.DIFFICULT_EASE_BELOW, query.difficultEaseBelow, 0.0)
        assertEquals(StatsBuilder.MATURE_INTERVAL_DAYS, query.matureIntervalDays)
        assertEquals(startOf(today.minusDays(StatsBuilder.HISTORY_DAYS)), query.historySince)
    }
}