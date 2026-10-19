package com.mindvault.domain.model

data class Deck(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val DEFAULT_DECK_ID = "default-deck"
        const val DEFAULT_DECK_NAME = "Default"
    }
}