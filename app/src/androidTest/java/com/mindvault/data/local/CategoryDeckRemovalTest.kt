package com.mindvault.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindvault.data.local.database.ActiveDatabase
import com.mindvault.data.local.database.ActiveOwnerStore
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.repository.CategoryRepositoryImpl
import com.mindvault.data.repository.DeckRepositoryImpl
import com.mindvault.data.repository.FlashcardRepositoryImpl
import com.mindvault.data.repository.KnowledgeRepositoryImpl
import com.mindvault.domain.model.Deck
import com.mindvault.domain.model.KnowledgeItemDraft
import com.mindvault.domain.model.KnowledgeType
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** An item leaving a category: its card in that category's deck is removed only when asked, atomically with the save. */
@RunWith(AndroidJUnit4::class)
class CategoryDeckRemovalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val guestName = "category-deck-removal-test-guest.db"
    private val prefsName = "category-deck-removal-test-owner"
    private val accountsDir = File(context.cacheDir, "category-deck-removal-test-users")

    private lateinit var active: ActiveDatabase
    private lateinit var knowledge: KnowledgeRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var decks: DeckRepositoryImpl
    private lateinit var flashcards: FlashcardRepositoryImpl

    @Before
    fun setUp() {
        context.deleteDatabase(guestName)
        accountsDir.deleteRecursively()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
        active = ActiveDatabase(context, ActiveOwnerStore(context, prefsName), guestName, accountsDir)
        knowledge = KnowledgeRepositoryImpl(active)
        categories = CategoryRepositoryImpl(active)
        decks = DeckRepositoryImpl(active)
        flashcards = FlashcardRepositoryImpl(active)
    }

    @After
    fun tearDown() {
        active.current.close()
        context.deleteDatabase(guestName)
        accountsDir.deleteRecursively()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun draft(title: String, categoryId: String? = null) =
        KnowledgeItemDraft(title = title, type = KnowledgeType.WORD, definition = "def", categoryId = categoryId)

    private suspend fun deckOf(categoryId: String) = active.current.deckDao().findByCategory(categoryId)!!

    private suspend fun cardIn(itemId: String, deckId: String) =
        active.current.flashcardDao().findByItemAndDeck(itemId, deckId)

    @Test
    fun leavingACategoryWithRemoveRemovesOnlyThatDecksCard() = runBlocking {
        val science = categories.create("Science")
        val item = knowledge.create(draft("Entropy", science.id))
        val scienceDeck = deckOf(science.id)
        val cardId = cardIn(item.id, scienceDeck.id)!!.id

        knowledge.update(item.copy(categoryId = null), removeFromOldCategoryDeck = true)

        assertNull(cardIn(item.id, scienceDeck.id))
        assertNotNull("the default deck keeps its card", cardIn(item.id, Deck.DEFAULT_DECK_ID))
        val entry = active.current.syncOutboxDao().getAll().first { it.entityType == SyncEntityType.FLASHCARD && it.entityId == cardId }
        assertEquals(SyncOperation.DELETE, entry.operation)
    }

    @Test
    fun leavingACategoryWithoutRemoveKeepsTheCard() = runBlocking {
        val science = categories.create("Science")
        val item = knowledge.create(draft("Entropy", science.id))
        val scienceDeck = deckOf(science.id)

        knowledge.update(item.copy(categoryId = null), removeFromOldCategoryDeck = false)

        assertNotNull(cardIn(item.id, scienceDeck.id))
    }

    @Test
    fun movingBetweenCategoriesAddsToTheNewDeckAndRemovesFromTheOldOneWhenAsked() = runBlocking {
        val science = categories.create("Science")
        val languages = categories.create("Languages")
        val item = knowledge.create(draft("Entropy", science.id))

        knowledge.update(item.copy(categoryId = languages.id), removeFromOldCategoryDeck = true)

        assertNull(cardIn(item.id, deckOf(science.id).id))
        assertNotNull(cardIn(item.id, deckOf(languages.id).id))
    }

    @Test
    fun theRemoveOptionDoesNothingWhenTheCategoryDoesNotChange() = runBlocking {
        val science = categories.create("Science")
        val item = knowledge.create(draft("Entropy", science.id))

        knowledge.update(item.copy(definition = "new"), removeFromOldCategoryDeck = true)

        assertNotNull(cardIn(item.id, deckOf(science.id).id))
    }

    @Test
    fun aCardInAnotherDeckIsNeverTouched() = runBlocking {
        val science = categories.create("Science")
        val item = knowledge.create(draft("Entropy", science.id))
        val mine = decks.create("Mine")
        flashcards.addToDeck(item.id, mine.id)

        knowledge.update(item.copy(categoryId = null), removeFromOldCategoryDeck = true)

        assertNotNull(cardIn(item.id, mine.id))
    }

    @Test
    fun findCategoryDeckHoldingItemReturnsTheDeckOnlyWhileTheCardExists() = runBlocking {
        val science = categories.create("Science")
        val item = knowledge.create(draft("Entropy", science.id))
        val scienceDeck = deckOf(science.id)

        assertEquals(scienceDeck.id, decks.findCategoryDeckHoldingItem(item.id, science.id)?.id)
        assertEquals(scienceDeck.id, decks.findCategoryDeck(science.id)?.id)

        flashcards.removeFromDeck(cardIn(item.id, scienceDeck.id)!!.id)

        assertNull(decks.findCategoryDeckHoldingItem(item.id, science.id))
        assertNull(decks.findCategoryDeck("missing"))
    }
}