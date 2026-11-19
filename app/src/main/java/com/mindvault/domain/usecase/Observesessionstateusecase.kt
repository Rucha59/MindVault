package com.mindvault.domain.usecase

import com.mindvault.domain.model.SessionState
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionStateUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<SessionState> = repository.observeSessionState()
}