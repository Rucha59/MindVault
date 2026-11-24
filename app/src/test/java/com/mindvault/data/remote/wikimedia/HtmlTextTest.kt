package com.mindvault.data.remote.wikimedia

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {

    @Test
    fun stripsTags_andKeepsTheTextBetweenThem() {
        val html = "<b>Run</b>, and you might still catch the train<span typeof=\"mw:Entity\">!</span>"
        assertEquals("Run, and you might still catch the train!", HtmlText.toPlain(html))
    }

    @Test
    fun removesEmptyLabelSpans_andTheSpaceTheyLeaveBehind() {
        val html = "<span class=\"usage-label-sense\" about=\"#mwt52\"></span> To <a href=\"/wiki/move\">move</a> forward."
        assertEquals("To move forward.", HtmlText.toPlain(html))
    }

    @Test
    fun decodesNamedAndNumericEntities() {
        assertEquals("a & b", HtmlText.toPlain("a &amp; b"))
        assertEquals("it's", HtmlText.toPlain("it&#039;s"))
        assertEquals("A", HtmlText.toPlain("&#x41;"))
        assertEquals("1 < 2 > 0", HtmlText.toPlain("1 &lt; 2 &gt; 0"))
    }

    @Test
    fun unknownEntities_andInvalidCodePoints_areLeftAsTheyAre() {
        assertEquals("&unknown; here", HtmlText.toPlain("&unknown; here"))
        assertEquals("&#0;", HtmlText.toPlain("&#0;"))
    }

    @Test
    fun escapedMarkup_isTextNotATag() {
        assertEquals("<b> is bold", HtmlText.toPlain("&lt;b&gt; is bold"))
    }

    @Test
    fun collapsesWhitespaceAndLineBreaks() {
        assertEquals("one two three", HtmlText.toPlain("  one \n two<br/>three \n"))
        assertEquals("a b", HtmlText.toPlain("<p>a</p><p>b</p>"))
    }

    @Test
    fun emptyAndTagOnlyInput_giveAnEmptyString() {
        assertEquals("", HtmlText.toPlain(""))
        assertEquals("", HtmlText.toPlain("<span></span> "))
    }
}