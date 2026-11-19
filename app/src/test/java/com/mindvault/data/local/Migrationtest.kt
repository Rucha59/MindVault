package com.mindvault.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.data.local.database.MIGRATION_1_2
import com.mindvault.data.local.database.MINDVAULT_MIGRATIONS
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.SyncEntityType
import com.mindvault.data.local.entity.SyncOperation
import com.mindvault.data.local.entity.outboxEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase C1 migration tests. They read the exported schemas (app/schemas/.../1.json and 2.json) and use their own
 * database file, so they never touch the app's real databases. 2.json is created by building the app once.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindVaultDatabase::class.java,
    )

    private fun createV1WithData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO categories (id, name, createdAt) VALUES ('cat1', 'Science', 1000)")
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('default-deck', 'Default', 1, 500, 600)")
            execSQL("INSERT INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES ('d2', 'Physics', 0, 501, 601)")
            execSQL(
                "INSERT INTO knowledge_items (id, title, type, definition, personalNotes, example, categoryId, createdAt, updatedAt) " +
                        "VALUES ('i1', 'Entropy', 'WORD', 'def', 'notes', NULL, 'cat1', 1000, 2000)",
            )
            execSQL("INSERT INTO flashcards (id, knowledgeItemId, deckId, createdAt) VALUES ('f1', 'i1', 'default-deck', 3000)")
            execSQL("INSERT INTO flashcards (id, knowledgeItemId, deckId, createdAt) VALUES ('f2', 'i1', 'd2', 3001)")
            // f1 was reviewed; f2 never was.
            execSQL("INSERT INTO review_states (flashcardId, lastReviewedAt, nextReviewAt, reviewCount, easeFactor, intervalDays) VALUES ('f1', 4000, 5000, 1, 2.5, 1)")
            execSQL("INSERT INTO review_states (flashcardId, lastReviewedAt, nextReviewAt, reviewCount, easeFactor, intervalDays) VALUES ('f2', NULL, 3001, 0, 2.5, 0)")
            close()
        }
    }

    @Test
    fun migration1To2_keepsEveryRowAndBackfillsUpdatedAt() {
        createV1WithData()

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        fun count(table: String): Int =
            db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(1, count("categories"))
        assertEquals(2, count("decks"))
        assertEquals(1, count("knowledge_items"))
        assertEquals(2, count("flashcards"))
        assertEquals(2, count("review_states"))

        db.query("SELECT updatedAt FROM categories WHERE id = 'cat1'").use {
            it.moveToFirst(); assertEquals(1000L, it.getLong(0)) // category createdAt
        }
        db.query("SELECT updatedAt FROM review_states WHERE flashcardId = 'f1'").use {
            it.moveToFirst(); assertEquals(4000L, it.getLong(0)) // lastReviewedAt
        }
        db.query("SELECT updatedAt FROM review_states WHERE flashcardId = 'f2'").use {
            it.moveToFirst(); assertEquals(3001L, it.getLong(0)) // nextReviewAt, never reviewed
        }
        // Existing columns are untouched.
        db.query("SELECT title, updatedAt, categoryId FROM knowledge_items WHERE id = 'i1'").use {
            it.moveToFirst()
            assertEquals("Entropy", it.getString(0))
            assertEquals(2000L, it.getLong(1))
            assertEquals("cat1", it.getString(2))
        }
        db.query("SELECT updatedAt FROM decks WHERE id = 'default-deck'").use {
            it.moveToFirst(); assertEquals(600L, it.getLong(0))
        }
        // The new sync tables exist and start empty.
        assertEquals(0, count("sync_outbox"))
        assertEquals(0, count("sync_state"))
    }

    @Test
    fun roomOpensMigratedDatabaseAndSyncTablesWork() = runBlocking {
        createV1WithData()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindVaultDatabase::class.java,
            dbName,
        ).addMigrations(*MINDVAULT_MIGRATIONS).build()
        try {
            assertEquals(1, db.knowledgeItemDao().observeAll().first().size)
            assertEquals(2, db.deckDao().observeAll().first().size)

            // One pending entry per entity: the later change replaces the earlier one.
            db.syncOutboxDao().upsert(outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, "i1", SyncOperation.UPSERT, 10L))
            db.syncOutboxDao().upsert(outboxEntry(SyncEntityType.KNOWLEDGE_ITEM, "i1", SyncOperation.DELETE, 20L))
            val pending = db.syncOutboxDao().getAll()
            assertEquals(1, pending.size)
            assertEquals(SyncOperation.DELETE, pending.single().operation)
        } finally {
            db.close()
        }
    }
}