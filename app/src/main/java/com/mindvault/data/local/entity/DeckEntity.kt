package com.mindvault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    indices = [Index(value = ["name"], unique = true)],
)
data class DeckEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val isDefault: Boolean,
    val createdAt: Long, // UTC epoch millis
    val updatedAt: Long, // UTC epoch millis
)