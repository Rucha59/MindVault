package com.mindvault.domain.usecase

import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.DeckRemovalPreference
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The category's deck if [itemId] has a card in it (so leaving the category raises the question), else null. */
class FindDeckToLeaveUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(itemId: String, categoryId: String): Deck? =
        deckRepository.findCategoryDeckHoldingItem(itemId, categoryId)
}

class ObserveDeckRemovalPreferenceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<DeckRemovalPreference> = repository.observeDeckRemovalPreference()
}

class SetDeckRemovalPreferenceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(preference: DeckRemovalPreference) =
        repository.setDeckRemovalPreference(preference)
}