package com.mindvault.feature.profile

import com.mindvault.domain.model.SyncStatus
import com.mindvault.domain.model.ThemePreference

/** What the Account section shows. [Hidden] when accounts are unavailable in this build or still loading. */
sealed interface AccountState {
    data object Hidden : AccountState
    data object Guest : AccountState
    data class SignedIn(val email: String?, val needsRefresh: Boolean) : AccountState
    data object SessionExpired : AccountState
}

data class ProfileUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    /** True when the last attempt to save a choice failed. */
    val saveFailed: Boolean = false,
    val account: AccountState = AccountState.Hidden,
    val isSigningOut: Boolean = false,
    val signOutFailed: Boolean = false,
    /** True after a sign-out that could not upload everything, so the vault stayed on this device. */
    val signOutKeptLocalCopy: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
    /** True after a successful deletion, so the guest card can confirm it. */
    val accountDeleted: Boolean = false,
    /** Cloud sync status for the signed-in account card. */
    val sync: SyncStatus = SyncStatus.Inactive,
)