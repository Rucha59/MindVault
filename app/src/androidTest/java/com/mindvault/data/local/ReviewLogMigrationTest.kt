package com.mindvault.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.data.local.database.MIGRATION_5_6
import com.mindvault.data.local.database.MindVaultDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v5 -> v6 only adds the empty review_log table; existing data is untouched. Needs schemas 5.json and 6.json. */
@RunWith(AndroidJUnit4::class)
class ReviewLogMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindVaultDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate5To6_addsAnEmptyReviewLogAndKeepsExistingRows() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO knowledge_items (id, title, type, definition, personalNotes, example, categoryId, " +
                        "createdAt, updatedAt) VALUES ('i1', 'Entropy', 'CONCEPT', 'def', '', NULL, NULL, 1, 1)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        db.query("SELECT COUNT(*) FROM review_log").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM knowledge_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "review-log-migration-test"
    }
}