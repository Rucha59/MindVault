package com.mindvault.data.remote.wikimedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the real response of https://en.wiktionary.org/w/api.php?action=opensearch&search=ent&limit=5&format=json */
class OpenSearchParserTest {

    private fun titles(json: String, dropDisambiguation: Boolean = false) =
        OpenSearchParser.titles(WikimediaJson.parseToJsonElement(json), dropDisambiguation)

    private val realResponse = """
        [
          "ent",
          ["ent", "enter", "entre", "entendre", "entender"],
          ["", "", "", "", ""],
          [
            "https://en.wiktionary.org/wiki/ent",
            "https://en.wiktionary.org/wiki/enter",
            "https://en.wiktionary.org/wiki/entre",
            "https://en.wiktionary.org/wiki/entendre",
            "https://en.wiktionary.org/wiki/entender"
          ]
        ]
    """.trimIndent()

    @Test
    fun readsTheTitlesOfTheRealResponse() {
        assertEquals(listOf("ent", "enter", "entre", "entendre", "entender"), titles(realResponse))
    }

    @Test
    fun noMatches_isAnEmptyList() {
        assertTrue(titles("""["zzqxv",[],[],[]]""").isEmpty())
    }

    @Test
    fun disambiguationTitles_areDroppedOnRequest() {
        val json = """["merc",["Mercury","Mercury (disambiguation)","Mercy"],[],[]]"""

        assertEquals(listOf("Mercury", "Mercury (disambiguation)", "Mercy"), titles(json))
        assertEquals(listOf("Mercury", "Mercy"), titles(json, dropDisambiguation = true))
    }

    @Test
    fun blankNonTextAndDuplicateEntries_areSkipped() {
        assertEquals(listOf("a", "b"), titles("""["x",[1, null, "a", " ", "b", "a"],[],[]]"""))
    }

    @Test
    fun anUnexpectedShape_givesNoTitlesInsteadOfAnError() {
        assertTrue(titles("""{"error":"x"}""").isEmpty())
        assertTrue(titles("""["only the query"]""").isEmpty())
        assertTrue(titles("""["x","not an array"]""").isEmpty())
        assertTrue(titles("[]").isEmpty())
    }
}