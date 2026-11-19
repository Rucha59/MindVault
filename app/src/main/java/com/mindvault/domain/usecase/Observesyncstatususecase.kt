package com.mindvault.domain.usecase

import com.mindvault.domain.model.SyncStatus
import com.mindvault.domain.repository.SyncRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSyncStatusUseCase @Inject constructor(private val repository: SyncRepository) {
    operator fun invoke(): Flow<SyncStatus> = repository.status
}