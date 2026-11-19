package com.mindvault.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindvault.domain.model.Deck

/**
 * Seeds the default deck once, when the database file is first created.
 * Its updatedAt is 0 on purpose: an untouched default deck must never win a sync conflict against a default
 * deck that was renamed on another device. Renaming it sets a real updatedAt.
 */
object DefaultDeckCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT OR IGNORE INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES (?, ?, 1, ?, 0)",
            arrayOf<Any?>(Deck.DEFAULT_DECK_ID, Deck.DEFAULT_DECK_NAME, now),
        )
    }
}