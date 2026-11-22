package com.mindvault.domain.model

/** What happens to an item's card in its old category's deck when the item leaves the category. */
enum class DeckRemovalPreference {
    /** Ask each time (the default). */
    ASK,
    ALWAYS_REMOVE,
    ALWAYS_KEEP,
}