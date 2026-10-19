package com.mindvault.data.local.database

import androidx.room.migration.Migration

/**
 * Add one Migration(from, to) per schema bump, then raise `version` in MindVaultDatabase.
 * Never use fallbackToDestructiveMigration: it deletes user data.
 * Version 1 is the first release schema, so the list is empty for now.
 */
val MINDVAULT_MIGRATIONS: Array<Migration> = emptyArray()