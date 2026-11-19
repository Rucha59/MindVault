package com.mindvault.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.SignUpOutcome
import com.mindvault.domain.usecase.IsAuthAvailableUseCase
import com.mindvault.domain.usecase.IsEmailSignInEnabledUseCase
import com.mindvault.domain.usecase.IsGoogleSignInAvailableUseCase
import com.mindvault.domain.usecase.ObserveSessionStateUseCase
import com.mindvault.domain.usecase.SignInWithEmailUseCase
import com.mindvault.domain.usecase.SignInWithGoogleUseCase
import com.mindvault.domain.usecase.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signUpWithEmail: SignUpWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    observeSessionState: ObserveSessionStateUseCase,
    isAuthAvailable: IsAuthAvailableUseCase,
    isGoogleSignInAvailable: IsGoogleSignInAvailableUseCase,
    isEmailSignInEnabled: IsEmailSignInEnabledUseCase,
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAvailable = isAuthAvailable(),
            googleAvailable = isGoogleSignInAvailable(),
            emailEnabled = isEmailSignInEnabled(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Also covers sign-in completed elsewhere (e.g. the email link opened while this screen is showing).
        viewModelScope.launch {
            observeSessionState().collect { session ->
                if (session is SessionState.SignedIn) _uiState.update { it.copy(signedIn = true) }
            }
        }
    }

    fun onEmailChange(value: String) {
        email = value
        _uiState.update { it.copy(emailError = null, formError = null, googleProblem = null) }
    }

    fun onPasswordChange(value: String) {
        password = value
        _uiState.update { it.copy(passwordError = null, formError = null, googleProblem = null) }
    }

    fun onToggleShowPassword() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun onToggleMode() {
        if (_uiState.value.isSubmitting) return
        password = ""
        _uiState.update {
            it.copy(
                mode = if (it.mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn,
                emailError = null,
                passwordError = null,
                formError = null,
                googleProblem = null,
                showPassword = false,
            )
        }
    }

    fun onConfirmationDismissed() {
        _uiState.update { it.copy(confirmationSentTo = null, mode = AuthMode.SignIn) }
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || !current.isAvailable || !current.emailEnabled) return // blocks double submits
        beginSubmit()
        val submittedEmail = email.trim()
        val submittedPassword = password
        viewModelScope.launch {
            when (current.mode) {
                AuthMode.SignIn -> handle(signInWithEmail(submittedEmail, submittedPassword), submittedEmail)
                AuthMode.SignUp -> handle(signUpWithEmail(submittedEmail, submittedPassword), submittedEmail)
            }
        }
    }

    /** Call right before showing the Google account sheet. Returns false if a request is already running. */
    fun onGoogleStarted(): Boolean {
        val current = _uiState.value
        if (current.isSubmitting || !current.isAvailable || !current.googleAvailable) return false
        beginSubmit()
        return true
    }

    fun onGoogleOutcome(outcome: GoogleTokenOutcome) {
        when (outcome) {
            is GoogleTokenOutcome.Token -> viewModelScope.launch {
                handle(signInWithGoogle(outcome.idToken, outcome.rawNonce), submittedEmail = "")
            }
            GoogleTokenOutcome.Cancelled -> _uiState.update { it.copy(isSubmitting = false) }
            GoogleTokenOutcome.NoAccount ->
                _uiState.update { it.copy(isSubmitting = false, googleProblem = GoogleProblem.NoAccount) }
            GoogleTokenOutcome.Failed ->
                _uiState.update { it.copy(isSubmitting = false, googleProblem = GoogleProblem.Failed) }
        }
    }

    private fun beginSubmit() = _uiState.update {
        it.copy(isSubmitting = true, emailError = null, passwordError = null, formError = null, googleProblem = null)
    }

    // Typed input is kept on every failure so the user can correct it.
    private fun handle(result: AuthResult<Any?>, submittedEmail: String) {
        when (result) {
            is AuthResult.Success -> {
                if (result.value == SignUpOutcome.ConfirmationRequired) {
                    password = ""
                    _uiState.update { it.copy(isSubmitting = false, confirmationSentTo = submittedEmail) }
                } else {
                    // The session collector sets signedIn.
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
            is AuthResult.Invalid -> _uiState.update {
                it.copy(isSubmitting = false, emailError = result.emailError, passwordError = result.passwordError)
            }
            is AuthResult.Failure -> _uiState.update { it.copy(isSubmitting = false, formError = result.error) }
        }
    }
}