package com.mindvault.data.remote.wikimedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchCacheTest {

    private var clock = 0L

    @Test
    fun returnsWhatWasStored_untilItExpires() {
        val cache = SearchCache<String>(maxEntries = 10, ttlMillis = 100) { clock }
        cache.put("a", "1")

        clock = 100
        assertEquals("1", cache.get("a"))
        clock = 101
        assertNull(cache.get("a"))
    }

    @Test
    fun dropsTheLeastRecentlyUsedEntry_beyondTheLimit() {
        val cache = SearchCache<String>(maxEntries = 2, ttlMillis = 1_000) { clock }
        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a") // "a" is now more recent than "b"
        cache.put("c", "3")

        assertEquals("1", cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals("3", cache.get("c"))
    }

    @Test
    fun unknownKey_isNull() {
        assertNull(SearchCache<String>().get("missing"))
    }
}