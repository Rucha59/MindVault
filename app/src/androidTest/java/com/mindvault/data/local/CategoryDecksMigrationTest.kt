package com.mindvault.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.data.local.database.MIGRATION_4_5
import com.mindvault.data.local.database.MindVaultDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v4 -> v5: a deck per category with cards for its items; nothing existing is touched. Needs 4.json and 5.json. */
@RunWith(AndroidJUnit4::class)
class CategoryDecksMigrationTest {

    private val dbName = "category-decks-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindVaultDatabase::class.java,
    )

    private fun createV4WithData() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO categories (id, name, createdAt, updatedAt) VALUES ('c1', 'Science', 100, 100)")
            execSQL("INSERT INTO categories (id, name, createdAt, updatedAt) VALUES ('c2', 'Physics', 101, 101)")
            execSQL("INSERT INTO categories (id, name, createdAt, updatedAt) VALUES ('c3', 'Empty', 102, 102)")
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('default-deck', 'Default', 1, 500, 0)")
            // A deck the user already made with the same name as a category.
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('d2', 'Physics', 0, 501, 601)")
            for ((id, category) in listOf("i1" to "c1", "i2" to "c1", "i3" to "c2", "i4" to null)) {
                val categoryValue = if (category == null) "NULL" else "'$category'"
                execSQL(
                    "INSERT INTO knowledge_items (id, title, type, definition, personalNotes, example, categoryId, createdAt, updatedAt) " +
                            "VALUES ('$id', 'Title $id', 'WORD', 'def', '', NULL, $categoryValue, 1000, 1000)",
                )
            }
            execSQL("INSERT INTO flashcards (id, knowledgeItemId, deckId, createdAt) VALUES ('f1', 'i1', 'default-deck', 3000)")
            execSQL("INSERT INTO review_states (flashcardId, lastReviewedAt, nextReviewAt, reviewCount, easeFactor, intervalDays, updatedAt) VALUES ('f1', 4000, 9000, 3, 1.9, 12, 4000)")
            close()
        }
    }

    @Test
    fun migration4To5_givesEveryCategoryADeckWithItsItemsAndKeepsExistingData() {
        createV4WithData()

        val db = helper.runMigrationsAndValidate(dbName, 5, true, MIGRATION_4_5)

        fun count(sql: String): Int = db.query(sql).use { it.moveToFirst(); it.getInt(0) }
        fun deckName(categoryId: String): String =
            db.query("SELECT name FROM decks WHERE categoryId = '$categoryId'").use { it.moveToFirst(); it.getString(0) }

        // One deck per category, named after it; "Physics" is taken by the user's own deck.
        assertEquals(3, count("SELECT COUNT(*) FROM decks WHERE categoryId IS NOT NULL"))
        assertEquals("Science", deckName("c1"))
        assertEquals("Physics (2)", deckName("c2"))
        assertEquals("Empty", deckName("c3"))
        assertEquals(5, count("SELECT COUNT(*) FROM decks"))

        // Cards: Science deck has i1 and i2, the Physics deck has i3, the Empty deck has none; i4 has no category.
        fun cardsIn(categoryId: String): Int = count(
            "SELECT COUNT(*) FROM flashcards f JOIN decks d ON d.id = f.deckId WHERE d.categoryId = '$categoryId'",
        )
        assertEquals(2, cardsIn("c1"))
        assertEquals(1, cardsIn("c2"))
        assertEquals(0, cardsIn("c3"))
        assertEquals(4, count("SELECT COUNT(*) FROM flashcards")) // f1 + 3 new
        assertEquals(1, count("SELECT COUNT(*) FROM review_states WHERE flashcardId = 'f1' AND reviewCount = 3 AND intervalDays = 12"))
        assertEquals(4, count("SELECT COUNT(*) FROM review_states"))
        assertEquals(0, count("SELECT COUNT(*) FROM review_states r JOIN flashcards f ON f.id = r.flashcardId JOIN decks d ON d.id = f.deckId WHERE d.categoryId IS NOT NULL AND r.reviewCount <> 0"))

        // The user's deck and the default deck are unchanged; items are untouched.
        assertEquals(1, count("SELECT COUNT(*) FROM decks WHERE id = 'd2' AND name = 'Physics' AND categoryId IS NULL AND updatedAt = 601"))
        assertEquals(1, count("SELECT COUNT(*) FROM decks WHERE id = 'default-deck' AND categoryId IS NULL AND updatedAt = 0"))
        assertEquals(4, count("SELECT COUNT(*) FROM knowledge_items"))

        // The 3 new decks, 3 new cards and 3 review states are queued for sync; f1 is not.
        assertEquals(3, count("SELECT COUNT(*) FROM sync_outbox WHERE entityType = 'DECK' AND operation = 'UPSERT'"))
        assertEquals(3, count("SELECT COUNT(*) FROM sync_outbox WHERE entityType = 'FLASHCARD' AND operation = 'UPSERT'"))
        assertEquals(3, count("SELECT COUNT(*) FROM sync_outbox WHERE entityType = 'REVIEW_STATE' AND operation = 'UPSERT'"))
        assertEquals(0, count("SELECT COUNT(*) FROM sync_outbox WHERE entityId = 'f1'"))
    }
}