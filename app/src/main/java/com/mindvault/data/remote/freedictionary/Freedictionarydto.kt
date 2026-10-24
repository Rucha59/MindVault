package com.mindvault.data.remote.freedictionary

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Lenient parsing: unknown fields are ignored and nulls fall back to defaults. */
val FreeDictionaryJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

@Serializable
data class FreeDictionaryEntryDto(
    val word: String = "",
    val phonetic: String? = null,
    val phonetics: List<FreeDictionaryPhoneticDto> = emptyList(),
    val meanings: List<FreeDictionaryMeaningDto> = emptyList(),
)

@Serializable
data class FreeDictionaryPhoneticDto(
    val text: String? = null,
)

@Serializable
data class FreeDictionaryMeaningDto(
    val partOfSpeech: String = "",
    val definitions: List<FreeDictionaryDefinitionDto> = emptyList(),
)

@Serializable
data class FreeDictionaryDefinitionDto(
    val definition: String = "",
    val example: String? = null,
)