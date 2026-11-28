package com.mindvault.domain.model

import java.time.LocalDate

/** The moments and thresholds the stats queries need. All timestamps are UTC epoch millis. */
data class StatsQuery(
    /** Start of the day six days ago: "the last 7 days" including today. */
    val weekStart: Long,
    /** A reviewed card is due today when its due time is before this. */
    val startOfTomorrow: Long,
    val difficultEaseBelow: Double,
    val matureIntervalDays: Int,
    val topCategoryLimit: Int,
    /** How far back review times are read (bounds the streak). */
    val historySince: Long,
)

/** Raw numbers read from the vault, before they are turned into [ProgressStats]. */
data class StatsRaw(
    val totalItems: Int,
    val addedRecently: Int,
    val uncategorizedItems: Int,
    val categoryCount: Int,
    val deckCount: Int,
    val flashcardCount: Int,
    val newCards: Int,
    val dueReviews: Int,
    val difficult: Int,
    val learning: Int,
    val mature: Int,
    val typeCounts: Map<KnowledgeType, Int>,
    val topCategories: List<CategoryCount>,
    val ratingCounts: Map<ReviewRating, Int>,
    val reviewTimes: List<Long>,
)

data class TypeCount(val type: KnowledgeType, val count: Int)
data class RatingCount(val rating: ReviewRating, val count: Int)

/** Counted answers (Hard, Good, Easy) on one calendar day (device time zone). */
data class DayActivity(val date: LocalDate, val reviews: Int)

/** Everything the Profile card and the Stats screen show. Derived from saved data only; nothing is invented. */
data class ProgressStats(
    val totalItems: Int,
    val addedLast7Days: Int,
    val categoryCount: Int,
    val typeCounts: List<TypeCount>,
    val topCategories: List<CategoryCount>,
    val deckCount: Int,
    /** One per deck membership: an item in two decks is two flashcards. */
    val flashcardCount: Int,
    /** Reviewed cards due today or overdue. New cards are counted separately in [newCards]. */
    val dueReviews: Int,
    val newCards: Int,
    val difficult: Int,
    val learning: Int,
    val mature: Int,
    val reviewedToday: Int,
    val streakDays: Int,
    /** The last 7 days, oldest first; the last entry is today. */
    val activity: List<DayActivity>,
    val totalReviews: Int,
    val ratingCounts: List<RatingCount>,
) {
    val hasReviewHistory: Boolean get() = totalReviews > 0
    val isEmpty: Boolean get() = totalItems == 0 && flashcardCount == 0 && totalReviews == 0
}