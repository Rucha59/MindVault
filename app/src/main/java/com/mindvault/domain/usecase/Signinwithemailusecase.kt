package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult<Unit> {
        val emailError = AuthValidator.validateEmail(email)
        val passwordError = AuthValidator.validatePassword(password, isSignUp = false)
        if (emailError != null || passwordError != null) return AuthResult.Invalid(emailError, passwordError)
        return repository.signInWithEmail(AuthValidator.normalizeEmail(email), password)
    }
}