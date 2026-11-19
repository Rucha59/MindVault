package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

/** Phase A: ends the session only. Phase B extends this to clear the user's local data safely. */
class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): AuthResult<Unit> = repository.signOut()
}