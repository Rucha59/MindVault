package com.mindvault.core.di

import com.mindvault.data.repository.NotificationSettingsRepositoryImpl
import com.mindvault.domain.repository.NotificationSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindNotificationSettingsRepository(impl: NotificationSettingsRepositoryImpl): NotificationSettingsRepository
}