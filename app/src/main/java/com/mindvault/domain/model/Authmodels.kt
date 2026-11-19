package com.mindvault.domain.model

/** The signed-in account, reduced to what the app needs. No Supabase types in the domain. */
data class AuthUser(val id: String, val email: String?)

/**
 * What the app knows about the account session.
 * [SignedOut] is guest mode: the app stays fully usable.
 */
sealed interface SessionState {
    /** Stored session is still being loaded at app start. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    /** [needsRefresh] is true while the token could not be refreshed yet (for example offline). */
    data class SignedIn(val user: AuthUser, val needsRefresh: Boolean = false) : SessionState

    /** A previously signed-in session was rejected (revoked or refresh token no longer valid). */
    data object Expired : SessionState
}

enum class AuthError {
    InvalidCredentials,
    EmailNotConfirmed,
    WeakPassword,
    RateLimited,
    Network,

    /** Supabase URL/key are missing from this build. */
    Unavailable,
    Unknown,
}

enum class AuthFieldError {
    EmailEmpty,
    EmailInvalid,
    EmailTooLong,
    PasswordEmpty,
    PasswordTooShort,
    PasswordTooLong,
}

enum class SignUpOutcome {
    /** Email confirmation is off (or not needed): a session already exists. */
    SignedIn,

    /** The user must open the email link. Also returned for already-registered addresses (no account enumeration). */
    ConfirmationRequired,
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Invalid(val emailError: AuthFieldError?, val passwordError: AuthFieldError?) : AuthResult<Nothing>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}