package com.mindvault.core.di

import com.mindvault.domain.scheduler.ReviewScheduler
import com.mindvault.domain.scheduler.Sm2Scheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The only place that names the active scheduling algorithm. Swap Sm2Scheduler here to change it. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {
    @Binds
    abstract fun bindReviewScheduler(impl: Sm2Scheduler): ReviewScheduler
}