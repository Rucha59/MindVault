package com.mindvault.domain.model

/** One definition of a word, with an optional usage example. */
data class SearchDefinition(
    val text: String,
    val example: String? = null,
)

/** The definitions a word has under one part of speech (for example "noun"). */
data class SearchMeaning(
    val partOfSpeech: String,
    val definitions: List<SearchDefinition>,
)

/**
 * A provider-independent Search result: one word, shown as one card.
 * Providers map their own responses into this; nothing outside `data/remote` sees provider models.
 */
data class SearchResult(
    val title: String,
    val pronunciation: String?,
    val meanings: List<SearchMeaning>,
    val sourceName: String,
)

/** What a result becomes when it is saved, or opened in the form to edit first. */
data class SearchSaveContent(
    val title: String,
    val definition: String,
    val example: String?,
)

object SearchLimits {
    const val MAX_QUERY_LENGTH = 100
    const val MAX_MEANINGS = 4
    const val MAX_DEFINITIONS_PER_MEANING = 2
}

/** Case-insensitive key used to decide whether a result is already saved. */
fun String.toTitleKey(): String = trim().lowercase()

/** Caps what is shown and saved so a word with dozens of senses stays readable. */
fun SearchResult.limited(): SearchResult = copy(
    meanings = meanings
        .take(SearchLimits.MAX_MEANINGS)
        .map { it.copy(definitions = it.definitions.take(SearchLimits.MAX_DEFINITIONS_PER_MEANING)) },
)

/** Flattens a result into the fields of a knowledge item. Uses exactly what the card shows. */
fun SearchResult.toSaveContent(): SearchSaveContent {
    val shown = limited()
    val lines = shown.meanings.flatMap { meaning ->
        meaning.definitions.map { definition ->
            if (meaning.partOfSpeech.isBlank()) definition.text else "(${meaning.partOfSpeech}) ${definition.text}"
        }
    }
    val example = shown.meanings
        .flatMap { it.definitions }
        .firstNotNullOfOrNull { it.example }
    return SearchSaveContent(
        title = title.trim(),
        definition = lines.joinToString("\n"),
        example = example,
    )
}