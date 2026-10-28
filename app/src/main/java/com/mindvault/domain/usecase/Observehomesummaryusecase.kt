package com.mindvault.domain.usecase

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
) {
    operator fun invoke(): Flow<HomeSummary> = combine(
        observeVaultItems(),
        decksWithCompletedToday(),
    ) { entries, (decks, completedToday) ->
        HomeSummaryBuilder.build(entries, decks, completedToday)
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

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}