package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.repository.AccountDataRepository
import com.mindvault.domain.repository.AuthRepository
import com.mindvault.domain.repository.SyncRepository
import javax.inject.Inject

sealed interface SignOutOutcome {
    /** Signed out; this account's on-device vault was removed because everything was uploaded first. */
    data object Cleared : SignOutOutcome

    /** Signed out, but some changes could not be uploaded, so the vault was kept on this device. */
    data object KeptLocalCopy : SignOutOutcome

    data object Failed : SignOutOutcome
}

/**
 * Phase C3 sign-out: one last sync, then sign out, then remove the account's on-device vault only if every
 * change reached the cloud. Otherwise the vault stays, so nothing is ever lost by signing out.
 */
class SignOutWithSyncUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val accountData: AccountDataRepository,
    private val sync: SyncRepository,
) {
    suspend operator fun invoke(): SignOutOutcome {
        val owner = accountData.owner.value
        if (owner !is DataOwner.Account) {
            return if (auth.signOut() is AuthResult.Success) SignOutOutcome.Cleared else SignOutOutcome.Failed
        }
        return sync.withFinalSync { fullyUploaded ->
            if (auth.signOut() is AuthResult.Failure) {
                SignOutOutcome.Failed
            } else if (fullyUploaded) {
                accountData.deleteAccountData(owner.userId)
                SignOutOutcome.Cleared
            } else {
                SignOutOutcome.KeptLocalCopy
            }
        }
    }
}