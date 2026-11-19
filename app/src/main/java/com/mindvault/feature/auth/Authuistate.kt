package com.mindvault.feature.auth

import com.mindvault.domain.model.AuthError
import com.mindvault.domain.model.AuthFieldError

enum class AuthMode { SignIn, SignUp }

enum class GoogleProblem { NoAccount, Failed }

/** Typed text lives in the ViewModel as Compose state (keeps the cursor stable); everything else is here. */
data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val isAvailable: Boolean = true,
    val googleAvailable: Boolean = false,
    val emailEnabled: Boolean = false,
    /** True while any sign-in/up request or the Google account sheet is in progress. */
    val isSubmitting: Boolean = false,
    val showPassword: Boolean = false,
    val emailError: AuthFieldError? = null,
    val passwordError: AuthFieldError? = null,
    val formError: AuthError? = null,
    val googleProblem: GoogleProblem? = null,
    /** Set after sign-up when the user must open the email link. */
    val confirmationSentTo: String? = null,
    /** True once a session exists; the screen closes itself. */
    val signedIn: Boolean = false,
) {
    /** At least one sign-in method is available in this build. */
    val canSignIn: Boolean get() = emailEnabled || googleAvailable
}