package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.SignUpOutcome
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult<SignUpOutcome> {
        val emailError = AuthValidator.validateEmail(email)
        val passwordError = AuthValidator.validatePassword(password, isSignUp = true)
        if (emailError != null || passwordError != null) return AuthResult.Invalid(emailError, passwordError)
        return repository.signUpWithEmail(AuthValidator.normalizeEmail(email), password)
    }
}