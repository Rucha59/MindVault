package com.mindvault.domain.usecase

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.NotificationSettings
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.repository.KnowledgeRepository
import com.mindvault.domain.repository.NotificationSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveNotificationSettingsUseCase @Inject constructor(
    private val repository: NotificationSettingsRepository,
) {
    operator fun invoke(): Flow<NotificationSettings> = repository.observe()
}

class UpdateNotificationSettingsUseCase @Inject constructor(
    private val repository: NotificationSettingsRepository,
) {
    suspend fun setDueReminders(enabled: Boolean) = repository.setDueRemindersEnabled(enabled)
    suspend fun setReminderTime(minuteOfDay: Int) = repository.setReminderMinuteOfDay(minuteOfDay)
    suspend fun setNudges(enabled: Boolean) = repository.setNudgesEnabled(enabled)
}

/**
 * Cards to review today across all decks except the default deck (which is practice only): due reviews plus the new
 * cards still allowed. Same rule as Home's "Due for review", through [ReviewQueueBuilder].
 */
class CountDueCardsUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val observeNewCardsPerDay: ObserveNewCardsPerDayUseCase,
) {
    suspend operator fun invoke(now: Long = nowMillis()): Int {
        val limit = observeNewCardsPerDay().first()
        return deckRepository.observeAll().first()
            .filter { it.id != Deck.DEFAULT_DECK_ID }
            .sumOf { deck ->
                ReviewQueueBuilder.counts(flashcardRepository.observeByDeck(deck.id).first(), limit, now).dailyTotal
            }
    }
}

/** When the newest item was saved, or null for an empty Vault. */
class GetLastSavedAtUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
) {
    suspend operator fun invoke(): Long? = knowledgeRepository.observeAll().first().maxOfOrNull { it.createdAt }
}