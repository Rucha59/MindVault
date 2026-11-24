package com.mindvault.data.remote.wikimedia

import com.mindvault.domain.model.SearchLimits
import com.mindvault.domain.model.SearchResultKind
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the shape of the real responses of the Wikipedia search and summary endpoints (shortened). */
class WikipediaMapperTest {

    private val searchJson = """
        {"pages":[
          {"id":9891,"key":"Entropy","title":"Entropy",
           "excerpt":"<span class=\"searchmatch\">Entropy</span> is a thermodynamic state variable",
           "matched_title":null,"anchor":null,"description":"Property of a thermodynamic system",
           "thumbnail":{"mimetype":"image/svg+xml","width":60,"height":26,"duration":null,"url":"//thumb.wikimedia.org/x.png"}},
          {"id":15445,"key":"Entropy_(information_theory)","title":"Entropy (information theory)",
           "excerpt":"In information theory, the entropy of a random variable","matched_title":null,"anchor":null,
           "description":"Average uncertainty in variable's states","thumbnail":null},
          {"id":302133,"key":"Entropy_(disambiguation)","title":"Entropy (disambiguation)",
           "excerpt":"Look up entropy in Wiktionary","matched_title":null,"anchor":null,
           "description":"Topics referred to by the same term","thumbnail":null},
          {"id":19007,"key":"Mercury","title":"Mercury","excerpt":"Mercury most commonly refers to","matched_title":null,
           "anchor":null,"description":"Topics referred to by the same term","thumbnail":null},
          {"id":1,"key":"No_description","title":"No description","excerpt":"x","matched_title":null,"anchor":null,
           "description":null,"thumbnail":null},
          {"id":1735250,"key":"Cross-entropy","title":"Cross-entropy","excerpt":"In information theory","matched_title":null,
           "anchor":null,"description":"Information-theoretic measure","thumbnail":null}
        ]}
    """.trimIndent()

    private val entropySummaryJson = """
        {"type":"standard","title":"Entropy","displaytitle":"Entropy","namespace":{"id":0,"text":""},"wikibase_item":"Q45003",
         "titles":{"canonical":"Entropy","normalized":"Entropy"},"pageid":9891,
         "thumbnail":{"source":"https://thumb.wikimedia.org/x.png","width":330,"height":145},
         "lang":"en","dir":"ltr","description":"Property of a thermodynamic system","description_source":"local",
         "content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Entropy","revisions":"r","edit":"e","talk":"t"},
                         "mobile":{"page":"https://en.m.wikipedia.org/wiki/Entropy"}},
         "extract":"Entropy is a thermodynamic state variable that quantifies the probabilistic distribution of accessible microstates in a system.",
         "extract_html":"<p><b>Entropy</b> is a thermodynamic state variable</p>"}
    """.trimIndent()

    private val mercurySummaryJson = """
        {"type":"disambiguation","title":"Mercury","pageid":19007,"description":"Topics referred to by the same term",
         "content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Mercury"}},
         "extract":"Mercury most commonly refers to:Mercury (planet), the closest planet to the Sun"}
    """.trimIndent()

    private fun hits() = WikipediaMapper.toHits(WikimediaJson.decodeFromString<WikipediaSearchResponseDto>(searchJson))
    private fun summary(json: String) = WikimediaJson.decodeFromString<WikipediaSummaryDto>(json)

    @Test
    fun searchHits_dropDisambiguationPages_byTitleAndByDescription() {
        assertEquals(
            listOf("Entropy", "Entropy_(information_theory)", "No_description", "Cross-entropy"),
            hits().map { it.key },
        )
    }

    @Test
    fun searchHits_keepTheShortDescription_andAllowNone() {
        val hits = hits()
        assertEquals("Property of a thermodynamic system", hits[0].description)
        assertNull(hits.first { it.key == "No_description" }.description)
    }

    @Test
    fun hitWithoutSummary_becomesALazyConceptCardWithALink() {
        val lazy = WikipediaMapper.toLazyResult(hits()[1])

        assertEquals("Entropy (information theory)", lazy.title)
        assertEquals(SearchResultKind.CONCEPT, lazy.kind)
        assertNull(lazy.summary)
        assertTrue(lazy.needsSummary)
        assertEquals("Average uncertainty in variable's states", lazy.description)
        assertEquals("https://en.wikipedia.org/wiki/Entropy_(information_theory)", lazy.sourceUrl)
        assertEquals("Wikipedia", lazy.sourceName)
        assertEquals("CC BY-SA 4.0", lazy.license)
    }

    @Test
    fun standardSummary_becomesAConceptResultWithItsExtractAndArticleLink() {
        val result = WikipediaMapper.toConceptResult(summary(entropySummaryJson), fallbackDescription = null)!!

        assertEquals("Entropy", result.title)
        assertEquals("Property of a thermodynamic system", result.description)
        assertTrue(result.summary!!.startsWith("Entropy is a thermodynamic state variable"))
        assertEquals("https://en.wikipedia.org/wiki/Entropy", result.sourceUrl)
        assertEquals(false, result.needsSummary)
    }

    @Test
    fun disambiguationSummary_isDropped() {
        assertNull(WikipediaMapper.toConceptResult(summary(mercurySummaryJson), fallbackDescription = null))
    }

    @Test
    fun summaryWithoutText_orWithAnotherType_isDropped() {
        val noText = summary(entropySummaryJson).copy(extract = "  ")
        val noExtractType = summary(entropySummaryJson).copy(type = "no-extract")

        assertNull(WikipediaMapper.toConceptResult(noText, null))
        assertNull(WikipediaMapper.toConceptResult(noExtractType, null))
    }

    @Test
    fun missingDescriptionFallsBackToTheSearchHitsDescription() {
        val dto = summary(entropySummaryJson).copy(description = null)

        assertEquals("From the hit", WikipediaMapper.toConceptResult(dto, "From the hit")!!.description)
    }

    @Test
    fun missingContentUrl_isBuiltFromTheTitle() {
        val dto = summary(entropySummaryJson).copy(contentUrls = null)

        assertEquals(
            "https://en.wikipedia.org/wiki/Entropy",
            WikipediaMapper.toConceptResult(dto, null)!!.sourceUrl,
        )
    }

    @Test
    fun veryLongSummary_isCutAtAWordBoundaryWithinTheLimit() {
        val dto = summary(entropySummaryJson).copy(extract = "word ".repeat(2000))

        val text = WikipediaMapper.toConceptResult(dto, null)!!.summary!!

        assertTrue(text.length <= SearchLimits.MAX_SUMMARY_LENGTH)
        assertTrue(text.endsWith("word\u2026"))
    }
}