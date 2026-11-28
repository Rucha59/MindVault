package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ProgressStats
import com.mindvault.domain.repository.StatsRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Profile stats from the same saved data as Home, Vault and Flashcards. "Today" is fixed when the flow is created
 * (like the other review counts), so numbers refresh on the next data change or when the screen is reopened,
 * not at midnight.
 */
class ObserveProgressStatsUseCase @Inject constructor(
    private val repository: StatsRepository,
) {
    operator fun invoke(now: Long = nowMillis(), zone: ZoneId = ZoneId.systemDefault()): Flow<ProgressStats> =
        repository.observeRaw(StatsBuilder.query(now, zone)).map { StatsBuilder.build(it, now, zone) }
}