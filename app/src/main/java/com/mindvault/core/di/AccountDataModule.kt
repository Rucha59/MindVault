package com.mindvault.core.di

import com.mindvault.data.repository.AccountDataRepositoryImpl
import com.mindvault.domain.repository.AccountDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDataModule {
    @Binds @Singleton
    abstract fun bindAccountDataRepository(impl: AccountDataRepositoryImpl): AccountDataRepository
}