package com.mindvault.domain.usecase

import com.mindvault.domain.model.CategoryCount
import com.mindvault.domain.model.DayActivity
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ProgressStats
import com.mindvault.domain.model.RatingCount
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.StatsQuery
import com.mindvault.domain.model.StatsRaw
import com.mindvault.domain.model.TypeCount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure stats logic (no Android, no I/O), so it is easy to unit test. Days are calendar days in the given zone. */
object StatsBuilder {
    /** A reviewed card with an interval of at least this many days is "mature"; shorter is "learning". */
    const val MATURE_INTERVAL_DAYS = 21
    const val RECENT_DAYS = 7
    const val TOP_CATEGORY_LIMIT = 5

    /** The ratings that are logged and shown. Again is not a category. */
    val LOGGED_RATINGS = listOf(ReviewRating.HARD, ReviewRating.GOOD, ReviewRating.EASY)

    /** Review times are read this far back, so a streak is counted up to about this many days. */
    const val HISTORY_DAYS = 400L

    fun query(now: Long, zone: ZoneId = ZoneId.systemDefault()) = StatsQuery(
        weekStart = startOfDay(now, zone, -(RECENT_DAYS - 1).toLong()),
        startOfTomorrow = startOfDay(now, zone, 1),
        difficultEaseBelow = ReviewQueueBuilder.DIFFICULT_EASE_BELOW,
        matureIntervalDays = MATURE_INTERVAL_DAYS,
        topCategoryLimit = TOP_CATEGORY_LIMIT,
        historySince = startOfDay(now, zone, -HISTORY_DAYS),
    )

    fun build(raw: StatsRaw, now: Long, zone: ZoneId = ZoneId.systemDefault()): ProgressStats {
        val today = dayOf(now, zone)
        val perDay = raw.reviewTimes.groupingBy { dayOf(it, zone) }.eachCount()
        val activity = (RECENT_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayActivity(date, perDay[date] ?: 0)
        }
        val categories = (
                raw.topCategories +
                        listOfNotNull(
                            raw.uncategorizedItems.takeIf { it > 0 }
                                ?.let { CategoryCount(HomeSummaryBuilder.UNCATEGORIZED, it) },
                        )
                )
            .sortedWith(compareByDescending<CategoryCount> { it.count }.thenBy { it.name.lowercase() })
            .take(TOP_CATEGORY_LIMIT)

        return ProgressStats(
            totalItems = raw.totalItems,
            addedLast7Days = raw.addedRecently,
            categoryCount = raw.categoryCount,
            typeCounts = KnowledgeType.entries.map { TypeCount(it, raw.typeCounts[it] ?: 0) },
            topCategories = categories,
            deckCount = raw.deckCount,
            flashcardCount = raw.flashcardCount,
            dueReviews = raw.dueReviews,
            newCards = raw.newCards,
            difficult = raw.difficult,
            learning = raw.learning,
            mature = raw.mature,
            reviewedToday = perDay[today] ?: 0,
            streakDays = streak(perDay.keys, today),
            activity = activity,
            totalReviews = LOGGED_RATINGS.sumOf { raw.ratingCounts[it] ?: 0 },
            ratingCounts = LOGGED_RATINGS.map { RatingCount(it, raw.ratingCounts[it] ?: 0) },
        )
    }

    /**
     * Consecutive days with at least one counted answer, counted back from today. A day with no review yet does
     * not break the streak until it is over, so counting starts from yesterday when today has none.
     */
    internal fun streak(reviewDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in reviewDays) today else today.minusDays(1)
        var count = 0
        while (day in reviewDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private fun dayOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private fun startOfDay(now: Long, zone: ZoneId, plusDays: Long): Long =
        dayOf(now, zone).plusDays(plusDays).atStartOfDay(zone).toInstant().toEpochMilli()
}