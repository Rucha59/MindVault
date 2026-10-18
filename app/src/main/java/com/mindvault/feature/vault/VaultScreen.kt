package com.mindvault.feature.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar

/**
 * Library-first Vault shell. Search, filter chips and sorting are omitted until saved items exist
 * and those features work; only the empty state and Add action are shown.
 */
@Composable
fun VaultScreen(
    onAddConceptClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "My Vault")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            EmptyState(
                icon = Icons.Outlined.Bookmarks,
                title = "Your vault is empty",
                message = "Words and concepts you save will appear here.",
                actionLabel = "Add a concept",
                onAction = onAddConceptClick,
            )
        }
    }
}