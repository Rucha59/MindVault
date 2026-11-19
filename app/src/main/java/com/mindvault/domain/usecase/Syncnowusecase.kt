package com.mindvault.domain.usecase

import com.mindvault.domain.repository.SyncRepository
import javax.inject.Inject

class SyncNowUseCase @Inject constructor(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.syncNow()
}