package com.mindvault.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultTextField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.domain.model.AuthError
import com.mindvault.domain.model.AuthFieldError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/** Sign in / Create account. Optional: the app works without an account. Reached from Profile. */
@Composable
fun AuthScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleFetcher = remember { GoogleIdTokenFetcher() }

    LaunchedEffect(state.signedIn) { if (state.signedIn) onClose() }

    val title = when {
        !state.isAvailable || !state.canSignIn -> "Account"
        state.confirmationSentTo != null -> "Check your email"
        state.emailEnabled && state.mode == AuthMode.SignUp -> "Create account"
        else -> "Sign in"
    }

    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = title, onBackClick = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal)
                .padding(bottom = MindVaultSpacing.space6),
        ) {
            when {
                !state.isAvailable || !state.canSignIn -> MessagePanel(
                    message = "This version of MindVault can't sign in yet. Everything else works as usual.",
                    actionLabel = "Back",
                    onAction = onClose,
                )
                state.confirmationSentTo != null -> MessagePanel(
                    message = "If an account can be created for ${state.confirmationSentTo}, we've sent a " +
                            "confirmation link. Open it on this phone to finish, then sign in.",
                    actionLabel = "Back to sign in",
                    onAction = viewModel::onConfirmationDismissed,
                )
                else -> AuthForm(
                    state = state,
                    email = viewModel.email,
                    password = viewModel.password,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onToggleShowPassword = viewModel::onToggleShowPassword,
                    onSubmit = viewModel::submit,
                    onToggleMode = viewModel::onToggleMode,
                    onNotNow = onClose,
                    onGoogleClick = {
                        if (viewModel.onGoogleStarted()) {
                            scope.launch {
                                val outcome = try {
                                    googleFetcher.fetch(context)
                                } catch (e: CancellationException) {
                                    viewModel.onGoogleOutcome(GoogleTokenOutcome.Cancelled)
                                    throw e
                                }
                                viewModel.onGoogleOutcome(outcome)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthForm(
    state: AuthUiState,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onNotNow: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    if (!state.emailEnabled) {
        GoogleOnlyForm(
            state = state,
            onGoogleClick = {
                focusManager.clearFocus()
                onGoogleClick()
            },
            onNotNow = onNotNow,
        )
        return
    }
    val isSignUp = state.mode == AuthMode.SignUp
    val enabled = !state.isSubmitting
    val linkColors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest)

    Text(
        text = if (isSignUp) "It only takes a minute." else "Sign in to your MindVault account.",
        style = MaterialTheme.typography.bodyLarge,
        color = MindVaultColors.TextSecondary,
    )
    Spacer(Modifier.height(MindVaultSpacing.space6))

    MindVaultTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        enabled = enabled,
        singleLine = true,
        errorText = state.emailError?.let(::fieldErrorMessage),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
    )
    Spacer(Modifier.height(MindVaultSpacing.space3))
    MindVaultTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Password",
        enabled = enabled,
        singleLine = true,
        errorText = state.passwordError?.let(::fieldErrorMessage),
        supportingText = if (isSignUp) "At least 8 characters" else null,
        visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShowPassword) {
                Icon(
                    imageVector = if (state.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (state.showPassword) "Hide password" else "Show password",
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onSubmit()
        }),
    )

    val problem = state.formError?.let(::formErrorMessage) ?: state.googleProblem?.let(::googleProblemMessage)
    if (problem != null) {
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Text(
            text = problem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    Spacer(Modifier.height(MindVaultSpacing.space5))
    PrimaryButton(
        text = when {
            state.isSubmitting && isSignUp -> "Creating account…"
            state.isSubmitting -> "Please wait…"
            isSignUp -> "Create account"
            else -> "Sign in"
        },
        onClick = {
            focusManager.clearFocus()
            onSubmit()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.googleAvailable) {
        Spacer(Modifier.height(MindVaultSpacing.space4))
        OrDivider()
        Spacer(Modifier.height(MindVaultSpacing.space4))
        GoogleSignInButton(
            onClick = {
                focusManager.clearFocus()
                onGoogleClick()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(MindVaultSpacing.space3))
    TextButton(onClick = onToggleMode, enabled = enabled, colors = linkColors, modifier = Modifier.fillMaxWidth()) {
        Text(if (isSignUp) "Already have an account? Sign in" else "New here? Create an account")
    }
    TextButton(onClick = onNotNow, enabled = enabled, colors = linkColors, modifier = Modifier.fillMaxWidth()) {
        Text("Continue without an account")
    }
}

/** Sign-in page while email/password is switched off: Google or continue as a guest. */
@Composable
private fun GoogleOnlyForm(
    state: AuthUiState,
    onGoogleClick: () -> Unit,
    onNotNow: () -> Unit,
) {
    val linkColors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest)
    Text(
        text = "Sign in with your Google account. You can also keep using MindVault without one.",
        style = MaterialTheme.typography.bodyLarge,
        color = MindVaultColors.TextSecondary,
    )
    val problem = state.formError?.let(::formErrorMessage) ?: state.googleProblem?.let(::googleProblemMessage)
    if (problem != null) {
        Spacer(Modifier.height(MindVaultSpacing.space4))
        Text(
            text = problem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Spacer(Modifier.height(MindVaultSpacing.space6))
    GoogleSignInButton(
        onClick = onGoogleClick,
        enabled = !state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(MindVaultSpacing.space3))
    TextButton(
        onClick = onNotNow,
        enabled = !state.isSubmitting,
        colors = linkColors,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continue without an account") }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MindVaultColors.Border)
        Text(
            text = "or",
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextSecondary,
            modifier = Modifier.padding(horizontal = MindVaultSpacing.space3),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MindVaultColors.Border)
    }
}

@Composable
private fun MessagePanel(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space4)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MindVaultColors.TextSecondary,
        )
        PrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
}

private fun fieldErrorMessage(error: AuthFieldError): String = when (error) {
    AuthFieldError.EmailEmpty -> "Enter your email address."
    AuthFieldError.EmailInvalid -> "Enter a valid email address."
    AuthFieldError.EmailTooLong -> "That email address is too long."
    AuthFieldError.PasswordEmpty -> "Enter your password."
    AuthFieldError.PasswordTooShort -> "Use at least 8 characters."
    AuthFieldError.PasswordTooLong -> "That password is too long. Use 72 characters or fewer."
}

private fun formErrorMessage(error: AuthError): String = when (error) {
    AuthError.InvalidCredentials -> "Incorrect email or password."
    AuthError.EmailNotConfirmed -> "Please confirm your email first. Check your inbox for the link."
    AuthError.WeakPassword -> "Please choose a stronger password."
    AuthError.RateLimited -> "Too many attempts. Please wait a few minutes and try again."
    AuthError.Network -> "No connection. Check your internet and try again."
    AuthError.Unavailable -> "Accounts aren't available in this version."
    AuthError.Unknown -> "Something went wrong. Please try again."
}

private fun googleProblemMessage(problem: GoogleProblem): String = when (problem) {
    GoogleProblem.NoAccount -> "No Google account found on this device. Add one in your device settings, then try again."
    GoogleProblem.Failed -> "Google sign-in didn't work. Please try again."
}