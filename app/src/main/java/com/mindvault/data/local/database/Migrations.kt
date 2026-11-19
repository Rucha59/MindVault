package com.mindvault.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Add one Migration(from, to) per schema bump, then raise `version` in MindVaultDatabase.
 * Never use fallbackToDestructiveMigration: it deletes user data.
 */

/**
 * v1 -> v2 (Phase C1, cloud sync). Only adds; no existing row is moved, rewritten or deleted.
 *  - categories.updatedAt and review_states.updatedAt: new columns, filled from the best timestamp each row
 *    already has (category createdAt; review state lastReviewedAt, or nextReviewAt for a card never reviewed).
 *  - sync_outbox and sync_state: new, empty tables.
 * Runs on the guest vault and on every account vault the first time it is opened.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `categories` SET `updatedAt` = `createdAt`")
        db.execSQL("ALTER TABLE `review_states` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `review_states` SET `updatedAt` = COALESCE(`lastReviewedAt`, `nextReviewAt`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_outbox` (`entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                    "`operation` TEXT NOT NULL, `changedAt` INTEGER NOT NULL, PRIMARY KEY(`entityType`, `entityId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_state` (`stateKey` TEXT NOT NULL, `stateValue` TEXT NOT NULL, " +
                    "PRIMARY KEY(`stateKey`))",
        )
    }
}

val MINDVAULT_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)