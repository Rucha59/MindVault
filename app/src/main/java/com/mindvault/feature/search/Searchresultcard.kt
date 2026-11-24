package com.mindvault.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapeLarge
import com.mindvault.core.theme.ShapeSmall
import com.mindvault.core.ui.components.PrimaryButton
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchResultKind

/**
 * One Search result. Compact by default (title, a Word or Concept tag, the first definition or the opening of the
 * summary, the source); "Show more" expands it to every definition and example, or the full summary.
 * Only fields that exist are shown. Save, Edit first and "Already in your Vault" work as before.
 */
@Composable
internal fun SearchResultCard(
    item: SearchResultUi,
    isSaving: Boolean,
    onToggleExpanded: () -> Unit,
    onRetrySummary: () -> Unit,
    onSave: () -> Unit,
    onEditFirst: () -> Unit,
    onViewSaved: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = item.result
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeLarge,
        color = MindVaultColors.Surface,
        border = BorderStroke(1.dp, MindVaultColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(MindVaultSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3),
        ) {
            CardHeader(result)

            if (item.isExpanded) {
                ExpandedBody(item, onRetrySummary)
            } else {
                CollapsedBody(result)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sourceLabel(result),
                    style = MaterialTheme.typography.bodySmall,
                    color = MindVaultColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (hasMoreToShow(result)) {
                    TextButton(
                        onClick = onToggleExpanded,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text(if (item.isExpanded) "Show less" else "Show more") }
                }
            }

            if (item.isExpanded && result.sourceUrl != null) {
                TextButton(
                    onClick = { runCatching { uriHandler.openUri(result.sourceUrl) } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                ) { Text("Open on ${result.sourceName}") }
            }

            val savedId = item.savedItemId
            if (savedId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MindVaultColors.Forest,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(MindVaultSpacing.space2))
                        Text(
                            text = "Already in your Vault",
                            style = MaterialTheme.typography.labelLarge,
                            color = MindVaultColors.TextPrimary,
                        )
                    }
                    TextButton(
                        onClick = { onViewSaved(savedId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("View") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onEditFirst,
                        enabled = !isSaving && !item.isLoadingSummary,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("Edit first") }
                    Spacer(Modifier.width(MindVaultSpacing.space2))
                    PrimaryButton(
                        text = if (isSaving) "Saving\u2026" else "Save",
                        onClick = onSave,
                        enabled = !isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardHeader(result: SearchResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleLarge,
                color = MindVaultColors.TextPrimary,
            )
            result.pronunciation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            }
        }
        // The kind is shown as text, so it never relies on color alone.
        Surface(shape = ShapeSmall, color = MindVaultColors.SurfaceVariant) {
            Text(
                text = if (result.kind == SearchResultKind.CONCEPT) "Concept" else "Word",
                style = MaterialTheme.typography.labelMedium,
                color = MindVaultColors.TextSecondary,
                modifier = Modifier.padding(
                    horizontal = MindVaultSpacing.space2,
                    vertical = MindVaultSpacing.space1,
                ),
            )
        }
    }
}

@Composable
private fun CollapsedBody(result: SearchResult) {
    when (result.kind) {
        SearchResultKind.WORD -> {
            val meaning = result.meanings.firstOrNull()
            val definition = meaning?.definitions?.firstOrNull() ?: return
            Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
                if (meaning.partOfSpeech.isNotBlank()) PartOfSpeechLabel(meaning.partOfSpeech)
                Text(
                    text = definition.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindVaultColors.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SearchResultKind.CONCEPT -> Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
            result.description?.let { Description(it) }
            result.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindVaultColors.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExpandedBody(item: SearchResultUi, onRetrySummary: () -> Unit) {
    val result = item.result
    when (result.kind) {
        SearchResultKind.WORD -> Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            result.meanings.forEach { meaning ->
                Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space1)) {
                    if (meaning.partOfSpeech.isNotBlank()) PartOfSpeechLabel(meaning.partOfSpeech)
                    meaning.definitions.forEach { definition ->
                        Text(
                            text = definition.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MindVaultColors.TextPrimary,
                        )
                        definition.example?.let { example ->
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MindVaultColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
        SearchResultKind.CONCEPT -> Column(verticalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2)) {
            result.description?.let { Description(it) }
            when {
                !result.needsSummary -> Text(
                    text = result.summary.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindVaultColors.TextPrimary,
                )
                item.summaryFailed && !item.isLoadingSummary -> {
                    Text(
                        text = "Couldn\u2019t load the summary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(
                        onClick = onRetrySummary,
                        colors = ButtonDefaults.textButtonColors(contentColor = MindVaultColors.Forest),
                    ) { Text("Try again") }
                }
                else -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MindVaultColors.Forest,
                    )
                    Text(
                        text = "Loading summary\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindVaultColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PartOfSpeechLabel(partOfSpeech: String) {
    Text(
        text = partOfSpeech.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelMedium,
        color = MindVaultColors.TextSecondary,
    )
}

@Composable
private fun Description(text: String) {
    Text(
        text = text.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.bodyMedium,
        color = MindVaultColors.TextSecondary,
    )
}

private fun sourceLabel(result: SearchResult): String =
    buildString {
        append("Source: ").append(result.sourceName)
        result.license?.takeIf { it.isNotBlank() }?.let { append(" \u00B7 ").append(it) }
    }

/** A word card has more to show when it has further senses or examples; a concept always has its full text. */
private fun hasMoreToShow(result: SearchResult): Boolean =
    when (result.kind) {
        SearchResultKind.CONCEPT -> true
        SearchResultKind.WORD ->
            result.meanings.size > 1 ||
                    result.meanings.any { meaning ->
                        meaning.definitions.size > 1 || meaning.definitions.any { it.example != null }
                    }
    }