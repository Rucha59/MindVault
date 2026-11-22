package com.mindvault.domain.usecase

import com.mindvault.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The daily new-card limit per deck (default 20). */
class ObserveNewCardsPerDayUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeNewCardsPerDay()
}

class SetNewCardsPerDayUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(limit: Int) = repository.setNewCardsPerDay(limit)
}