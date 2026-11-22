package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.DeckDue
import com.mindvault.domain.model.DeckSummary
import com.mindvault.domain.model.HomeSummary
import com.mindvault.domain.repository.FlashcardRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Home dashboard data from the same saved items and decks used everywhere else. Re-emits on any change. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveHomeSummaryUseCase @Inject constructor(
    private val observeVaultItems: ObserveVaultItemsUseCase,
    private val observeDeckSummaries: ObserveDeckSummariesUseCase,
    private val flashcardRepository: FlashcardRepository,
    private val observeNewCardsPerDay: ObserveNewCardsPerDayUseCase,
) {
    operator fun invoke(): Flow<HomeSummary> = combine(
        observeVaultItems(),
        decksWithCompletedToday(),
        dueDecks(),
    ) { entries, (decks, completedToday), due ->
        HomeSummaryBuilder.build(entries, decks, completedToday, due)
    }

    private fun decksWithCompletedToday(): Flow<Pair<List<DeckSummary>, Int>> =
        observeDeckSummaries().flatMapLatest { decks ->
            val complete = decks.filter { it.isComplete }
            if (complete.isEmpty()) {
                flowOf(decks to 0)
            } else {
                combine(
                    complete.map { summary ->
                        flashcardRepository.observeByDeck(summary.deck.id).map { cards ->
                            HomeSummaryBuilder.isCompletedToday(cards, startOfTodayMillis())
                        }
                    },
                ) { flags -> decks to flags.count { it } }
            }
        }

    /** Decks that have cards to review today (due reviews plus new cards still allowed), in deck order. */
    private fun dueDecks(): Flow<List<DeckDue>> =
        observeDeckSummaries().flatMapLatest { decks ->
            if (decks.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    decks.map { summary ->
                        combine(
                            flashcardRepository.observeByDeck(summary.deck.id),
                            observeNewCardsPerDay(),
                        ) { cards, limit ->
                            DeckDue(summary.deck, ReviewQueueBuilder.counts(cards, limit, nowMillis()).dailyTotal)
                        }
                    },
                ) { due -> due.filter { it.cardsToReview > 0 } }
            }
        }

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}