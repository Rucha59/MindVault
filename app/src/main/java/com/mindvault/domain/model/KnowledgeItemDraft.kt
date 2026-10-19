package com.mindvault.domain.model

/** Input for creating an item, from the manual form or a saved Search result. */
data class KnowledgeItemDraft(
    val title: String,
    val type: KnowledgeType,
    val definition: String,
    val personalNotes: String = "",
    val example: String? = null,
    val categoryId: String? = null,
)