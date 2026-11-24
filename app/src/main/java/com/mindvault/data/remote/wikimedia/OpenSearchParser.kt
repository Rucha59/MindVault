package com.mindvault.data.remote.wikimedia

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads a MediaWiki `action=opensearch` response: `[query, [titles], [descriptions], [urls]]`. Only the titles
 * are used. Anything that does not look like that (wrong shape, non-text entries) yields fewer or no titles
 * instead of an error.
 */
internal object OpenSearchParser {

    fun titles(response: JsonElement, dropDisambiguation: Boolean = false): List<String> {
        val titles = (response as? JsonArray)?.getOrNull(1) as? JsonArray ?: return emptyList()
        return titles
            .mapNotNull { element ->
                (element as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
            }
            .filter { !dropDisambiguation || !it.endsWith("(disambiguation)", ignoreCase = true) }
            .distinct()
    }
}