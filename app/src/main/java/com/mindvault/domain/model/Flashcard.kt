package com.mindvault.domain.model

data class Flashcard(
    val id: String,
    val knowledgeItemId: String,
    val deckId: String,
    val createdAt: Long,
)