package com.mindvault.domain.model

data class KnowledgeItem(
    val id: String,
    val title: String,
    val type: KnowledgeType,
    val definition: String,
    val personalNotes: String,
    val example: String?,
    val categoryId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)