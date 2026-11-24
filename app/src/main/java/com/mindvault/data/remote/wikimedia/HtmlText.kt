package com.mindvault.data.remote.wikimedia

/**
 * Turns the HTML fragments Wikimedia returns into plain text: tags removed, entities decoded, whitespace
 * collapsed. Plain Kotlin on purpose (android.text.Html returns null in local unit tests, and a parser library
 * is not worth a dependency for this).
 */
internal object HtmlText {
    private val BLOCK_BREAK = Regex("(?i)</(p|li|ul|ol|div)>|<br\\s*/?>")
    private val TAG = Regex("<[^>]*>")
    private val ENTITY = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);")
    private val WHITESPACE = Regex("\\s+")

    private val NAMED = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "ndash" to "\u2013",
        "mdash" to "\u2014",
        "hellip" to "\u2026",
        "lsquo" to "\u2018",
        "rsquo" to "\u2019",
        "ldquo" to "\u201C",
        "rdquo" to "\u201D",
    )

    fun toPlain(html: String): String {
        if (html.isEmpty()) return ""
        val withBreaks = BLOCK_BREAK.replace(html, " ")
        val withoutTags = TAG.replace(withBreaks, "")
        val decoded = ENTITY.replace(withoutTags) { match -> decode(match.groupValues[1]) ?: match.value }
        return WHITESPACE.replace(decoded, " ").trim()
    }

    private fun decode(body: String): String? {
        if (body.startsWith("#")) {
            val codePoint = if (body[1] == 'x' || body[1] == 'X') {
                body.substring(2).toIntOrNull(16)
            } else {
                body.substring(1).toIntOrNull()
            }
            return codePoint
                ?.takeIf { it in 1..0x10FFFF && it !in 0xD800..0xDFFF }
                ?.let { String(Character.toChars(it)) }
        }
        return NAMED[body]
    }
}