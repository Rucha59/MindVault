package com.mindvault.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.core.theme.MindVaultColors
import com.mindvault.core.theme.MindVaultSpacing
import com.mindvault.core.theme.ShapePill
import com.mindvault.core.theme.ShapeSmall
import com.mindvault.core.ui.components.EmptyState
import com.mindvault.core.ui.components.MindVaultCard
import com.mindvault.core.ui.components.MindVaultTopBar
import com.mindvault.core.ui.components.SectionHeader
import com.mindvault.domain.model.DayActivity
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ProgressStats
import com.mindvault.domain.model.ReviewRating
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Stats reached from the Profile "Your progress" card. Every number comes from saved data. */
@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        MindVaultTopBar(title = "Stats", onBackClick = onBackClick)
        val current = stats
        when {
            current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MindVaultColors.Forest)
            }
            current.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.BarChart,
                    title = "No stats yet",
                    message = "Save a word or concept and review a few flashcards to see your progress here.",
                )
            }
            else -> StatsContent(current)
        }
    }
}

@Composable
private fun StatsContent(stats: ProgressStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MindVaultSpacing.screenHorizontal),
    ) {
        SectionHeader("Overview")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            StatTile(stats.totalItems.toString(), "Saved", Modifier.weight(1f))
            StatTile(stats.flashcardCount.toString(), "Flashcards", Modifier.weight(1f))
        }
        Spacer(Modifier.height(MindVaultSpacing.space3))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space3)) {
            StatTile(stats.dueReviews.toString(), "Due today", Modifier.weight(1f))
            StatTile(stats.streakDays.toString(), "Day streak", Modifier.weight(1f))
        }

        Spacer(Modifier.height(MindVaultSpacing.space6))
        SectionHeader("Activity")
        MindVaultCard(Modifier.fillMaxWidth()) {
            Text(
                text = "Reviewed today: ${stats.reviewedToday}",
                style = MaterialTheme.typography.titleMedium,
                color = MindVaultColors.TextPrimary,
            )
            Spacer(Modifier.height(MindVaultSpacing.space3))
            ActivityChart(stats.activity)
            Spacer(Modifier.height(MindVaultSpacing.space3))
            Text(
                text = if (stats.hasReviewHistory) {
                    "A day counts toward your streak when you answer at least one card. A card you rate Again " +
                            "counts once you answer it again. Practice doesn't count."
                } else {
                    "Start reviewing to see your activity here. History begins with your first review."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(MindVaultSpacing.space6))
        SectionHeader("Flashcards")
        MindVaultCard(Modifier.fillMaxWidth()) {
            if (stats.flashcardCount == 0) {
                Text(
                    text = "No flashcards yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindVaultColors.TextSecondary,
                )
            } else {
                Text(
                    text = "Card progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))
                StateBar(stats.newCards, stats.learning, stats.mature)
                Spacer(Modifier.height(MindVaultSpacing.space2))
                LegendRow(MindVaultColors.AccentBlue.foreground, "New", stats.newCards)
                LegendRow(MindVaultColors.AccentGold.foreground, "Learning (under 21 days)", stats.learning)
                LegendRow(MindVaultColors.AccentMint.foreground, "Mature (21 days or more)", stats.mature)
                Spacer(Modifier.height(MindVaultSpacing.space3))
                InfoRow("Difficult cards", stats.difficult.toString())
                InfoRow("Decks", stats.deckCount.toString())
            }
        }

        if (stats.hasReviewHistory) {
            Spacer(Modifier.height(MindVaultSpacing.space6))
            SectionHeader("How reviews went")
            MindVaultCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "${stats.totalReviews} answers in total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space2))
                stats.ratingCounts.forEach { entry ->
                    val share = entry.count.toFloat() / stats.totalReviews
                    BarRow(
                        label = ratingLabel(entry.rating),
                        fraction = share,
                        valueText = "${entry.count} · ${(share * 100).roundToInt()}%",
                    )
                }
            }
        }

        Spacer(Modifier.height(MindVaultSpacing.space6))
        SectionHeader("Library")
        MindVaultCard(Modifier.fillMaxWidth()) {
            val maxType = (stats.typeCounts.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
            stats.typeCounts.forEach { entry ->
                BarRow(
                    label = typeLabel(entry.type),
                    fraction = entry.count.toFloat() / maxType,
                    valueText = entry.count.toString(),
                )
            }
            if (stats.topCategories.isNotEmpty()) {
                Spacer(Modifier.height(MindVaultSpacing.space3))
                Text(
                    text = "Top categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindVaultColors.TextPrimary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                val maxCategory = (stats.topCategories.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
                stats.topCategories.forEach { entry ->
                    BarRow(
                        label = entry.name,
                        fraction = entry.count.toFloat() / maxCategory,
                        valueText = entry.count.toString(),
                    )
                }
            }
            Spacer(Modifier.height(MindVaultSpacing.space3))
            InfoRow("Categories", stats.categoryCount.toString())
            InfoRow("Added in the last 7 days", stats.addedLast7Days.toString())
        }
        Spacer(Modifier.height(MindVaultSpacing.space8))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    MindVaultCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MindVaultColors.Forest)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MindVaultSpacing.space1),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextPrimary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
    }
}

