package com.mindvault.data.auth

import android.util.Log
import com.mindvault.domain.model.AuthError
import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.AuthUser
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.SignUpOutcome
import com.mindvault.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.postgrest
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * The only place (with AuthModule, EncryptedSessionManager and AuthDeepLinkHandler) that touches Supabase types.
 * The client is created lazily, so a build without keys never builds it.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val config: AuthConfig,
    private val client: dagger.Lazy<SupabaseClient>,
) : AuthRepository {

    override val isAvailable: Boolean get() = config.isConfigured

    override val isEmailSignInEnabled: Boolean get() = config.emailEnabled

    override val isGoogleSignInAvailable: Boolean get() = config.isGoogleConfigured

    override fun observeSessionState(): Flow<SessionState> {
        if (!config.isConfigured) return flowOf(SessionState.SignedOut)
        return flow {
            var wasSignedIn = false
            client.get().auth.sessionStatus.collect { status ->
                val mapped = status.toSessionState(wasSignedIn)
                wasSignedIn = mapped is SessionState.SignedIn
                emit(mapped)
            }
        }.distinctUntilChanged()
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult<SignUpOutcome> =
        if (!config.emailEnabled) AuthResult.Failure(AuthError.Unavailable) else runAuth {
            val auth = client.get().auth
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // With email confirmation on there is no session yet; the user must open the link.
            if (auth.currentSessionOrNull() != null) SignUpOutcome.SignedIn else SignUpOutcome.ConfirmationRequired
        }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult<Unit> =
        if (!config.emailEnabled) AuthResult.Failure(AuthError.Unavailable) else runAuth {
            client.get().auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthResult<Unit> =
        runAuth {
            client.get().auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                nonce = rawNonce
            }
        }

    override suspend fun deleteAccount(): AuthResult<Unit> = runAuth {
        val supabase = client.get()
        // Runs as the signed-in user; the function deletes only auth.uid() (see docs/supabase_delete_account.sql).
        supabase.postgrest.rpc("delete_my_account")
        // The server-side user is gone: end the local session without another server call.
        supabase.auth.clearSession()
    }

    override suspend fun signOut(): AuthResult<Unit> {
        if (!config.isConfigured) return AuthResult.Success(Unit)
        val auth = client.get().auth
        return try {
            auth.signOut()
            AuthResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Server unreachable: still end the session on this device. The server-side token expires on its own.
            Log.w(TAG, "Server sign-out failed (${e::class.simpleName}); clearing local session")
            try {
                auth.clearSession()
                AuthResult.Success(Unit)
            } catch (inner: CancellationException) {
                throw inner
            } catch (inner: Exception) {
                AuthResult.Failure(inner.toAuthError())
            }
        }
    }

    private suspend fun <T> runAuth(block: suspend () -> T): AuthResult<T> {
        if (!config.isConfigured) return AuthResult.Failure(AuthError.Unavailable)
        return try {
            AuthResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Log the type only: messages can contain the email address.
            Log.w(TAG, "Auth call failed: ${e::class.simpleName}")
            AuthResult.Failure(e.toAuthError())
        }
    }

    private fun SessionStatus.toSessionState(wasSignedIn: Boolean): SessionState = when (this) {
        is SessionStatus.Initializing -> SessionState.Loading
        is SessionStatus.Authenticated ->
            session.user?.let { SessionState.SignedIn(AuthUser(it.id, it.email)) } ?: SessionState.Loading
        is SessionStatus.RefreshFailure ->
            client.get().auth.currentUserOrNull()
                ?.let { SessionState.SignedIn(AuthUser(it.id, it.email), needsRefresh = true) }
                ?: SessionState.Loading
        is SessionStatus.NotAuthenticated ->
            if (!isSignOut && wasSignedIn) SessionState.Expired else SessionState.SignedOut
    }

    // If anything fails to compile in this file, it is most likely this mapping (library error-code names).
    private fun Exception.toAuthError(): AuthError = when (this) {
        is AuthRestException -> when {
            errorCode == AuthErrorCode.InvalidCredentials -> AuthError.InvalidCredentials
            errorCode == AuthErrorCode.EmailNotConfirmed -> AuthError.EmailNotConfirmed
            errorCode == AuthErrorCode.WeakPassword -> AuthError.WeakPassword
            errorCode == AuthErrorCode.OverRequestRateLimit ||
                    errorCode == AuthErrorCode.OverEmailSendRateLimit ||
                    statusCode == 429 -> AuthError.RateLimited
            else -> AuthError.Unknown
        }
        is HttpRequestException, is IOException -> AuthError.Network
        else -> AuthError.Unknown
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}