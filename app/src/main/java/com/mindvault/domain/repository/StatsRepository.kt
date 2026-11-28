package com.mindvault.domain.repository

import com.mindvault.domain.model.StatsQuery
import com.mindvault.domain.model.StatsRaw
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    /** Raw stats for the active vault; re-emits whenever saved items, cards, review state or history change. */
    fun observeRaw(query: StatsQuery): Flow<StatsRaw>
}