package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthError
import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String, rawNonce: String): AuthResult<Unit> {
        if (idToken.isBlank() || rawNonce.isBlank()) return AuthResult.Failure(AuthError.Unknown)
        return repository.signInWithGoogle(idToken, rawNonce)
    }
}