package com.mindvault.domain.model

data class Deck(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    /** The category this deck was made for (a category deck); null for the default deck and ordinary decks. */
    val categoryId: String? = null,
) {
    companion object {
        const val DEFAULT_DECK_ID = "default-deck"
        const val DEFAULT_DECK_NAME = "Default"
    }
}