package com.mindvault.domain.usecase

import com.mindvault.domain.model.AddToDeckResult
import com.mindvault.domain.model.Flashcard
import com.mindvault.domain.model.FlashcardDetail
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType
import com.mindvault.domain.model.ReviewState
import com.mindvault.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDeckReviewCountsUseCaseTest {

    private class FakeFlashcardRepository(private val cards: List<FlashcardDetail>) : FlashcardRepository {
        override fun observeByDeck(deckId: String): Flow<List<FlashcardDetail>> = flowOf(cards)
        override fun observeDue(deckId: String, now: Long): Flow<List<FlashcardDetail>> = emptyFlow()
        override fun observeCountByDeck(deckId: String): Flow<Int> = emptyFlow()
        override fun observeUnreviewedCountByDeck(deckId: String): Flow<Int> = emptyFlow()
        override fun observeDueCountByDeck(deckId: String, now: Long): Flow<Int> = emptyFlow()
        override fun observeDeckIdsContaining(knowledgeItemId: String): Flow<List<String>> = emptyFlow()
        override suspend fun addToDeck(knowledgeItemId: String, deckId: String): AddToDeckResult = error("unused")
        override suspend fun removeFromDeck(flashcardId: String) = error("unused")
        override suspend fun getReviewState(flashcardId: String): ReviewState? = null
        override suspend fun saveReviewState(state: ReviewState) = error("unused")
    }

    private fun newCard(id: String, createdAt: Long) = FlashcardDetail(
        flashcard = Flashcard(id = id, knowledgeItemId = "k$id", deckId = "deck", createdAt = createdAt),
        knowledgeItem = KnowledgeItem(
            id = "k$id", title = "t$id", type = KnowledgeType.WORD, definition = "d",
            personalNotes = "", example = null, categoryId = null, createdAt = 0L, updatedAt = 0L,
        ),
        reviewState = ReviewState.initial(id, 0L),
    )

    @Test
    fun newCardCountFollowsTheDailyLimit() = runBlocking {
        val cards = (1..5).map { newCard("c$it", it.toLong()) }
        val settings = FakeSettingsRepository()
        val useCase = ObserveDeckReviewCountsUseCase(
            FakeFlashcardRepository(cards),
            ObserveNewCardsPerDayUseCase(settings),
        )

        val byDefault = useCase("deck").first()
        assertEquals(5, byDefault.newCards) // default limit is 20
        assertEquals(0, byDefault.dueReviews)
        assertEquals(0, byDefault.difficult)

        settings.newCardsPerDay.value = 3
        assertEquals(3, useCase("deck").first().newCards)
    }
}