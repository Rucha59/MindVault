package com.mindvault.data.remote.wikimedia

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchResult
import com.mindvault.domain.model.SearchResultKind

/** One Wikipedia search hit: enough for a compact card; the summary is loaded separately. */
internal data class ConceptHit(
    val key: String,
    val title: String,
    val description: String?,
)

/**
 * Maps Wikipedia responses into concept results.
 * Disambiguation pages (a list of meanings, not an explanation) are dropped, both from search hits and from
 * summaries, because there is nothing meaningful to save from them.
 */
internal object WikipediaMapper {
    private val DISAMBIGUATION_DESCRIPTIONS = setOf(
        "topics referred to by the same term",
        "wikimedia disambiguation page",
    )

    fun toHits(response: WikipediaSearchResponseDto): List<ConceptHit> =
        response.pages
            .mapNotNull { page ->
                val key = page.key.trim()
                val title = page.title.trim()
                if (key.isEmpty() || title.isEmpty()) return@mapNotNull null
                if (title.endsWith("(disambiguation)", ignoreCase = true)) return@mapNotNull null
                val description = page.description?.let { HtmlText.toPlain(it) }?.takeIf { it.isNotEmpty() }
                if (description != null && description.lowercase() in DISAMBIGUATION_DESCRIPTIONS) {
                    return@mapNotNull null
                }
                ConceptHit(key = key, title = title, description = description)
            }
            .distinctBy { it.key }

    /** A concept result without a summary; it is completed when the card is opened or saved. */
    fun toLazyResult(hit: ConceptHit): SearchResult = SearchResult(
        title = hit.title,
        pronunciation = null,
        meanings = emptyList(),
        sourceName = WIKIPEDIA_SOURCE_NAME,
        kind = SearchResultKind.CONCEPT,
        description = hit.description,
        summary = null,
        sourceUrl = wikiPageUrl(WIKIPEDIA_HOST, hit.key),
        license = WIKIMEDIA_LICENSE,
    )

    /** Returns null for disambiguation pages, pages without text and anything that is not a normal article. */
    fun toConceptResult(dto: WikipediaSummaryDto, fallbackDescription: String?): SearchResult? {
        if (!dto.type.trim().equals("standard", ignoreCase = true)) return null
        val summary = dto.extract?.trim().orEmpty()
        if (summary.isEmpty()) return null
        val title = dto.title.trim()
        if (title.isEmpty()) return null
        return SearchResult(
            title = title,
            pronunciation = null,
            meanings = emptyList(),
            sourceName = WIKIPEDIA_SOURCE_NAME,
            kind = SearchResultKind.CONCEPT,
            description = dto.description?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackDescription,
            summary = limitSummary(summary),
            sourceUrl = dto.contentUrls?.desktop?.page?.takeIf { it.startsWith("https://") }
                ?: wikiPageUrl(WIKIPEDIA_HOST, title),
            license = WIKIMEDIA_LICENSE,
        )
    }

    private fun limitSummary(text: String): String {
        val max = SearchLimits.MAX_SUMMARY_LENGTH
        if (text.length <= max) return text
        val cut = text.substring(0, max - 1)
        val atWord = cut.lastIndexOf(' ').takeIf { it > max / 2 } ?: cut.length
        return cut.substring(0, atWord).trimEnd() + "\u2026"
    }
}