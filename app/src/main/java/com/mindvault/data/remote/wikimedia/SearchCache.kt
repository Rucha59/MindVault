package com.mindvault.data.remote.wikimedia

/**
 * Small in-memory cache: a repeated search or summary inside the same session does not call Wikimedia again.
 * Entries expire after [ttlMillis]; the least recently used entry is dropped beyond [maxEntries].
 */
internal class SearchCache<V : Any>(
    private val maxEntries: Int = 50,
    private val ttlMillis: Long = 5 * 60 * 1000L,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private class Entry<T>(val value: T, val storedAt: Long)

    private val entries = object : LinkedHashMap<String, Entry<V>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry<V>>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(key: String): V? {
        val entry = entries[key] ?: return null
        if (now() - entry.storedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: String, value: V) {
        entries[key] = Entry(value, now())
    }
}