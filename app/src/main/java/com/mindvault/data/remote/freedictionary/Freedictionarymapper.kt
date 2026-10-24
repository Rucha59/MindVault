package com.mindvault.data.remote.freedictionary

import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.toTitleKey

const val FREE_DICTIONARY_SOURCE_NAME = "Free Dictionary API"

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Returns null when the entry has no word or no usable definition. */
fun FreeDictionaryEntryDto.toSearchResult(): SearchResult? {
    val title = word.cleaned() ?: return null
    val mappedMeanings = meanings.mapNotNull { meaning ->
        val definitions = meaning.definitions.mapNotNull { definition ->
            val text = definition.definition.cleaned() ?: return@mapNotNull null
            SearchDefinition(text = text, example = definition.example.cleaned())
        }
        if (definitions.isEmpty()) null else SearchMeaning(meaning.partOfSpeech.trim(), definitions)
    }
    if (mappedMeanings.isEmpty()) return null
    val pronunciation = phonetic.cleaned() ?: phonetics.firstNotNullOfOrNull { it.text.cleaned() }
    return SearchResult(
        title = title,
        pronunciation = pronunciation,
        meanings = mappedMeanings,
        sourceName = FREE_DICTIONARY_SOURCE_NAME,
    )
}

/** The API can return several entries for one word; they are merged so a word is one result. */
fun List<FreeDictionaryEntryDto>.toSearchResults(): List<SearchResult> =
    mapNotNull { it.toSearchResult() }
        .groupBy { it.title.toTitleKey() }
        .values
        .map { group ->
            group.first().copy(
                pronunciation = group.firstNotNullOfOrNull { it.pronunciation },
                meanings = group.flatMap { it.meanings },
            )
        }