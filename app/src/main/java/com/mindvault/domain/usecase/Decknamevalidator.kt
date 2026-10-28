package com.mindvault.domain.usecase

import com.mindvault.domain.model.ValidationError

/** One rule for deck names, shared by create and rename. Duplicates are caught by the database. */
object DeckNameValidator {
    const val MAX_NAME_LENGTH = 50

    fun validate(name: String): ValidationError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.BLANK
            trimmed.length > MAX_NAME_LENGTH -> ValidationError.TOO_LONG
            else -> null
        }
    }
}