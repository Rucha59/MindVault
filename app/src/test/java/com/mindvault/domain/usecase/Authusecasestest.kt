package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthError
import com.mindvault.domain.model.AuthFieldError
import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.SignUpOutcome
import com.mindvault.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAuthRepository(
    override val isAvailable: Boolean = true,
    override val isGoogleSignInAvailable: Boolean = true,
    override val isEmailSignInEnabled: Boolean = true,
) : AuthRepository {
    var signInResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signUpResult: AuthResult<SignUpOutcome> = AuthResult.Success(SignUpOutcome.ConfirmationRequired)
    var googleResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signOutResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var deleteResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var deleteCalls = 0
    val signInCalls = mutableListOf<Pair<String, String>>()
    val signUpCalls = mutableListOf<Pair<String, String>>()
    val googleCalls = mutableListOf<Pair<String, String>>()
    var signOutCalls = 0

    override fun observeSessionState(): Flow<SessionState> = flowOf(SessionState.SignedOut)
    override suspend fun signUpWithEmail(email: String, password: String): AuthResult<SignUpOutcome> {
        signUpCalls += email to password
        return signUpResult
    }
    override suspend fun signInWithEmail(email: String, password: String): AuthResult<Unit> {
        signInCalls += email to password
        return signInResult
    }
    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthResult<Unit> {
        googleCalls += idToken to rawNonce
        return googleResult
    }
    override suspend fun deleteAccount(): AuthResult<Unit> {
        deleteCalls++
        return deleteResult
    }
    override suspend fun signOut(): AuthResult<Unit> {
        signOutCalls++
        return signOutResult
    }
}

class AuthUseCasesTest {
    private val repo = FakeAuthRepository()

    @Test fun signInWithInvalidInputNeverCallsRepository() = runBlocking {
        val result = SignInWithEmailUseCase(repo)("nope", "")
        assertEquals(AuthResult.Invalid(AuthFieldError.EmailInvalid, AuthFieldError.PasswordEmpty), result)
        assertTrue(repo.signInCalls.isEmpty())
    }

    @Test fun signInNormalizesEmailButNotPassword() = runBlocking {
        SignInWithEmailUseCase(repo)("  A@B.com ", " pass ")
        assertEquals(listOf("a@b.com" to " pass "), repo.signInCalls)
    }

    @Test fun signInPassesThroughFailure() = runBlocking {
        repo.signInResult = AuthResult.Failure(AuthError.InvalidCredentials)
        val result = SignInWithEmailUseCase(repo)("a@b.com", "x")
        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
    }

    @Test fun signUpRejectsShortPassword() = runBlocking {
        val result = SignUpWithEmailUseCase(repo)("a@b.com", "short")
        assertEquals(AuthResult.Invalid(null, AuthFieldError.PasswordTooShort), result)
        assertTrue(repo.signUpCalls.isEmpty())
    }

    @Test fun signUpPassesThroughOutcome() = runBlocking {
        repo.signUpResult = AuthResult.Success(SignUpOutcome.SignedIn)
        val result = SignUpWithEmailUseCase(repo)("a@b.com", "longenough")
        assertEquals(AuthResult.Success(SignUpOutcome.SignedIn), result)
        assertEquals(listOf("a@b.com" to "longenough"), repo.signUpCalls)
    }

    @Test fun googleSignInRejectsBlankTokenOrNonce() = runBlocking {
        assertEquals(AuthResult.Failure(AuthError.Unknown), SignInWithGoogleUseCase(repo)("", "nonce"))
        assertEquals(AuthResult.Failure(AuthError.Unknown), SignInWithGoogleUseCase(repo)("token", " "))
        assertTrue(repo.googleCalls.isEmpty())
    }

    @Test fun googleSignInPassesTokenAndRawNonce() = runBlocking {
        val result = SignInWithGoogleUseCase(repo)("token", "nonce")
        assertEquals(AuthResult.Success(Unit), result)
        assertEquals(listOf("token" to "nonce"), repo.googleCalls)
    }

    @Test fun googleAvailabilityNeedsBothFlags() {
        assertTrue(IsGoogleSignInAvailableUseCase(FakeAuthRepository(true, true))())
        assertFalse(IsGoogleSignInAvailableUseCase(FakeAuthRepository(true, false))())
        assertFalse(IsGoogleSignInAvailableUseCase(FakeAuthRepository(false, true))())
    }

    @Test fun emailSignInNeedsAccountsAndTheFlag() {
        assertTrue(IsEmailSignInEnabledUseCase(FakeAuthRepository(isEmailSignInEnabled = true))())
        assertFalse(IsEmailSignInEnabledUseCase(FakeAuthRepository(isEmailSignInEnabled = false))())
        assertFalse(IsEmailSignInEnabledUseCase(FakeAuthRepository(isAvailable = false))())
    }

    @Test fun deleteAccountDelegatesAndPassesFailureThrough() = runBlocking {
        assertEquals(AuthResult.Success(Unit), DeleteAccountUseCase(repo)())
        repo.deleteResult = AuthResult.Failure(AuthError.Network)
        assertEquals(AuthResult.Failure(AuthError.Network), DeleteAccountUseCase(repo)())
        assertEquals(2, repo.deleteCalls)
    }

    @Test fun signOutDelegates() = runBlocking {
        assertEquals(AuthResult.Success(Unit), SignOutUseCase(repo)())
        assertEquals(1, repo.signOutCalls)
    }
}