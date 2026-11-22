package com.mindvault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["name"], unique = true), Index("categoryId")],
)
data class DeckEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val isDefault: Boolean,
    val createdAt: Long, // UTC epoch millis
    val updatedAt: Long, // UTC epoch millis
    /** The category this deck was made for (null for the default deck and ordinary decks). Added in schema v5. */
    val categoryId: String? = null,
)