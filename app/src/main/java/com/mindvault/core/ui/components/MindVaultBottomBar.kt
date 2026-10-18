package com.mindvault.core.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindvault.core.navigation.MindVaultDestination
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill

private data class BottomBarTab(
    val destination: MindVaultDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

// Fixed order from DESIGN_SYSTEM.md 6.1: Search, Vault, Add, Flashcards, Home.
private val LeadingTabs = listOf(
    BottomBarTab(MindVaultDestination.Search, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomBarTab(MindVaultDestination.Vault, "Vault", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
)
private val TrailingTabs = listOf(
    BottomBarTab(MindVaultDestination.Flashcards, "Flashcards", Icons.Filled.Style, Icons.Outlined.Style),
    BottomBarTab(MindVaultDestination.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
)

/** The one shared bottom navigation: four destinations plus the centered Add action. */
@Composable
fun MindVaultBottomBar(
    selectedRoute: String?,
    onDestinationClick: (MindVaultDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MindVaultColors.Surface) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MindVaultColors.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeadingTabs.forEach { tab ->
                    BottomBarItem(
                        tab = tab,
                        selected = tab.destination.route == selectedRoute,
                        onClick = { onDestinationClick(tab.destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
                AddButton(onClick = onAddClick, modifier = Modifier.weight(1f))
                TrailingTabs.forEach { tab ->
                    BottomBarItem(
                        tab = tab,
                        selected = tab.destination.route == selectedRoute,
                        onClick = { onDestinationClick(tab.destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomBarTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) MindVaultColors.Forest else MindVaultColors.TextSecondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Selected state: filled icon + pale sage pill + Forest color (not color alone).
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 28.dp)
                .clip(ShapePill)
                .background(if (selected) MindVaultColors.SageLight else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = null, // the label below names the tab
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(if (pressed) MindVaultColors.ForestPressed else MindVaultColors.Forest)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add",
                tint = MindVaultColors.TextOnPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}