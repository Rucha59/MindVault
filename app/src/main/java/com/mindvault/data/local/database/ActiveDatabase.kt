package com.mindvault.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import com.mindvault.domain.model.DataOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Owns the open Room database and switches it when the account changes.
 *
 * - Guest vault: the original [guestDatabaseName] file in the normal databases folder (never touched).
 * - Account vault: one file per account in [accountsDir] (no_backup, so Android never backs it up).
 *
 * Repositories must go through [current] or [observe]; the database and DAOs are not injectable.
 */
@Singleton
class ActiveDatabase(
    private val context: Context,
    private val ownerStore: ActiveOwnerStore,
    private val guestDatabaseName: String,
    private val accountsDir: File,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        ownerStore: ActiveOwnerStore,
    ) : this(context, ownerStore, MindVaultDatabase.NAME, File(context.noBackupFilesDir, "users"))

    private val lock = Any()
    private val databases = HashMap<DataOwner, MindVaultDatabase>()
    private val _owner = MutableStateFlow(ownerStore.read())

    val owner: StateFlow<DataOwner> = _owner.asStateFlow()

    /** The database for the owner right now. For read-then-write work, read this once and reuse it. */
    val current: MindVaultDatabase get() = databaseFor(_owner.value)


    /** Re-subscribes to [source] whenever the owner changes, so screens follow the active vault. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(source: (MindVaultDatabase) -> Flow<T>): Flow<T> =
        owner.flatMapLatest { source(databaseFor(it)) }

    fun switchTo(newOwner: DataOwner) {
        synchronized(lock) {
            if (_owner.value == newOwner) return
            ownerStore.write(newOwner)
            _owner.value = newOwner
        }
    }

    /** Switches to the guest vault if [userId]'s vault is open, then closes and deletes its files. */
    fun deleteAccountDatabase(userId: String) {
        val target = DataOwner.Account(userId)
        synchronized(lock) {
            if (_owner.value == target) {
                ownerStore.write(DataOwner.Guest)
                _owner.value = DataOwner.Guest
            }
            databases.remove(target)?.let { db ->
                try {
                    db.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not close a vault that is being deleted (${e::class.simpleName})")
                }
            }
            if (!SQLiteDatabase.deleteDatabase(accountFile(userId))) {
                Log.w(TAG, "No local vault file was deleted for the removed account")
            }
        }
    }

    private fun databaseFor(owner: DataOwner): MindVaultDatabase = synchronized(lock) {
        databases.getOrPut(owner) { build(owner) }
    }

    private fun build(owner: DataOwner): MindVaultDatabase {
        val name = when (owner) {
            DataOwner.Guest -> guestDatabaseName
            is DataOwner.Account -> {
                accountsDir.mkdirs()
                accountFile(owner.userId).absolutePath
            }
        }
        return Room.databaseBuilder(context, MindVaultDatabase::class.java, name)
            .addCallback(DefaultDeckCallback)
            .addMigrations(*MINDVAULT_MIGRATIONS)
            .build()
    }

    private fun accountFile(userId: String) =
        File(accountsDir, "mindvault_${userId.replace(UNSAFE_FILE_CHARS, "_")}.db")

    private companion object {
        const val TAG = "ActiveDatabase"
        val UNSAFE_FILE_CHARS = Regex("[^A-Za-z0-9_-]")
    }
}