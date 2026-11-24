package com.mindvault.data.remote.wikimedia

import java.net.URLEncoder

const val WIKTIONARY_SOURCE_NAME = "Wiktionary"
const val WIKIPEDIA_SOURCE_NAME = "Wikipedia"

/** Wikipedia and Wiktionary text is available under this licence; it is shown with every result. */
const val WIKIMEDIA_LICENSE = "CC BY-SA 4.0"

const val WIKTIONARY_HOST = "en.wiktionary.org"
const val WIKIPEDIA_HOST = "en.wikipedia.org"

/** The public article URL for a title, for example https://en.wikipedia.org/wiki/Entropy_(information_theory). */
internal fun wikiPageUrl(host: String, title: String): String {
    val path = URLEncoder.encode(title.trim().replace(' ', '_'), "UTF-8")
        .replace("+", "%20")
        .replace("%28", "(")
        .replace("%29", ")")
        .replace("%2C", ",")
        .replace("%3A", ":")
    return "https://$host/wiki/$path"
}