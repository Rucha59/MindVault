package com.mindvault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val createdAt: Long, // UTC epoch millis
    /** Last create or rename, UTC epoch millis. Decides sync conflicts. Added in schema v2. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
)