package com.mindvault.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultTopBar

/**
 * Profile shell, reached from the Home top bar. The design system forbids nonfunctional
 * settings, so no toggles are shown until a setting actually works.
 */
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Profile", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MindVaultSpacing.screenHorizontal),
        ) {
            EmptyState(
                icon = Icons.Outlined.Settings,
                title = "No settings yet",
                message = "Preferences such as theme and search suggestions will appear here once they work.",
            )
        }
    }
}