package com.mindvault.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindvault.data.local.dao.CategoryDao
import com.mindvault.data.local.dao.DeckDao
import com.mindvault.data.local.dao.FlashcardDao
import com.mindvault.data.local.dao.KnowledgeItemDao
import com.mindvault.data.local.dao.ReviewStateDao
import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.data.local.entity.FlashcardEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.data.local.entity.ReviewStateEntity

@Database(
    entities = [
        KnowledgeItemEntity::class,
        CategoryEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MindVaultDatabase : RoomDatabase() {
    abstract fun knowledgeItemDao(): KnowledgeItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewStateDao(): ReviewStateDao

    companion object {
        const val NAME = "mindvault.db"
    }
}