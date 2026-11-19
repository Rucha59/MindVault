package com.mindvault.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.model.AuthResult
import com.mindvault.domain.model.SessionState
import com.mindvault.domain.model.ThemePreference
import com.mindvault.domain.usecase.DeleteAccountUseCase
import com.mindvault.domain.usecase.IsAuthAvailableUseCase
import com.mindvault.domain.usecase.ObserveSessionStateUseCase
import com.mindvault.domain.usecase.ObserveSyncStatusUseCase
import com.mindvault.domain.usecase.ObserveThemePreferenceUseCase
import com.mindvault.domain.usecase.SetThemePreferenceUseCase
import com.mindvault.domain.usecase.SignOutOutcome
import com.mindvault.domain.usecase.SignOutWithSyncUseCase
import com.mindvault.domain.usecase.SyncNowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeThemePreference: ObserveThemePreferenceUseCase,
    private val setThemePreference: SetThemePreferenceUseCase,
    observeSessionState: ObserveSessionStateUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    isAuthAvailable: IsAuthAvailableUseCase,
    private val signOut: SignOutWithSyncUseCase,
    private val syncNow: SyncNowUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    /** Short-lived flags that are not stored anywhere else. */
    private data class Transient(
        val saveFailed: Boolean = false,
        val signingOut: Boolean = false,
        val signOutFailed: Boolean = false,
        val keptLocalCopy: Boolean = false,
        val deleting: Boolean = false,
        val deleteFailed: Boolean = false,
        val accountDeleted: Boolean = false,
    )

    private val transient = MutableStateFlow(Transient())
    private val authAvailable = isAuthAvailable()

    val uiState: StateFlow<ProfileUiState> = combine(
        observeThemePreference(),
        observeSessionState(),
        observeSyncStatus(),
        transient,
    ) { preference, session, sync, flags ->
        ProfileUiState(
            themePreference = preference,
            saveFailed = flags.saveFailed,
            account = accountState(session),
            isSigningOut = flags.signingOut,
            signOutFailed = flags.signOutFailed,
            signOutKeptLocalCopy = flags.keptLocalCopy,
            isDeleting = flags.deleting,
            deleteFailed = flags.deleteFailed,
            accountDeleted = flags.accountDeleted,
            sync = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onThemeSelected(preference: ThemePreference) {
        viewModelScope.launch {
            transient.update { it.copy(saveFailed = false) }
            try {
                setThemePreference(preference)
            } catch (e: IOException) {
                transient.update { it.copy(saveFailed = true) }
            }
        }
    }

    fun onSyncNowClicked() {
        viewModelScope.launch { syncNow() } // the result shows up in uiState.sync
    }

    fun onSignOutConfirmed() {
        if (transient.value.signingOut || transient.value.deleting) return
        viewModelScope.launch {
            transient.update {
                it.copy(signingOut = true, signOutFailed = false, keptLocalCopy = false, accountDeleted = false)
            }
            val outcome = signOut()
            transient.update {
                it.copy(
                    signingOut = false,
                    signOutFailed = outcome == SignOutOutcome.Failed,
                    keptLocalCopy = outcome == SignOutOutcome.KeptLocalCopy,
                )
            }
        }
    }

    fun onDeleteAccountConfirmed() {
        if (transient.value.deleting || transient.value.signingOut) return
        viewModelScope.launch {
            transient.update { it.copy(deleting = true, deleteFailed = false, signOutFailed = false, keptLocalCopy = false) }
            val result = deleteAccount()
            transient.update {
                it.copy(
                    deleting = false,
                    deleteFailed = result is AuthResult.Failure,
                    accountDeleted = result is AuthResult.Success,
                )
            }
        }
    }

    private fun accountState(session: SessionState): AccountState = when {
        !authAvailable -> AccountState.Hidden
        else -> when (session) {
            SessionState.Loading -> AccountState.Hidden
            SessionState.SignedOut -> AccountState.Guest
            is SessionState.SignedIn -> AccountState.SignedIn(session.user.email, session.needsRefresh)
            SessionState.Expired -> AccountState.SessionExpired
        }
    }
}