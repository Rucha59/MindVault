package com.mindvault.core.di

import com.mindvault.data.repository.CategoryRepositoryImpl
import com.mindvault.data.repository.DeckRepositoryImpl
import com.mindvault.data.repository.FlashcardRepositoryImpl
import com.mindvault.data.repository.KnowledgeRepositoryImpl
import com.mindvault.data.repository.SearchRepositoryImpl
import com.mindvault.data.repository.WikimediaSearchRepository
import com.mindvault.domain.repository.CategoryRepository
import com.mindvault.domain.repository.DeckRepository
import com.mindvault.domain.repository.FlashcardRepository
import com.mindvault.domain.repository.KnowledgeRepository
import com.mindvault.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindKnowledgeRepository(impl: KnowledgeRepositoryImpl): KnowledgeRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindDeckRepository(impl: DeckRepositoryImpl): DeckRepository

    @Binds @Singleton
    abstract fun bindFlashcardRepository(impl: FlashcardRepositoryImpl): FlashcardRepository

    // Search V2 (Wiktionary words + Wikipedia concepts). To switch back to V1 (Free Dictionary, words only),
    // bind SearchRepositoryImpl here instead; nothing else changes.
    @Binds @Singleton
    abstract fun bindSearchRepository(impl: WikimediaSearchRepository): SearchRepository
}