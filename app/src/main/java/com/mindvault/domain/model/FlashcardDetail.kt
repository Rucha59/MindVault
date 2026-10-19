package com.mindvault.domain.model

/** A card plus the knowledge item it references (never a copy) and its review state. */
data class FlashcardDetail(
    val flashcard: Flashcard,
    val knowledgeItem: KnowledgeItem,
    val reviewState: ReviewState,
)