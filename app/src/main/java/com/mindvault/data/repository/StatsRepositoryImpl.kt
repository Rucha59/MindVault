package com.mindvault.data.repository

import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.domain.model.CategoryCount
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.StatsQuery
import com.mindvault.domain.model.StatsRaw
import com.mindvault.domain.repository.StatsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val active: ActiveDatabase,
) : StatsRepository {

    override fun observeRaw(query: StatsQuery): Flow<StatsRaw> =
        active.observe { db ->
            val dao = db.statsDao()
            combine(
                dao.observeCounts(query.weekStart, query.startOfTomorrow, query.difficultEaseBelow, query.matureIntervalDays),
                dao.observeTypeCounts(),
                dao.observeTopCategories(query.topCategoryLimit),
                dao.observeRatingCounts(),
                dao.observeReviewTimes(query.historySince),
            ) { counts, types, categories, ratings, times ->
                StatsRaw(
                    totalItems = counts.totalItems,
                    addedRecently = counts.addedRecently,
                    uncategorizedItems = counts.uncategorizedItems,
                    categoryCount = counts.categoryCount,
                    deckCount = counts.deckCount,
                    flashcardCount = counts.flashcardCount,
                    newCards = counts.newCards,
                    dueReviews = counts.dueReviews,
                    difficult = counts.difficult,
                    learning = counts.learning,
                    mature = counts.mature,
                    typeCounts = types
                        .groupBy { row -> knowledgeTypeOf(row.name) }
                        .mapValues { (_, rows) -> rows.sumOf { it.total } },
                    topCategories = categories.map { CategoryCount(it.name, it.total) },
                    ratingCounts = ratings
                        .mapNotNull { row ->
                            ReviewRating.entries.firstOrNull { it.name == row.name }?.let { it to row.total }
                        }
                        .toMap(),
                    reviewTimes = times,
                )
            }
        }

    /** Unknown stored values count as OTHER, like the item mappers. */
    private fun knowledgeTypeOf(name: String): KnowledgeType =
        KnowledgeType.entries.firstOrNull { it.name == name } ?: KnowledgeType.OTHER
}