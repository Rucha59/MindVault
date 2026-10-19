package com.mindvault.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindvault.domain.model.Deck

/** Seeds the default deck once, when the database file is first created. */
object DefaultDeckCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT OR IGNORE INTO decks (id, name, isDefault, createdAt, updatedAt) VALUES (?, ?, 1, ?, ?)",
            arrayOf<Any?>(Deck.DEFAULT_DECK_ID, Deck.DEFAULT_DECK_NAME, now, now),
        )
    }
}