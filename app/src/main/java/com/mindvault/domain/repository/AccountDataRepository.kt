package com.mindvault.domain.repository

import com.mindvault.domain.model.DataOwner
import kotlinx.coroutines.flow.StateFlow

interface AccountDataRepository {
    /** The vault currently in use. Changes when the session changes. */
    val owner: StateFlow<DataOwner>

    /** Starts following the session so the right vault is open. Safe to call more than once. */
    fun start()

    /** Closes and deletes one account's on-device vault. Switches to the guest vault first if it is open. */
    suspend fun deleteAccountData(userId: String)
}