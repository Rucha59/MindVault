package com.mindvault.core.di

import com.mindvault.data.sync.SupabaseSyncRemote
import com.mindvault.data.sync.SyncRemote
import com.mindvault.data.sync.SyncRepositoryImpl
import com.mindvault.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds @Singleton
    abstract fun bindSyncRemote(impl: SupabaseSyncRemote): SyncRemote
}