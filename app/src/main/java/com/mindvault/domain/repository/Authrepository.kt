package com.mindvault.domain.repository

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.SignUpOutcome
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** False when this build has no Supabase URL/key. Accounts are then hidden; guest mode still works. */
    val isAvailable: Boolean

    /** False when email/password sign-in is switched off for this build (Google only). */
    val isEmailSignInEnabled: Boolean

    /** False when this build has no Google web client ID. The Google button is then hidden. */
    val isGoogleSignInAvailable: Boolean

    fun observeSessionState(): Flow<SessionState>

    suspend fun signUpWithEmail(email: String, password: String): AuthResult<SignUpOutcome>

    suspend fun signInWithEmail(email: String, password: String): AuthResult<Unit>

    /** [rawNonce] is the un-hashed nonce whose SHA-256 was sent to Google when the token was requested. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthResult<Unit>

    /**
     * Permanently deletes the signed-in account and its cloud data (server-side function
     * `delete_my_account`), then ends the local session. Data saved on this device is not touched.
     */
    suspend fun deleteAccount(): AuthResult<Unit>

    /** Always ends the local session, even if the server cannot be reached. */
    suspend fun signOut(): AuthResult<Unit>
}