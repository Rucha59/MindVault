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

/** What a result is: a dictionary word (Wiktionary) or an encyclopedia concept (Wikipedia). */
enum class SearchResultKind { WORD, CONCEPT }

/**
 * A provider-independent Search result, shown as one card.
 * Providers map their own responses into this; nothing outside `data/remote` sees provider models.
 *
 * Words use [meanings]. Concepts use [description] (one short line) and [summary] (the article's opening text).
 * A concept can arrive without a [summary]; it is loaded when the card is opened or saved (see [needsSummary]).
 * [sourceUrl] and [license] are shown on the card only (a footnote). They are NOT part of the saved text:
 * the definition is also shown on flashcards, where a source line is not wanted.
 */
data class SearchResult(
    val title: String,
    val pronunciation: String?,
    val meanings: List<SearchMeaning>,
    val sourceName: String,
    val kind: SearchResultKind = SearchResultKind.WORD,
    val description: String? = null,
    val summary: String? = null,
    val sourceUrl: String? = null,
    val license: String? = null,
) {
    /** True for a concept whose summary has not been loaded yet. It cannot be saved until it is. */
    val needsSummary: Boolean get() = kind == SearchResultKind.CONCEPT && summary.isNullOrBlank()
}

/** What a result becomes when it is saved, or opened in the form to edit first. */
data class SearchSaveContent(
    val title: String,
    val definition: String,
    val example: String?,
    val type: KnowledgeType = KnowledgeType.WORD,
)

object SearchLimits {
    const val MAX_QUERY_LENGTH = 100
    const val MAX_MEANINGS = 4
    const val MAX_DEFINITIONS_PER_MEANING = 2

    /** Keeps the saved concept text well under the 5000-character definition limit. */
    const val MAX_SUMMARY_LENGTH = 4000

    /** Autocomplete: suggestions start after this many characters, once typing has paused this long. */
    const val MIN_SUGGESTION_LENGTH = 2
    const val SUGGESTION_DEBOUNCE_MILLIS = 300L
    const val MAX_SUGGESTIONS = 6
}

/** Case-insensitive key used to decide whether a result is already saved. */
fun String.toTitleKey(): String = trim().lowercase()

fun SearchResultKind.toKnowledgeType(): KnowledgeType = when (this) {
    SearchResultKind.WORD -> KnowledgeType.WORD
    SearchResultKind.CONCEPT -> KnowledgeType.CONCEPT
}

fun SearchResult.knowledgeType(): KnowledgeType = kind.toKnowledgeType()

/**
 * Key of a saved item or a result: type plus case-insensitive title. A word and a concept with the same
 * title have different keys, so both can be saved.
 */
fun savedItemKey(title: String, type: KnowledgeType): String = "${type.name}:${title.toTitleKey()}"

fun SearchResult.savedItemKey(): String = savedItemKey(title, knowledgeType())

/** Caps what is shown and saved so a word with dozens of senses stays readable. */
fun SearchResult.limited(): SearchResult = copy(
    meanings = meanings
        .take(SearchLimits.MAX_MEANINGS)
        .map { it.copy(definitions = it.definitions.take(SearchLimits.MAX_DEFINITIONS_PER_MEANING)) },
)

/** Flattens a result into the fields of a knowledge item. Uses exactly what the card shows, without the source footnote. */
fun SearchResult.toSaveContent(): SearchSaveContent {
    val shown = limited()
    val body: String
    val example: String?
    when (kind) {
        SearchResultKind.WORD -> {
            val lines = shown.meanings.flatMap { meaning ->
                meaning.definitions.map { definition ->
                    if (meaning.partOfSpeech.isBlank()) definition.text else "(${meaning.partOfSpeech}) ${definition.text}"
                }
            }
            body = lines.joinToString("\n")
            example = shown.meanings
                .flatMap { it.definitions }
                .firstNotNullOfOrNull { it.example }
        }
        SearchResultKind.CONCEPT -> {
            body = summary.orEmpty().trim()
            example = null
        }
    }
    return SearchSaveContent(
        title = title.trim(),
        definition = body,
        example = example,
        type = knowledgeType(),
    )
}