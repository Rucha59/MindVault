package com.mindvault.domain.model

enum class KnowledgeField { TITLE, DEFINITION, PERSONAL_NOTES, EXAMPLE }

enum class ValidationError { BLANK, TOO_LONG }

sealed interface SaveKnowledgeResult {
    data class Success(val item: KnowledgeItem) : SaveKnowledgeResult
    data class Invalid(val errors: Map<KnowledgeField, ValidationError>) : SaveKnowledgeResult
}

sealed interface CreateCategoryResult {
    data class Created(val category: Category) : CreateCategoryResult
    data class Invalid(val error: ValidationError) : CreateCategoryResult
}

sealed interface UpdateKnowledgeResult {
    data class Updated(val item: KnowledgeItem) : UpdateKnowledgeResult
    data class Invalid(val errors: Map<KnowledgeField, ValidationError>) : UpdateKnowledgeResult
    data object NotFound : UpdateKnowledgeResult
}