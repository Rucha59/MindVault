package com.mindvault.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mindvault.domain.repository.AccountDataRepository
import com.mindvault.domain.repository.SyncRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MindVaultApp : Application(), Configuration.Provider {

    @Inject lateinit var accountData: AccountDataRepository
    @Inject lateinit var sync: SyncRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // WorkManager starts on demand with Hilt's factory (the default initializer is removed in the manifest).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Keeps the open vault in step with the signed-in account for the whole process.
        accountData.start()
        // Then keeps that vault in step with the cloud (app open, after changes, about every 6 hours).
        sync.start()
    }
}