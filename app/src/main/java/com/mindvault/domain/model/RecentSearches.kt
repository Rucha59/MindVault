package com.mindvault.domain.model

/** The last few searches, newest first. Pure logic; storage is up to the settings repository. */
object RecentSearches {
    const val MAX = 4

    private val WHITESPACE = Regex("\\s+")

    /**
     * [query] goes to the front. An earlier entry with the same text (ignoring case) is removed, so a repeated
     * search just moves up. Blank queries are ignored. Only the newest [MAX] are kept.
     */
    fun add(current: List<String>, query: String): List<String> {
        val normalized = query.trim().replace(WHITESPACE, " ")
        if (normalized.isEmpty()) return current
        return (listOf(normalized) + current.filterNot { it.equals(normalized, ignoreCase = true) }).take(MAX)
    }
}