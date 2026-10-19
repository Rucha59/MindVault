package com.mindvault.data.mapper

import com.mindvault.data.local.entity.DeckEntity
import com.mindvault.domain.model.Deck

internal fun DeckEntity.toDomain() = Deck(
    id = id,
    name = name,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Deck.toEntity() = DeckEntity(
    id = id,
    name = name,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
)