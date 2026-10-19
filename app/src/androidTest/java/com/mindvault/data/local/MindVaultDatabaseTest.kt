package com.mindvault.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindVaultDatabaseTest {

    private lateinit var db: MindVaultDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private fun item(id: String, title: String = "Title $id", categoryId: String? = null) =
        KnowledgeItemEntity(id, title, "WORD", "Definition", "", null, categoryId, 1L, 1L)

    private fun deck(id: String) = DeckEntity(id, "Deck $id", false, 1L, 1L)

    private fun card(id: String, itemId: String, deckId: String) =
        FlashcardEntity(id, itemId, deckId, 1L)

    private fun state(cardId: String) = ReviewStateEntity(cardId, null, 1L, 0, 2.5, 0)

    @Test
    fun insertedItem_isEmittedByObserveAll() = runBlocking {
        db.knowledgeItemDao().insert(item("k1"))
        val all = db.knowledgeItemDao().observeAll().first()
        assertEquals(listOf("k1"), all.map { it.id })
    }

    @Test
    fun findByTitle_isCaseInsensitive() = runBlocking {
        db.knowledgeItemDao().insert(item("k1", title = "Ephemeral"))
        assertNotNull(db.knowledgeItemDao().findByTitle("ephemeral"))
    }

    @Test
    fun duplicateFlashcardInSameDeck_isIgnored() = runBlocking {
        db.knowledgeItemDao().insert(item("k1"))
        db.deckDao().insert(deck("d1"))
        val first = db.flashcardDao().insert(card("f1", "k1", "d1"))
        val second = db.flashcardDao().insert(card("f2", "k1", "d1"))
        assertTrue(first != -1L)
        assertEquals(-1L, second)
        assertEquals(1, db.flashcardDao().observeCountByDeck("d1").first())
    }

    @Test
    fun deletingItem_cascadesToFlashcardAndReviewState() = runBlocking {
        db.knowledgeItemDao().insert(item("k1"))
        db.deckDao().insert(deck("d1"))
        db.flashcardDao().insert(card("f1", "k1", "d1"))
        db.reviewStateDao().upsert(state("f1"))

        db.knowledgeItemDao().deleteById("k1")

        assertEquals(0, db.flashcardDao().observeCountByDeck("d1").first())
        assertNull(db.reviewStateDao().getByFlashcardId("f1"))
    }

    @Test
    fun deletingCategory_keepsItemAndNullsCategory() = runBlocking {
        db.categoryDao().insert(CategoryEntity("c1", "Science", 1L))
        db.knowledgeItemDao().insert(item("k1", categoryId = "c1"))

        db.categoryDao().deleteById("c1")

        val saved = db.knowledgeItemDao().getById("k1")
        assertNotNull(saved)
        assertNull(saved!!.categoryId)
    }
}