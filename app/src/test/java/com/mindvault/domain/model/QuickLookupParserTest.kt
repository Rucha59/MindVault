package com.mindvault.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickLookupParserTest {

    @Test
    fun trimsAndCollapsesWhitespace() {
        assertEquals("ephemeral nature", QuickLookupParser.parse("  ephemeral \n\t nature  "))
    }

    @Test
    fun collapsesNonBreakingSpaces() {
        assertEquals("a b", QuickLookupParser.parse("a\u00A0\u00A0b"))
    }

    @Test
    fun nullAndBlankAreIgnored() {
        assertNull(QuickLookupParser.parse(null))
        assertNull(QuickLookupParser.parse(""))
        assertNull(QuickLookupParser.parse(" \n\t\u00A0 "))
    }

    @Test
    fun aBareLinkIsIgnored() {
        assertNull(QuickLookupParser.parse("https://example.com/some/page"))
        assertNull(QuickLookupParser.parse("  www.example.com  "))
    }

    @Test
    fun aLinkSharedWithTheWordIsRemoved() {
        assertEquals(
            "entropy",
            QuickLookupParser.parse("\u201Centropy\u201D https://en.wikipedia.org/wiki/Entropy#:~:text=entropy"),
        )
        assertEquals("entropy", QuickLookupParser.parse("entropy\n\nhttps://example.com/page"))
        assertEquals("entropy", QuickLookupParser.parse("entropy www.example.com"))
    }

    @Test
    fun aLinkInTheMiddleIsRemovedAndTheRestKept() {
        assertEquals("see now", QuickLookupParser.parse("see https://example.com now"))
    }

    @Test
    fun quotesAroundTheWholeTextAreRemoved() {
        assertEquals("entropy", QuickLookupParser.parse("\"entropy\""))
        assertEquals("entropy", QuickLookupParser.parse("\u201Centropy\u201D"))
        assertEquals("rock and roll", QuickLookupParser.parse("\u00AB rock and roll \u00BB"))
    }

    @Test
    fun apostrophesInsideAWordAreKept() {
        assertEquals("don't", QuickLookupParser.parse("don't"))
        assertEquals("dogs'", QuickLookupParser.parse("dogs'"))
    }

    @Test
    fun aQuotedLinkAloneIsIgnored() {
        assertNull(QuickLookupParser.parse("\u201Chttps://example.com\u201D"))
        assertNull(QuickLookupParser.parse("\"\""))
    }

    @Test
    fun aSingleWordIsKeptAsIs() {
        assertEquals("entropy", QuickLookupParser.parse("entropy"))
    }

    @Test
    fun longTextIsCutToTheFirstHundredCharacters() {
        val result = QuickLookupParser.parse("a".repeat(150))
        assertEquals("a".repeat(QuickLookupParser.MAX_LENGTH), result)
    }

    @Test
    fun theCutDoesNotSplitASurrogatePair() {
        // 99 letters + one emoji (two chars): the cut at 100 would leave half the emoji.
        assertEquals("a".repeat(99), QuickLookupParser.parse("a".repeat(99) + "\uD83D\uDE00"))
    }

    @Test
    fun theCutDoesNotLeaveATrailingSpace() {
        assertEquals("a".repeat(99), QuickLookupParser.parse("a".repeat(99) + " b"))
    }
}