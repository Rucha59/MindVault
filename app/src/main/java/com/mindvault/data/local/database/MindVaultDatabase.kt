package com.mindvault.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindvault.data.local.dao.CategoryDao
import com.mindvault.data.local.dao.DeckDao
import com.mindvault.data.local.dao.FlashcardDao
import com.mindvault.data.local.dao.KnowledgeItemDao
import com.mindvault.data.local.dao.ReviewStateDao
import com.mindvault.data.local.dao.SyncOutboxDao
import com.mindvault.data.local.dao.SyncStateDao
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity
import com.mindvault.data.local.entity.SyncOutboxEntity
import com.mindvault.data.local.entity.SyncStateEntity

@Database(
    entities = [
        KnowledgeItemEntity::class,
        CategoryEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewStateEntity::class,
        SyncOutboxEntity::class,
        SyncStateEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MindVaultDatabase : RoomDatabase() {
    abstract fun knowledgeItemDao(): KnowledgeItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "mindvault.db"
    }
}