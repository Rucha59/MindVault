package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTextField
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.core.ui.components.SecondaryButton
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.model.ThemePreference
import android.text.format.DateUtils
import com.mindvault.domain.model.SyncProblem
import com.mindvault.domain.model.SyncStatus

/** Profile and settings, reached from the Home top bar. Only settings that actually work are shown. */
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmText by rememberSaveable { mutableStateOf("") }
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Profile", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            if (state.account != AccountState.Hidden) {
                SectionHeader("Account")
                AccountCard(
                    state = state,
                    onSignInClick = onSignInClick,
                    onSyncNowClick = viewModel::onSyncNowClicked,
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteClick = { showDeleteDialog = true },
                )
                Spacer(Modifier.height(MindVaultSpacing.space6))
            }

            SectionHeader("Appearance")
            MindVaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    ThemeOption(
                        label = "System default",
                        supporting = "Match your device setting",
                        selected = state.themePreference == ThemePreference.SYSTEM,
                        onClick = { viewModel.onThemeSelected(ThemePreference.SYSTEM) },
                    )
                    ThemeOption(
                        label = "Light",
                        supporting = null,
                        selected = state.themePreference == ThemePreference.LIGHT,
                        onClick = { viewModel.onThemeSelected(ThemePreference.LIGHT) },
                    )
                    ThemeOption(
                        label = "Dark",
                        supporting = null,
                        selected = state.themePreference == ThemePreference.DARK,
                        onClick = { viewModel.onThemeSelected(ThemePreference.DARK) },
                    )
                }
            }
            if (state.saveFailed) {
                Text(
                    text = "Couldn't save your choice. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = MindVaultSpacing.space3),
                )
            }
            Spacer(Modifier.height(MindVaultSpacing.space6))
            NewCardsPerDaySetting() // delete or comment out this line to hide the setting
            Spacer(Modifier.height(MindVaultSpacing.space3))
            CategoryDeckSetting() // delete or comment out this line to hide the setting
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = MindVaultColors.Surface,
            title = { Text("Sign out?") },
            text = {
                Text(
                    "We'll upload your latest changes first. You'll go back to using MindVault as a guest, and this " +
                            "account's items will be removed from this device. Sign in again to get them back. " +
                            "If you're offline, the items stay on this device.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.onSignOutConfirmed()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.TextSecondary),
                ) { Text("Cancel") }
            },
        )
    }

    if (showDeleteDialog) {
        val canDelete = deleteConfirmText.trim() == DELETE_WORD && !state.isDeleting
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmText = ""
            },
            containerColor = MindVaultColors.Surface,
            title = { Text("Delete account?") },
            text = {
                Column {
                    Text(
                        "This permanently deletes your MindVault account, any data stored in the cloud for it, " +
                                "and this account's items on this device. This can't be undone." +
                                "\n\nItems you saved as a guest are not affected.",
                    )
                    Spacer(Modifier.height(MindVaultSpacing.space4))
                    MindVaultTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = "Type $DELETE_WORD to confirm",
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canDelete,
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmText = ""
                        viewModel.onDeleteAccountConfirmed()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete account") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmText = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.TextSecondary),
                ) { Text("Cancel") }
            },
        )
    }
}

private const val DELETE_WORD = "DELETE"

