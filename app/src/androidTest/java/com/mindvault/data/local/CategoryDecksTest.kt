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

/** Category decks: created with the category, items join them, they follow renames and survive deletions. */
@RunWith(AndroidJUnit4::class)
class CategoryDecksTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val guestName = "category-decks-test-guest.db"
    private val prefsName = "category-decks-test-owner"
    private val accountsDir = File(context.cacheDir, "category-decks-test-users")

    private lateinit var active: ActiveDatabase
    private lateinit var knowledge: KnowledgeRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var decks: DeckRepositoryImpl

    @Before
    fun setUp() {
        context.deleteDatabase(guestName)
        accountsDir.deleteRecursively()
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit()
        active = ActiveDatabase(context, ActiveOwnerStore(context, prefsName), guestName, accountsDir)
        knowledge = KnowledgeRepositoryImpl(active)
        categories = CategoryRepositoryImpl(active)
        decks = DeckRepositoryImpl(active)
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

    private suspend fun deckOf(categoryId: String) = active.current.deckDao().findByCategory(categoryId)

    private suspend fun hasCard(itemId: String, deckId: String) =
        active.current.flashcardDao().findByItemAndDeck(itemId, deckId) != null

    private suspend fun outbox() = active.current.syncOutboxDao().getAll()

    @Test
    fun creatingACategoryCreatesItsDeck() = runBlocking {
        val category = categories.create("Science")

        val deck = deckOf(category.id)
        assertNotNull(deck)
        assertEquals("Science", deck!!.name)
        assertEquals(false, deck.isDefault)
        assertTrue(outbox().any { it.entityType == SyncEntityType.DECK && it.entityId == deck.id && it.operation == SyncOperation.UPSERT })
    }

    @Test
    fun aNameClashGivesANumberedDeck() = runBlocking {
        decks.create("Science")
        val category = categories.create("Science")
        assertEquals("Science (2)", deckOf(category.id)!!.name)
    }

    @Test
    fun anItemSavedInACategoryJoinsItsDeckAndTheDefaultDeck() = runBlocking {
        val category = categories.create("Science")
        val item = knowledge.create(draft("Entropy", category.id))

        assertTrue(hasCard(item.id, deckOf(category.id)!!.id))
        assertTrue(hasCard(item.id, Deck.DEFAULT_DECK_ID))
    }

    @Test
    fun movingAnItemIntoACategoryAddsItToThatDeck() = runBlocking {
        val category = categories.create("Science")
        val item = knowledge.create(draft("Entropy"))
        val deckId = deckOf(category.id)!!.id
        assertTrue(!hasCard(item.id, deckId))

        knowledge.update(item.copy(categoryId = category.id))

        assertTrue(hasCard(item.id, deckId))
    }

    @Test
    fun renamingACategoryRenamesItsDeckUnlessTheUserRenamedTheDeck() = runBlocking {
        val first = categories.create("Science")
        categories.rename(first.id, "Natural science")
        assertEquals("Natural science", deckOf(first.id)!!.name)

        val second = categories.create("Languages")
        val deck = deckOf(second.id)!!
        decks.rename(deck.id, "My words")
        categories.rename(second.id, "Foreign languages")
        assertEquals("My words", deckOf(second.id)!!.name)
    }

    @Test
    fun deletingACategoryKeepsItsDeckAsAnOrdinaryDeck() = runBlocking {
        val category = categories.create("Science")
        val item = knowledge.create(draft("Entropy", category.id))
        val deckId = deckOf(category.id)!!.id

        categories.delete(category.id)

        val deck = active.current.deckDao().getById(deckId)
        assertNotNull(deck)
        assertNull(deck!!.categoryId)
        assertTrue(hasCard(item.id, deckId))
    }

    @Test
    fun aDeckTheUserDeletedIsNotRecreatedWhenMoreItemsAreSaved() = runBlocking {
        val category = categories.create("Science")
        decks.delete(deckOf(category.id)!!.id)

        knowledge.create(draft("Entropy", category.id))

        assertNull(deckOf(category.id))
    }

    @Test
    fun ensureCategoryDeckRecreatesAMissingDeckWithAllItsItems() = runBlocking {
        val category = categories.create("Science")
        val one = knowledge.create(draft("Entropy", category.id))
        val two = knowledge.create(draft("Enthalpy", category.id))
        knowledge.create(draft("Other"))
        decks.delete(deckOf(category.id)!!.id)

        val deck = decks.ensureCategoryDeck(category.id)

        assertNotNull(deck)
        assertEquals("Science", deck!!.name)
        assertEquals(category.id, deck.categoryId)
        assertTrue(hasCard(one.id, deck.id))
        assertTrue(hasCard(two.id, deck.id))
        // Asking again returns the same deck.
        assertEquals(deck.id, decks.ensureCategoryDeck(category.id)!!.id)
        assertNull(decks.ensureCategoryDeck("missing"))
    }
}