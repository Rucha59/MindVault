package com.mindvault.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.Deck
import java.util.UUID

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

/**
 * v2 -> v3 (Flashcards V2, spaced repetition). DATA ONLY: no table or column changes.
 *  - Every review state becomes a new card (never reviewed, due now, ease 2.5, interval 0). Vault items, decks,
 *    cards and categories are untouched.
 *  - updatedAt = MAX(updatedAt + 1, now), so the reset is newer than any copy in the cloud and wins sync conflicts.
 *  - Every review state is queued in sync_outbox so the cloud receives the reset.
 * Runs on the guest vault and on every account vault the first time each is opened.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = nowMillis()
        db.execSQL(
            "UPDATE `review_states` SET `lastReviewedAt` = NULL, `nextReviewAt` = $now, `reviewCount` = 0, " +
                    "`easeFactor` = 2.5, `intervalDays` = 0, `updatedAt` = MAX(`updatedAt` + 1, $now)",
        )
        db.execSQL(
            "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                    "SELECT 'REVIEW_STATE', `flashcardId`, 'UPSERT', `updatedAt` FROM `review_states`",
        )
    }
}

/**
 * v3 -> v4 (default deck holds every Vault item). DATA ONLY: no table or column changes.
 *  - Every Vault item without a card in the default deck gets one (created with the item's own creation date, so
 *    new cards come oldest first) and a starting review state (new card, ease 2.5, interval 0).
 *  - Existing cards and their progress are untouched. Nothing is deleted.
 *  - The new cards and review states are queued in sync_outbox so the cloud receives them.
 * Skipped when the vault has no default deck. Runs on the guest vault and on every account vault the first time
 * each is opened.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = nowMillis()
        val deckId = Deck.DEFAULT_DECK_ID
        db.execSQL(
            "CREATE TEMP TABLE `_default_deck_backfill` AS " +
                    "SELECT lower(hex(randomblob(16))) AS `cardId`, k.`id` AS `itemId`, k.`createdAt` AS `itemCreatedAt` " +
                    "FROM `knowledge_items` k " +
                    "WHERE EXISTS (SELECT 1 FROM `decks` WHERE `id` = '$deckId') " +
                    "AND NOT EXISTS (SELECT 1 FROM `flashcards` f WHERE f.`knowledgeItemId` = k.`id` AND f.`deckId` = '$deckId')",
        )
        db.execSQL(
            "INSERT INTO `flashcards` (`id`, `knowledgeItemId`, `deckId`, `createdAt`) " +
                    "SELECT `cardId`, `itemId`, '$deckId', `itemCreatedAt` FROM `_default_deck_backfill`",
        )
        db.execSQL(
            "INSERT INTO `review_states` (`flashcardId`, `lastReviewedAt`, `nextReviewAt`, `reviewCount`, " +
                    "`easeFactor`, `intervalDays`, `updatedAt`) " +
                    "SELECT `cardId`, NULL, $now, 0, 2.5, 0, $now FROM `_default_deck_backfill`",
        )
        db.execSQL(
            "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                    "SELECT 'FLASHCARD', `cardId`, 'UPSERT', $now FROM `_default_deck_backfill`",
        )
        db.execSQL(
            "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                    "SELECT 'REVIEW_STATE', `cardId`, 'UPSERT', $now FROM `_default_deck_backfill`",
        )
        db.execSQL("DROP TABLE `_default_deck_backfill`")
    }
}

/**
 * v4 -> v5 (category decks). Adds the nullable column decks.categoryId (foreign key to categories, ON DELETE SET
 * NULL, indexed), then gives every existing category a deck and every item in it a card:
 *  - one deck per category, named after it ("Name (2)" if a deck already has that name), linked by categoryId;
 *  - one card per item in the category (new card, ease 2.5, interval 0), created with the item's own date;
 *  - the new decks, cards and review states are queued in sync_outbox so the cloud receives them.
 * Nothing existing is changed or deleted. Runs on the guest vault and on every account vault the first time each
 * is opened.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `decks` ADD COLUMN `categoryId` TEXT REFERENCES `categories`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE SET NULL",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_decks_categoryId` ON `decks` (`categoryId`)")

        val now = nowMillis()
        val takenNames = mutableSetOf<String>()
        db.query("SELECT `name` FROM `decks`").use { cursor ->
            while (cursor.moveToNext()) takenNames += cursor.getString(0).lowercase()
        }
        val categories = mutableListOf<Pair<String, String>>()
        db.query("SELECT `id`, `name` FROM `categories` ORDER BY `createdAt`, `id`").use { cursor ->
            while (cursor.moveToNext()) categories += cursor.getString(0) to cursor.getString(1)
        }

        for ((categoryId, categoryName) in categories) {
            var name = categoryName
            var n = 2
            while (name.lowercase() in takenNames) name = "$categoryName (${n++})"
            takenNames += name.lowercase()
            val deckId = UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO `decks` (`id`, `name`, `isDefault`, `createdAt`, `updatedAt`, `categoryId`) " +
                        "VALUES (?, ?, 0, ?, ?, ?)",
                arrayOf<Any?>(deckId, name, now, now, categoryId),
            )
            db.execSQL(
                "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                        "VALUES ('DECK', ?, 'UPSERT', ?)",
                arrayOf<Any?>(deckId, now),
            )
        }

        db.execSQL(
            "CREATE TEMP TABLE `_category_deck_backfill` AS " +
                    "SELECT lower(hex(randomblob(16))) AS `cardId`, k.`id` AS `itemId`, d.`id` AS `deckId`, " +
                    "k.`createdAt` AS `itemCreatedAt` " +
                    "FROM `knowledge_items` k JOIN `decks` d ON d.`categoryId` = k.`categoryId` " +
                    "WHERE NOT EXISTS (SELECT 1 FROM `flashcards` f WHERE f.`knowledgeItemId` = k.`id` AND f.`deckId` = d.`id`)",
        )
        db.execSQL(
            "INSERT INTO `flashcards` (`id`, `knowledgeItemId`, `deckId`, `createdAt`) " +
                    "SELECT `cardId`, `itemId`, `deckId`, `itemCreatedAt` FROM `_category_deck_backfill`",
        )
        db.execSQL(
            "INSERT INTO `review_states` (`flashcardId`, `lastReviewedAt`, `nextReviewAt`, `reviewCount`, " +
                    "`easeFactor`, `intervalDays`, `updatedAt`) " +
                    "SELECT `cardId`, NULL, $now, 0, 2.5, 0, $now FROM `_category_deck_backfill`",
        )
        db.execSQL(
            "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                    "SELECT 'FLASHCARD', `cardId`, 'UPSERT', $now FROM `_category_deck_backfill`",
        )
        db.execSQL(
            "INSERT OR REPLACE INTO `sync_outbox` (`entityType`, `entityId`, `operation`, `changedAt`) " +
                    "SELECT 'REVIEW_STATE', `cardId`, 'UPSERT', $now FROM `_category_deck_backfill`",
        )
        db.execSQL("DROP TABLE `_category_deck_backfill`")
    }
}

val MINDVAULT_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
