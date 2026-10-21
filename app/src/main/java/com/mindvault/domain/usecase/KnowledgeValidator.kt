package com.mindvault.domain.usecase

import com.mindvault.domain.model.KnowledgeField
import com.mindvault.domain.model.ValidationError

/** One set of rules for creating and editing, so the two can't drift apart. */
internal fun validateKnowledgeFields(
    title: String,
    definition: String,
    personalNotes: String,
    example: String?,
): Map<KnowledgeField, ValidationError> {
    val errors = mutableMapOf<KnowledgeField, ValidationError>()
    requiredError(title, SaveKnowledgeItemUseCase.MAX_TITLE_LENGTH)
        ?.let { errors[KnowledgeField.TITLE] = it }
    requiredError(definition, SaveKnowledgeItemUseCase.MAX_DEFINITION_LENGTH)
        ?.let { errors[KnowledgeField.DEFINITION] = it }
    lengthError(personalNotes, SaveKnowledgeItemUseCase.MAX_NOTES_LENGTH)
        ?.let { errors[KnowledgeField.PERSONAL_NOTES] = it }
    lengthError(example.orEmpty(), SaveKnowledgeItemUseCase.MAX_EXAMPLE_LENGTH)
        ?.let { errors[KnowledgeField.EXAMPLE] = it }
    return errors
}

private fun requiredError(value: String, max: Int): ValidationError? {
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> ValidationError.BLANK
        trimmed.length > max -> ValidationError.TOO_LONG
        else -> null
    }
}

private fun lengthError(value: String, max: Int): ValidationError? =
    if (value.trim().length > max) ValidationError.TOO_LONG else null