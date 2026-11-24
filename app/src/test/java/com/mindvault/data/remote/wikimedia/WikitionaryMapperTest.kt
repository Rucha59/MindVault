package com.mindvault.data.remote.wikimedia

import com.mindvault.domain.model.SearchResultKind
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the shape of the real response of https://en.wiktionary.org/api/rest_v1/page/definition/run (shortened). */
class WiktionaryMapperTest {

    private val runJson = """
        {"en":[
          {"partOfSpeech":"Symbol","language":"Translingual","definitions":[
            {"definition":"\u003Ci\u003E\u003Ca href=\"https://en.wikipedia.org/wiki/ISO%20639-2\"\u003EISO 639-2\u003C/a\u003E\u003Clink rel=\"mw:PageProp/Category\" href=\"./Category:ISO_639-2\"\u003E &amp; \u003Ca href=\"x\"\u003EISO 639-3\u003C/a\u003E language code for \u003C/i\u003E\u003Cb\u003EKirundi\u003C/b\u003E."}
          ]},
          {"partOfSpeech":"Verb","language":"English","definitions":[
            {"definition":"To move \u003Ca rel=\"mw:WikiLink\" href=\"/wiki/swiftly\" title=\"swiftly\"\u003Eswiftly\u003C/a\u003E.\n\u003Col\u003E\u003Cli\u003E\u003Cspan class=\"usage-label-sense\" about=\"#mwt52\" typeof=\"mw:Transclusion\"\u003E\u003C/span\u003E To \u003Ca href=\"/wiki/move\"\u003Emove\u003C/a\u003E forward \u003Ca href=\"/wiki/quickly\"\u003Equickly\u003C/a\u003E upon \u003Ca href=\"/wiki/two\"\u003Etwo\u003C/a\u003E \u003Ca href=\"/wiki/feet\"\u003Efeet\u003C/a\u003E.",
             "parsedExamples":[{"example":"x"}],
             "examples":["\u003Cb\u003ERun\u003C/b\u003E, and you might still catch the train\u003Cspan typeof=\"mw:Entity\"\u003E!\u003C/span\u003E","I have been \u003Cb\u003Erunning\u003C/b\u003E all over the building looking for him."]},
            {"definition":"\u003Cspan class=\"usage-label-sense\" about=\"#mwt52\" typeof=\"mw:Transclusion\"\u003E\u003C/span\u003E To \u003Ca href=\"/wiki/move\"\u003Emove\u003C/a\u003E forward \u003Ca href=\"/wiki/quickly\"\u003Equickly\u003C/a\u003E upon \u003Ca href=\"/wiki/two\"\u003Etwo\u003C/a\u003E \u003Ca href=\"/wiki/feet\"\u003Efeet\u003C/a\u003E.",
             "examples":["\u003Cb\u003ERun\u003C/b\u003E, and you might still catch the train\u003Cspan typeof=\"mw:Entity\"\u003E!\u003C/span\u003E"]},
            {"definition":""},
            {"definition":"\u003Cspan class=\"usage-label-sense\"\u003E\u003C/span\u003E To \u003Ca href=\"/wiki/go\"\u003Ego\u003C/a\u003E at a \u003Ca href=\"/wiki/fast\"\u003Efast\u003C/a\u003E pace.",
             "examples":["I have been \u003Cb\u003Erunning\u003C/b\u003E all over the building looking for him."]}
          ]},
          {"partOfSpeech":"Noun","language":"English","definitions":[
            {"definition":"An act of \u003Cb\u003Erunning\u003C/b\u003E.","examples":["a \u003Cb\u003Erun\u003C/b\u003E in the park"]}
          ]},
          {"partOfSpeech":"Verb","language":"English","definitions":[{"definition":"To flow."}]}
        ]}
    """.trimIndent()

    private fun runResponse() = WikimediaJson.decodeFromString<Map<String, List<WiktionarySectionDto>>>(runJson)

    @Test
    fun keepsOnlyTheEnglishSections() {
        val result = WiktionaryMapper.toWordResult("run", runResponse())!!

        assertEquals(listOf("verb", "noun"), result.meanings.map { it.partOfSpeech })
        assertTrue(result.meanings.flatMap { it.definitions }.none { it.text.contains("Kirundi") })
    }

    @Test
    fun definitionsAreCleanPlainText_withoutEmptyOnesOrDuplicatedSubSenses() {
        val verb = WiktionaryMapper.toWordResult("run", runResponse())!!.meanings.first { it.partOfSpeech == "verb" }

        assertEquals(
            listOf(
                "To move swiftly.",
                "To move forward quickly upon two feet.",
                "To go at a fast pace.",
                "To flow.", // from the second Verb section, merged into the first
            ),
            verb.definitions.map { it.text },
        )
    }

    @Test
    fun aDefinitionWithSubSenses_getsNoExample_butSubSensesKeepTheirOwn() {
        val verb = WiktionaryMapper.toWordResult("run", runResponse())!!.meanings.first { it.partOfSpeech == "verb" }

        assertNull(verb.definitions[0].example)
        assertEquals("Run, and you might still catch the train!", verb.definitions[1].example)
        assertEquals("I have been running all over the building looking for him.", verb.definitions[2].example)
    }

    @Test
    fun resultCarriesSourceNameLinkAndLicence_andNoInventedFields() {
        val result = WiktionaryMapper.toWordResult("run", runResponse())!!

        assertEquals("run", result.title)
        assertEquals(SearchResultKind.WORD, result.kind)
        assertEquals("Wiktionary", result.sourceName)
        assertEquals("https://en.wiktionary.org/wiki/run", result.sourceUrl)
        assertEquals("CC BY-SA 4.0", result.license)
        assertNull(result.pronunciation)
        assertNull(result.summary)
    }

    @Test
    fun responseWithoutEnglishDefinitions_isNoResult() {
        val onlySymbol = runResponse().mapValues { (_, sections) -> sections.filter { it.language == "Translingual" } }

        assertNull(WiktionaryMapper.toWordResult("run", onlySymbol))
        assertNull(WiktionaryMapper.toWordResult("run", emptyMap()))
    }

    @Test
    fun blankTerm_isNoResult() {
        assertNull(WiktionaryMapper.toWordResult("  ", runResponse()))
    }

    @Test
    fun termsWithSpacesOrSymbols_getAnEncodedLink() {
        val result = WiktionaryMapper.toWordResult("ice cream", runResponse())!!
        assertEquals("https://en.wiktionary.org/wiki/ice_cream", result.sourceUrl)
    }
}