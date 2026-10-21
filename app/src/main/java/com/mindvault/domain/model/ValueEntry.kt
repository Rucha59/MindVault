package com.mindvault.domain.model

/** A saved item with its category name resolved for display. */
data class VaultEntry(
    val item: KnowledgeItem,
    val categoryName: String?,
)