/** A labelled horizontal bar. The number is always written out, so meaning never rests on the bar alone. */
@Composable
private fun BarRow(label: String, fraction: Float, valueText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MindVaultSpacing.space1),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MindVaultColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(MindVaultSpacing.space2))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
        }
        Spacer(Modifier.height(MindVaultSpacing.space1))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ShapePill)
                .background(MindVaultColors.SurfaceVariant),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MindVaultColors.Forest),
                )
            }
        }
    }
}

/** New / Learning / Mature as one stacked bar; the legend below spells out every number. */
@Composable
private fun StateBar(newCards: Int, learning: Int, mature: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(ShapePill)
            .background(MindVaultColors.SurfaceVariant),
    ) {
        if (newCards > 0) {
            Box(
                Modifier
                    .weight(newCards.toFloat())
                    .fillMaxHeight()
                    .background(MindVaultColors.AccentBlue.foreground),
            )
        }
        if (learning > 0) {
            Box(
                Modifier
                    .weight(learning.toFloat())
                    .fillMaxHeight()
                    .background(MindVaultColors.AccentGold.foreground),
            )
        }
        if (mature > 0) {
            Box(
                Modifier
                    .weight(mature.toFloat())
                    .fillMaxHeight()
                    .background(MindVaultColors.AccentMint.foreground),
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MindVaultSpacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(MindVaultSpacing.space2))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MindVaultColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = MindVaultColors.TextSecondary)
    }
}

/** Seven small bars, oldest to today. Each bar has its count above it and its day below it. */
@Composable
private fun ActivityChart(days: List<DayActivity>) {
    val maxReviews = (days.maxOfOrNull { it.reviews } ?: 0).coerceAtLeast(1)
    val barMax = 64.dp
    val description = days.mapIndexed { index, day -> "${dayLabel(day, index == days.lastIndex)} ${day.reviews}" }
        .joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Reviews in the last 7 days: $description" },
        horizontalArrangement = Arrangement.spacedBy(MindVaultSpacing.space2),
    ) {
        days.forEachIndexed { index, day ->
            val barHeight = if (day.reviews == 0) 4.dp else maxOf(6.dp, barMax * (day.reviews.toFloat() / maxReviews))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(barMax - barHeight)) // aligns every bar to the same baseline
                Text(
                    text = day.reviews.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MindVaultColors.TextSecondary,
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(ShapeSmall)
                        .background(if (day.reviews > 0) MindVaultColors.Forest else MindVaultColors.SurfaceVariant),
                )
                Spacer(Modifier.height(MindVaultSpacing.space1))
                Text(
                    text = dayLabel(day, index == days.lastIndex),
                    style = MaterialTheme.typography.labelMedium,
                    color = MindVaultColors.TextPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun dayLabel(day: DayActivity, isToday: Boolean): String =
    if (isToday) "Today" else day.date.dayOfWeek.getDisplayName(DayTextStyle.SHORT, Locale.getDefault())

private fun ratingLabel(rating: ReviewRating): String = when (rating) {
    ReviewRating.AGAIN -> "Again"
    ReviewRating.HARD -> "Hard"
    ReviewRating.GOOD -> "Good"
    ReviewRating.EASY -> "Easy"
}

private fun typeLabel(type: KnowledgeType): String = when (type) {
    KnowledgeType.WORD -> "Words"
    KnowledgeType.CONCEPT -> "Concepts"
    KnowledgeType.OTHER -> "Other"
}