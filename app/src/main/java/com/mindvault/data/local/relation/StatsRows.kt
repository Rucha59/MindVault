package com.mindvault.data.local.relation

/** One row of numbers for the stats screen; column aliases in StatsDao.observeCounts must match these names. */
data class StatsCountsRow(
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
)

/** A label and how many rows have it (item type, category name or rating). */
data class NameCountRow(
    val name: String,
    val total: Int,
)