package com.mindvault.core.di

import android.content.Context
import androidx.room.Room
import com.mindvault.data.local.dao.CategoryDao
import com.mindvault.data.local.dao.DeckDao
import com.mindvault.data.local.dao.FlashcardDao
import com.mindvault.data.local.dao.KnowledgeItemDao
import com.mindvault.data.local.dao.ReviewStateDao
import com.mindvault.data.local.database.DefaultDeckCallback
import com.mindvault.data.local.database.MINDVAULT_MIGRATIONS
import com.mindvault.data.local.database.MindVaultDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MindVaultDatabase =
        Room.databaseBuilder(context, MindVaultDatabase::class.java, MindVaultDatabase.NAME)
            .addCallback(DefaultDeckCallback)
            .addMigrations(*MINDVAULT_MIGRATIONS)
            .build()

    @Provides fun provideKnowledgeItemDao(db: MindVaultDatabase): KnowledgeItemDao = db.knowledgeItemDao()
    @Provides fun provideCategoryDao(db: MindVaultDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideDeckDao(db: MindVaultDatabase): DeckDao = db.deckDao()
    @Provides fun provideFlashcardDao(db: MindVaultDatabase): FlashcardDao = db.flashcardDao()
    @Provides fun provideReviewStateDao(db: MindVaultDatabase): ReviewStateDao = db.reviewStateDao()
}