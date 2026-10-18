package com.mindvault.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultSearchField
import com.mindvault.core.ui.components.MindVaultTopBar

/**
 * Lookup-first Search shell. External search is not built yet, so the field is disabled and the
 * screen says so plainly instead of showing fabricated results.
 */
@Composable
fun SearchScreen(
    onAddConceptClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Search")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            MindVaultSearchField(
                value = "",
                onValueChange = {},
                placeholder = "Search words and concepts",
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MindVaultSpacing.space6))
            EmptyState(
                icon = Icons.Outlined.Search,
                title = "Search isn\u2019t available yet",
                message = "Looking up definitions online hasn\u2019t been set up. You can still add your own concepts.",
                actionLabel = "Add a concept",
                onAction = onAddConceptClick,
            )
        }
    }
}