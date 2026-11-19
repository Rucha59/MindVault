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
import com.mindvault.domain.error.DuplicateNameException
import com.mindvault.domain.model.AddToDeckResult
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that every write path records its change in sync_outbox in the same transaction. Uses its own database,
 * preferences and folder, so it never touches the app's real data.
 */
@RunWith(AndroidJUnit4::class)
class OutboxRecordingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val guestName = "outbox-test-guest.db"
    private val prefsName = "outbox-test-owner"
    private val accountsDir = File(context.cacheDir, "outbox-test-users")

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

    private suspend fun outbox() = active.current.syncOutboxDao().getAll()

    private fun draft(title: String, categoryId: String? = null) =
        KnowledgeItemDraft(title = title, type = KnowledgeType.WORD, definition = "def", categoryId = categoryId)

    /** Adds the item to the default deck and returns the new card's ID and creation time (read from its outbox entry). */
    private suspend fun addCard(itemId: String): Pair<String, Long> {
        assertTrue(flashcards.addToDeck(itemId, Deck.DEFAULT_DECK_ID) is AddToDeckResult.Added)
        val entry = outbox().first { it.entityType == SyncEntityType.FLASHCARD }
        return entry.entityId to entry.changedAt
    }

    @Test
    fun creatingAnItemRecordsAnUpsert() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        val entry = outbox().single()
        assertEquals(SyncEntityType.KNOWLEDGE_ITEM, entry.entityType)
        assertEquals(item.id, entry.entityId)
        assertEquals(SyncOperation.UPSERT, entry.operation)
        assertEquals(item.updatedAt, entry.changedAt)
    }

    @Test
    fun editingAnItemAlwaysMovesUpdatedAtForward() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        val edited = knowledge.update(item.copy(definition = "new"))
        assertNotNull(edited)
        assertTrue(edited!!.updatedAt > item.updatedAt)
        assertEquals(edited.updatedAt, outbox().single().changedAt)
    }

    @Test
    fun deletingAnItemReplacesItsPendingEntryWithADelete() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        knowledge.delete(item.id)
        val entry = outbox().single()
        assertEquals(item.id, entry.entityId)
        assertEquals(SyncOperation.DELETE, entry.operation)
        assertNull(knowledge.getById(item.id))
    }

    @Test
    fun deletingAnItemThatDoesNotExistRecordsNothing() = runBlocking {
        knowledge.delete("missing")
        assertTrue(outbox().isEmpty())
    }

    @Test
    fun categoryCreateAndRenameRecordUpserts() = runBlocking {
        val category = categories.create("Science")
        assertEquals(SyncOperation.UPSERT, outbox().single().operation)
        assertEquals(category.createdAt, outbox().single().changedAt)

        categories.rename(category.id, "Natural science")
        val entry = outbox().single()
        assertEquals(SyncEntityType.CATEGORY, entry.entityType)
        assertTrue(entry.changedAt > category.createdAt)
        assertEquals(entry.changedAt, active.current.categoryDao().getById(category.id)!!.updatedAt)
    }

    @Test
    fun deletingACategoryKeepsItsItemsAndRecordsOnlyTheCategoryDelete() = runBlocking {
        val category = categories.create("Science")
        val item = knowledge.create(draft("Entropy", categoryId = category.id))
        categories.delete(category.id)

        assertNull(knowledge.getById(item.id)!!.categoryId)
        val byType = outbox().associateBy { it.entityType }
        assertEquals(SyncOperation.DELETE, byType.getValue(SyncEntityType.CATEGORY).operation)
        assertEquals(SyncOperation.UPSERT, byType.getValue(SyncEntityType.KNOWLEDGE_ITEM).operation)
    }

    @Test
    fun decksRecordUpsertsAndDeletes() = runBlocking {
        val deck = decks.create("Physics")
        assertEquals(SyncOperation.UPSERT, outbox().single().operation)
        decks.rename(deck.id, "Physics 101")
        assertEquals(1, outbox().size)
        decks.delete(deck.id)
        val entry = outbox().single()
        assertEquals(SyncEntityType.DECK, entry.entityType)
        assertEquals(SyncOperation.DELETE, entry.operation)
    }

    @Test
    fun aFailedDuplicateDeckLeavesNoOutboxEntry() = runBlocking {
        decks.create("Physics")
        try {
            decks.create("physics")
            fail("expected DuplicateNameException")
        } catch (expected: DuplicateNameException) {
        }
        assertEquals(1, outbox().size) // only the first deck
    }

    @Test
    fun addingACardRecordsTheCardAndItsReviewState() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        val (cardId, createdAt) = addCard(item.id)

        val byType = outbox().associateBy { it.entityType }
        assertEquals(cardId, byType.getValue(SyncEntityType.FLASHCARD).entityId)
        assertEquals(cardId, byType.getValue(SyncEntityType.REVIEW_STATE).entityId)
        assertEquals(createdAt, active.current.reviewStateDao().getByFlashcardId(cardId)!!.updatedAt)

        // Adding the same item to the same deck again changes nothing.
        val before = outbox().size
        assertEquals(AddToDeckResult.AlreadyInDeck, flashcards.addToDeck(item.id, Deck.DEFAULT_DECK_ID))
        assertEquals(before, outbox().size)
    }

    @Test
    fun savingAReviewStateRecordsAFreshUpdatedAt() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        val (cardId, _) = addCard(item.id)
        val initial = flashcards.getReviewState(cardId)!!
        val firstStamp = active.current.reviewStateDao().getByFlashcardId(cardId)!!.updatedAt

        flashcards.saveReviewState(initial.copy(reviewCount = 1, lastReviewedAt = firstStamp + 5))

        val stored = active.current.reviewStateDao().getByFlashcardId(cardId)!!
        assertEquals(1, stored.reviewCount)
        assertTrue(stored.updatedAt > firstStamp)
        val entry = outbox().first { it.entityType == SyncEntityType.REVIEW_STATE }
        assertEquals(stored.updatedAt, entry.changedAt)
    }

    @Test
    fun removingACardRecordsADelete() = runBlocking {
        val item = knowledge.create(draft("Entropy"))
        val (cardId, _) = addCard(item.id)
        flashcards.removeFromDeck(cardId)
        val entry = outbox().first { it.entityType == SyncEntityType.FLASHCARD }
        assertEquals(SyncOperation.DELETE, entry.operation)
        assertNull(flashcards.getReviewState(cardId))
    }

    @Test
    fun theSeededDefaultDeckNeverWinsAConflictUntilItIsRenamed() = runBlocking {
        assertEquals(0L, active.current.deckDao().getById(Deck.DEFAULT_DECK_ID)!!.updatedAt)
        decks.rename(Deck.DEFAULT_DECK_ID, "My cards")
        assertTrue(active.current.deckDao().getById(Deck.DEFAULT_DECK_ID)!!.updatedAt > 0L)
    }
}