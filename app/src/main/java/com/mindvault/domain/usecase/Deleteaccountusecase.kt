package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.repository.AccountDataRepository
import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Deletes the signed-in account and its cloud data, then that account's on-device vault.
 * The guest vault is never touched. Local data is removed only after the server deletion succeeded.
 */
class DeleteAccountUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val accountData: AccountDataRepository,
) {
    suspend operator fun invoke(): AuthResult<Unit> {
        val owner = accountData.owner.value
        val result = auth.deleteAccount()
        if (result is AuthResult.Success && owner is DataOwner.Account) {
            accountData.deleteAccountData(owner.userId)
        }
        return result
    }
}