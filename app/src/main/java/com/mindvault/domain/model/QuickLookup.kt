package com.mindvault.domain.model

/**
 * A word or phrase the user sent to MindVault from another app (selection menu or Share).
 * [id] is unique per arrival, so sending the same text twice still counts as a new lookup.
 */
data class QuickLookup(val id: Long, val text: String)

/** Cleans text that arrives from other apps. Pure Kotlin, so it is unit-tested without Android. */
object QuickLookupParser {

    /** Longest text kept; anything longer is cut to its first [MAX_LENGTH] characters. */
    const val MAX_LENGTH = 100

    /** Only the start of very large selections is looked at, to keep this cheap. */
    private const val MAX_SCAN = 2_000

    private val whitespace = Regex("[\\p{Z}\\s]+")
    private val link = Regex("(https?://|www\\.)\\S+", RegexOption.IGNORE_CASE)

    /** Straight and curly quotes, low-9 quotes and guillemets. */
    private const val QUOTES = "\"'\u201C\u201D\u201E\u201F\u2018\u2019\u201A\u201B\u00AB\u00BB\u2039\u203A"

    /**
     * Keeps only the selected word: removes links (many apps add the page address when sharing), trims and
     * collapses whitespace, removes quotes wrapped around the whole text, and caps the length. Returns null when
     * nothing is left to look up (null, blank, or only a link).
     */
    fun parse(raw: CharSequence?): String? {
        if (raw == null) return null
// Replace each link by a space, but keep any quote the link swallowed at its end, so the
// "wrapped in quotes" rule below still sees both quotes.
        val withoutLinks = link.replace(raw.subSequence(0, minOf(raw.length, MAX_SCAN))) { match ->
            " " + match.value.takeLastWhile { it in QUOTES }
        }
        val collapsed = whitespace.replace(withoutLinks, " ").trim()
        // "word" or “word”: remove quotes only when they wrap the whole text, so don't / dogs' are untouched.
        val unquoted =
            if (collapsed.length >= 2 && collapsed.first() in QUOTES && collapsed.last() in QUOTES) {
                collapsed.substring(1, collapsed.length - 1).trim()
            } else {
                collapsed
            }
        if (unquoted.isEmpty()) return null

        var capped = unquoted.take(MAX_LENGTH)
        // Never leave half of an emoji / surrogate pair at the cut.
        if (capped.isNotEmpty() && capped.last().isHighSurrogate()) capped = capped.dropLast(1)
        return capped.trim().ifEmpty { null }
    }
}