@Composable
private fun AccountCard(
    state: ProfileUiState,
    onSignInClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    MindVaultCard(modifier = Modifier.fillMaxWidth()) {
        when (val account = state.account) {
            AccountState.Hidden -> Unit
            AccountState.Guest -> {
                if (state.accountDeleted) {
                    Text(
                        "Your account was deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                }
                if (state.signOutKeptLocalCopy) {
                    Text(
                        "Some changes hadn't been uploaded, so this account's items were kept on this device. Sign in again to finish syncing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                }
                Text("Using MindVault as a guest", style = MaterialTheme.typography.titleMedium, color = MindVaultColors.TextPrimary)
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    "Everything works without an account. Create one if you'd like to sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space4))
                PrimaryButton(text = "Sign in or create account", onClick = onSignInClick, modifier = Modifier.fillMaxWidth())
            }
            is AccountState.SignedIn -> {
                Text("Signed in", style = MaterialTheme.typography.titleMedium, color = MindVaultColors.TextPrimary)
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    account.email ?: "Your account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindVaultColors.TextSecondary,
                )
                if (account.needsRefresh) {
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                    Text(
                        "We couldn't refresh your sign-in just now. We'll try again when you're online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
                SyncStatusBlock(state.sync, onSyncNowClick)
                if (state.signOutFailed) {
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                    Text(
                        "Couldn't sign out. Please try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(MindVaultSpacing.space4))
                if (state.deleteFailed) {
                    Text(
                        "Couldn't delete your account. Check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(MindVaultSpacing.space2))
                }
                SecondaryButton(
                    text = if (state.isSigningOut) "Signing out…" else "Sign out",
                    onClick = onSignOutClick,
                    enabled = !state.isSigningOut && !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))
                TextButton(
                    onClick = onDeleteClick,
                    enabled = !state.isSigningOut && !state.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.isDeleting) "Deleting account…" else "Delete account") }
            }
            AccountState.SessionExpired -> {
                Text("Your sign-in ended", style = MaterialTheme.typography.titleMedium, color = MindVaultColors.TextPrimary)
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    "For your security, please sign in again. Your saved items are still on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space4))
                PrimaryButton(text = "Sign in again", onClick = onSignInClick, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = MindVaultSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // the whole row handles the click
            colors = RadioButtonDefaults.colors(
                selectedColor = MindVaultColors.Forest,
                unselectedColor = MindVaultColors.TextSecondary,
            ),
        )
        Spacer(Modifier.width(MindVaultSpacing.space3))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MindVaultColors.TextPrimary)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
    }
}
@Composable
private fun SyncStatusBlock(status: SyncStatus, onSyncNowClick: () -> Unit) {
    if (status == SyncStatus.Inactive) return
    Spacer(Modifier.height(MindVaultSpacing.space3))
    Text(
        text = syncStatusText(status),
        style = MaterialTheme.typography.bodyMedium,
        color = if (status is SyncStatus.NeedsAttention) MaterialTheme.colorScheme.error else MindVaultColors.TextSecondary,
    )
    TextButton(
        onClick = onSyncNowClick,
        enabled = status != SyncStatus.Syncing,
        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
    ) { Text("Sync now") }
}

private fun pendingText(n: Int) = if (n == 1) "1 change is waiting to upload." else "$n changes are waiting to upload."

private fun syncStatusText(status: SyncStatus): String = when (status) {
    SyncStatus.Inactive -> ""
    SyncStatus.Syncing -> "Syncing…"
    SyncStatus.NotSyncedYet -> "Not synced yet"
    is SyncStatus.Waiting -> pendingText(status.pendingChanges)
    is SyncStatus.Synced -> {
        val now = System.currentTimeMillis()
        if (now - status.at < 60_000) "Synced just now"
        else "Synced " + DateUtils.getRelativeTimeSpanString(status.at, now, DateUtils.MINUTE_IN_MILLIS)
    }
    is SyncStatus.NeedsAttention -> {
        val base = when (status.problem) {
            SyncProblem.OFFLINE -> "Couldn't reach the server. We'll retry when you're online."
            SyncProblem.SIGN_IN_NEEDED -> "Sign in again to keep syncing."
            SyncProblem.SERVER -> "The server had a problem. We'll try again."
            SyncProblem.REJECTED -> "Some changes couldn't be uploaded."
            SyncProblem.UNKNOWN -> "Something went wrong while syncing."
        }
        if (status.pendingChanges > 0) "$base ${pendingText(status.pendingChanges)}" else base
    }
}
