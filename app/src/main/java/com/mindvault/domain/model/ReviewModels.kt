package com.mindvault.domain.model

/** How well the user remembered a card. */
enum class ReviewRating { AGAIN, HARD, GOOD, EASY }

/** Which cards a rated session contains. Practice is separate (it records nothing). */
enum class ReviewMode { DAILY, NEW_CARDS, DIFFICULT }

/** What a deck can offer for review today. */
data class ReviewCounts(
    val dueReviews: Int,
    val newCards: Int,
    val difficult: Int,
) {
    /** Cards in a Daily Review: due reviews plus the new cards still allowed today. */
    val dailyTotal: Int get() = dueReviews + newCards
}