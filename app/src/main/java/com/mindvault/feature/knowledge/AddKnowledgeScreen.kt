package com.mindvault.feature.knowledge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar

/** PLACEHOLDER: the add/edit form (title, definition, notes, category) is built in a later phase. */
@Composable
fun AddKnowledgeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Add a concept", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            EmptyState(
                icon = Icons.Outlined.Edit,
                title = "Form coming next",
                message = "Placeholder screen. The form for title, definition, notes and category is built in the next phase.",
            )
        }
    }
}