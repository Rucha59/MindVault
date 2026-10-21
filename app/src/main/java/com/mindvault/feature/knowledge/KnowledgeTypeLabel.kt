package com.mindvault.feature.knowledge

import com.mindvault.domain.model.KnowledgeType

fun KnowledgeType.label(): String = when (this) {
    KnowledgeType.WORD -> "Word"
    KnowledgeType.CONCEPT -> "Concept"
    KnowledgeType.OTHER -> "Other"
}