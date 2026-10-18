package com.mindvault.feature.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar

/**
 * Learning-first Flashcards shell. Decks, review and statistics are not built yet, so no tabs,
 * progress or card are shown.
 */
@Composable
fun FlashcardsScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Flashcards")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            EmptyState(
                icon = Icons.Outlined.Style,
                title = "No cards yet",
                message = "Flashcards made from your saved concepts will appear here for review.",
            )
        }
    }
}