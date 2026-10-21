package com.mindvault.domain.usecase

import com.mindvault.domain.model.CreateCategoryResult
import com.mindvault.domain.model.ValidationError
import com.mindvault.domain.repository.CategoryRepository
import javax.inject.Inject

/** Validates the name, then returns the existing category with that name or creates a new one. */
class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(name: String): CreateCategoryResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> CreateCategoryResult.Invalid(ValidationError.BLANK)
            trimmed.length > MAX_NAME_LENGTH -> CreateCategoryResult.Invalid(ValidationError.TOO_LONG)
            else -> CreateCategoryResult.Created(repository.create(trimmed))
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}