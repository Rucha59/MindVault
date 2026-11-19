package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthFieldError

/** Shared sign-in / sign-up input rules. Pure logic, no Android or Supabase types. */
object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 8

    /** Supabase hashes with bcrypt, which only uses the first 72 bytes. */
    const val MAX_PASSWORD_LENGTH = 72
    const val MAX_EMAIL_LENGTH = 254

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun validateEmail(raw: String): AuthFieldError? {
        val email = normalizeEmail(raw)
        return when {
            email.isEmpty() -> AuthFieldError.EmailEmpty
            email.length > MAX_EMAIL_LENGTH -> AuthFieldError.EmailTooLong
            !emailPattern.matches(email) -> AuthFieldError.EmailInvalid
            else -> null
        }
    }

    /** Sign-in only checks presence, so older or differently-created passwords are never blocked here. */
    fun validatePassword(raw: String, isSignUp: Boolean): AuthFieldError? = when {
        raw.isEmpty() -> AuthFieldError.PasswordEmpty
        !isSignUp -> null
        raw.length < MIN_PASSWORD_LENGTH -> AuthFieldError.PasswordTooShort
        raw.toByteArray(Charsets.UTF_8).size > MAX_PASSWORD_LENGTH -> AuthFieldError.PasswordTooLong
        else -> null
    }
}