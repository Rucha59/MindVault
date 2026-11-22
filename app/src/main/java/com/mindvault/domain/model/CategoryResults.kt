package com.mindvault.domain.model

sealed interface RenameCategoryResult {
    data class Renamed(val category: Category) : RenameCategoryResult
    data class Invalid(val error: ValidationError) : RenameCategoryResult
    data object DuplicateName : RenameCategoryResult
    data object NotFound : RenameCategoryResult
}