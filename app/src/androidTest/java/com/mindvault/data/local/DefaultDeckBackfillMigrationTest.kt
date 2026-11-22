package com.mindvault.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.data.local.database.MIGRATION_3_4
import com.mindvault.data.local.database.MindVaultDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v3 -> v4: every Vault item gets a default-deck card; nothing existing is touched. Needs app/schemas/.../3.json and 4.json. */
@RunWith(AndroidJUnit4::class)
class DefaultDeckBackfillMigrationTest {

    private val dbName = "default-deck-backfill-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindVaultDatabase::class.java,
    )

    private fun createV3WithData() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('default-deck', 'Default', 1, 500, 0)")
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('d2', 'Physics', 0, 501, 601)")
            for ((id, created) in listOf("i1" to 1000, "i2" to 1001, "i3" to 1002)) {
                execSQL(
                    "INSERT INTO knowledge_items (id, title, type, definition, personalNotes, example, categoryId, createdAt, updatedAt) " +
                            "VALUES ('$id', 'Title $id', 'WORD', 'def', '', NULL, NULL, $created, $created)",
                )
            }
            // i1 already has a (reviewed) card in the default deck; i2 only has one in another deck; i3 has none.
            execSQL("INSERT INTO flashcards (id, knowledgeItemId, deckId, createdAt) VALUES ('f1', 'i1', 'default-deck', 3000)")
            execSQL("INSERT INTO flashcards (id, knowledgeItemId, deckId, createdAt) VALUES ('f2', 'i2', 'd2', 3001)")
            execSQL("INSERT INTO review_states (flashcardId, lastReviewedAt, nextReviewAt, reviewCount, easeFactor, intervalDays, updatedAt) VALUES ('f1', 4000, 9000, 3, 1.9, 12, 4000)")
            execSQL("INSERT INTO review_states (flashcardId, lastReviewedAt, nextReviewAt, reviewCount, easeFactor, intervalDays, updatedAt) VALUES ('f2', NULL, 3001, 0, 2.5, 0, 3001)")
            close()
        }
    }

    @Test
    fun migration3To4_givesEveryItemADefaultDeckCardAndKeepsExistingData() {
        createV3WithData()

        val db = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        fun count(sql: String): Int = db.query(sql).use { it.moveToFirst(); it.getInt(0) }

        // Exactly one default-deck card per item.
        assertEquals(3, count("SELECT COUNT(*) FROM flashcards WHERE deckId = 'default-deck'"))
        for (id in listOf("i1", "i2", "i3")) {
            assertEquals(1, count("SELECT COUNT(*) FROM flashcards WHERE knowledgeItemId = '$id' AND deckId = 'default-deck'"))
        }
        // Existing cards and progress are untouched.
        assertEquals(1, count("SELECT COUNT(*) FROM flashcards WHERE id = 'f1' AND deckId = 'default-deck'"))
        assertEquals(1, count("SELECT COUNT(*) FROM flashcards WHERE id = 'f2' AND deckId = 'd2'"))
        assertEquals(1, count("SELECT COUNT(*) FROM review_states WHERE flashcardId = 'f1' AND reviewCount = 3 AND intervalDays = 12"))
        assertEquals(3, count("SELECT COUNT(*) FROM knowledge_items"))

        // The two new cards are new cards, created with their item's date, each with a review state.
        db.query(
            "SELECT f.createdAt, r.reviewCount, r.easeFactor, r.intervalDays FROM flashcards f " +
                    "JOIN review_states r ON r.flashcardId = f.id " +
                    "WHERE f.deckId = 'default-deck' AND f.id <> 'f1' ORDER BY f.createdAt",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(1001L, it.getLong(0)) // i2
            assertEquals(0, it.getInt(1))
            assertEquals(2.5, it.getDouble(2), 0.0)
            assertEquals(0, it.getInt(3))
            assertTrue(it.moveToNext())
            assertEquals(1002L, it.getLong(0)) // i3
            assertTrue(!it.moveToNext())
        }

        // Only the two new cards and their review states are queued for sync.
        assertEquals(2, count("SELECT COUNT(*) FROM sync_outbox WHERE entityType = 'FLASHCARD' AND operation = 'UPSERT'"))
        assertEquals(2, count("SELECT COUNT(*) FROM sync_outbox WHERE entityType = 'REVIEW_STATE' AND operation = 'UPSERT'"))
        assertEquals(0, count("SELECT COUNT(*) FROM sync_outbox WHERE entityId = 'f1'"))
    }
}