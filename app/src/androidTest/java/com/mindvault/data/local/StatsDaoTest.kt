package com.mindvault.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindvault.data.local.database.MindVaultDatabase
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewLogEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The stats queries against a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
class StatsDaoTest {
    private lateinit var db: MindVaultDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MindVaultDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun seed() = runBlocking {
        db.categoryDao().insert(CategoryEntity("c1", "Science", 1L, 1L))
        db.deckDao().insert(DeckEntity("d1", "Default", true, 1L, 1L, null))
        db.knowledgeItemDao().insert(KnowledgeItemEntity("i1", "Entropy", "CONCEPT", "def", "", null, "c1", 10L, 10L))
        db.knowledgeItemDao().insert(KnowledgeItemEntity("i2", "Run", "WORD", "def", "", null, null, 20L, 20L))
        db.flashcardDao().insert(FlashcardEntity("f1", "i1", "d1", 10L))
        db.flashcardDao().insert(FlashcardEntity("f2", "i2", "d1", 20L))
        // f1: reviewed, due before tomorrow, difficult (ease 1.8), learning (interval 3). f2: never reviewed.
        db.reviewStateDao().upsert(ReviewStateEntity("f1", 50L, 100L, 2, 1.8, 3, 50L))
        db.reviewStateDao().upsert(ReviewStateEntity("f2", null, 20L, 0, 2.5, 0, 20L))
        db.reviewLogDao().insert(ReviewLogEntity("l1", "f1", "GOOD", 50L, 0, 2))
        db.reviewLogDao().insert(ReviewLogEntity("l2", "f1", "HARD", 60L, 2, 1))
        db.reviewLogDao().insert(ReviewLogEntity("l3", "gone", "GOOD", 5L, 0, 2))
    }

    @Test
    fun countsFollowTheReviewQueueRules() = runBlocking {
        seed()
        val counts = db.statsDao().observeCounts(weekStart = 0L, startOfTomorrow = 1_000L, difficultBelow = 2.0, matureDays = 21).first()

        assertEquals(2, counts.totalItems)
        assertEquals(2, counts.addedRecently)
        assertEquals(1, counts.uncategorizedItems)
        assertEquals(1, counts.categoryCount)
        assertEquals(1, counts.deckCount)
        assertEquals(2, counts.flashcardCount)
        assertEquals(1, counts.newCards)
        assertEquals(1, counts.dueReviews)
        assertEquals(1, counts.difficult)
        assertEquals(1, counts.learning)
        assertEquals(0, counts.mature)
    }

    @Test
    fun recentlyAddedUsesTheWindowStart() = runBlocking {
        seed()
        val counts = db.statsDao().observeCounts(weekStart = 15L, startOfTomorrow = 1_000L, difficultBelow = 2.0, matureDays = 21).first()
        assertEquals(1, counts.addedRecently) // only "Run" (created at 20)
    }

    @Test
    fun cardsDueTomorrowAreNotDue() = runBlocking {
        seed()
        val counts = db.statsDao().observeCounts(weekStart = 0L, startOfTomorrow = 100L, difficultBelow = 2.0, matureDays = 21).first()
        assertEquals(0, counts.dueReviews) // f1 is due at 100, not before 100
    }

    @Test
    fun typeCategoryAndRatingGroups() = runBlocking {
        seed()
        val dao = db.statsDao()

        assertEquals(mapOf("CONCEPT" to 1, "WORD" to 1), dao.observeTypeCounts().first().associate { it.name to it.total })
        assertEquals(listOf("Science" to 1), dao.observeTopCategories(5).first().map { it.name to it.total })
        assertEquals(mapOf("GOOD" to 2, "HARD" to 1), dao.observeRatingCounts().first().associate { it.name to it.total })
    }

    @Test
    fun reviewTimesAreNewestFirstAndRespectTheStart() = runBlocking {
        seed()
        assertEquals(listOf(60L, 50L), db.statsDao().observeReviewTimes(since = 10L).first())
        assertEquals(listOf(60L, 50L, 5L), db.statsDao().observeReviewTimes(since = 0L).first())
    }

    @Test
    fun logHasNoForeignKeyToCardsSoHistorySurvivesDeletingACard() = runBlocking {
        seed()
        db.flashcardDao().deleteById("f1")
        assertTrue(db.reviewLogDao().getById("l1") != null)
        assertEquals(3, db.statsDao().observeReviewTimes(since = 0L).first().size)
    }

    @Test
    fun anExistingLogEntryIsNotReplaced() = runBlocking {
        seed()
        val second = db.reviewLogDao().insert(ReviewLogEntity("l1", "f1", "EASY", 99L, 9, 9))
        assertEquals(-1L, second)
        assertEquals("GOOD", db.reviewLogDao().getById("l1")?.rating)
    }
}