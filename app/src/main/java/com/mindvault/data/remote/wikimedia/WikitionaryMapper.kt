package com.mindvault.data.remote.wikimedia

import com.mindvault.domain.model.SearchDefinition
import com.mindvault.domain.model.SearchMeaning
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchResultKind

/**
 * Maps a Wiktionary definition response into one word result.
 * - Only the English section is kept (the response also lists other languages and symbols).
 * - Definitions are HTML: tags and entities are removed; empty definitions are dropped.
 * - A definition that embeds sub-senses (`<ol>`) keeps only its own text. The sub-senses also appear as separate
 *   entries, and the parent's example list is the union of theirs, so it is not used.
 * - Sections with the same part of speech are merged, so a word with several etymologies reads as one entry.
 * Wiktionary has no pronunciation, synonym or etymology field in this response, so none is shown.
 */
internal object WiktionaryMapper {
    private const val ENGLISH = "English"
    private const val MAX_DEFINITION_LENGTH = 1000
    private const val MAX_EXAMPLE_LENGTH = 500

    /** [term] is the spelling that was found, for example "run". Returns null when nothing usable is left. */
    fun toWordResult(term: String, response: Map<String, List<WiktionarySectionDto>>): SearchResult? {
        val title = term.trim()
        if (title.isEmpty()) return null

        val meanings = response.values
            .flatten()
            .filter { it.language.trim().equals(ENGLISH, ignoreCase = true) }
            .mapNotNull { section ->
                val definitions = section.definitions.mapNotNull { it.toDefinition() }
                if (definitions.isEmpty()) null else section.partOfSpeech.trim().lowercase() to definitions
            }
            .groupBy({ it.first }, { it.second })
            .map { (partOfSpeech, groups) ->
                SearchMeaning(partOfSpeech, groups.flatten().distinctBy { it.text })
            }
        if (meanings.isEmpty()) return null

        return SearchResult(
            title = title,
            pronunciation = null,
            meanings = meanings,
            sourceName = WIKTIONARY_SOURCE_NAME,
            kind = SearchResultKind.WORD,
            sourceUrl = wikiPageUrl(WIKTIONARY_HOST, title),
            license = WIKIMEDIA_LICENSE,
        )
    }

    private fun WiktionaryDefinitionDto.toDefinition(): SearchDefinition? {
        val subSenses = definition.indexOfAny(listOf("<ol", "<ul"), ignoreCase = true)
        val ownHtml = if (subSenses >= 0) definition.substring(0, subSenses) else definition
        val text = HtmlText.toPlain(ownHtml).take(MAX_DEFINITION_LENGTH)
        if (text.isEmpty()) return null
        val example = if (subSenses >= 0) {
            null
        } else {
            examples.firstNotNullOfOrNull { HtmlText.toPlain(it).takeIf { plain -> plain.isNotEmpty() } }
                ?.take(MAX_EXAMPLE_LENGTH)
        }
        return SearchDefinition(text = text, example = example)
    }
}