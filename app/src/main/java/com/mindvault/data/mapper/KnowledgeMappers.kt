package com.mindvault.data.mapper

import com.mindvault.data.local.entity.CategoryEntity
import com.mindvault.data.local.entity.KnowledgeItemEntity
import com.mindvault.domain.model.Category
import com.mindvault.domain.model.KnowledgeItem
import com.mindvault.domain.model.KnowledgeType

/** Unknown stored values (e.g. from a newer app version) fall back to OTHER instead of crashing. */
internal fun String.toKnowledgeType(): KnowledgeType =
    KnowledgeType.entries.firstOrNull { it.name == this } ?: KnowledgeType.OTHER

internal fun KnowledgeItemEntity.toDomain() = KnowledgeItem(
    id = id,
    title = title,
    type = type.toKnowledgeType(),
    definition = definition,
    personalNotes = personalNotes,
    example = example,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun KnowledgeItem.toEntity() = KnowledgeItemEntity(
    id = id,
    title = title,
    type = type.name,
    definition = definition,
    personalNotes = personalNotes,
    example = example,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CategoryEntity.toDomain() = Category(id = id, name = name, createdAt = createdAt)

internal fun Category.toEntity() = CategoryEntity(id = id, name = name, createdAt = createdAt)