package com.mindvault.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("title"),
        Index("createdAt"),
        Index("updatedAt"),
    ],
)
data class KnowledgeItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // KnowledgeType.name, stored as text so enum changes never break old rows
    val definition: String,
    val personalNotes: String,
    val example: String?,
    val categoryId: String?,
    val createdAt: Long, // UTC epoch millis
    val updatedAt: Long, // UTC epoch millis
)