package com.mindvault.data.remote.wikimedia

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Lenient parsing: unknown fields are ignored and nulls fall back to defaults. */
val WikimediaJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Wiktionary `page/definition/{term}`: a map of language code to sections; each section is one part of speech. */
@Serializable
data class WiktionarySectionDto(
    val partOfSpeech: String = "",
    val language: String = "",
    val definitions: List<WiktionaryDefinitionDto> = emptyList(),
)

/** `definition` and `examples` contain HTML. A definition with sub-senses embeds them in an `<ol>`. */
@Serializable
data class WiktionaryDefinitionDto(
    val definition: String = "",
    val examples: List<String> = emptyList(),
)

/** Wikipedia Core REST `search/page`. */
@Serializable
data class WikipediaSearchResponseDto(
    val pages: List<WikipediaPageHitDto> = emptyList(),
)

@Serializable
data class WikipediaPageHitDto(
    val key: String = "",
    val title: String = "",
    val description: String? = null,
)

/** Wikipedia `page/summary/{title}`. `type` is "standard", "disambiguation", "no-extract" or "mainpage". */
@Serializable
data class WikipediaSummaryDto(
    val type: String = "",
    val title: String = "",
    val description: String? = null,
    val extract: String? = null,
    @SerialName("content_urls") val contentUrls: WikipediaContentUrlsDto? = null,
)

@Serializable
data class WikipediaContentUrlsDto(
    val desktop: WikipediaPageUrlDto? = null,
)

@Serializable
data class WikipediaPageUrlDto(
    val page: String? = null,
)