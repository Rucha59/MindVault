package com.mindvault.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesTest {

    @Test
    fun newestSearchGoesFirst() {
        assertEquals(listOf("b", "a"), RecentSearches.add(listOf("a"), "b"))
    }

    @Test
    fun repeatingASearch_movesItUp_ignoringCase_withoutDuplicates() {
        assertEquals(listOf("Entropy", "run"), RecentSearches.add(listOf("run", "entropy"), "Entropy"))
    }

    @Test
    fun onlyTheNewestFourAreKept() {
        val list = listOf("a", "b", "c", "d").fold(emptyList<String>()) { acc, q -> RecentSearches.add(acc, q) }
        assertEquals(listOf("d", "c", "b", "a"), list)

        assertEquals(listOf("e", "d", "c", "b"), RecentSearches.add(list, "e"))
        assertEquals(RecentSearches.MAX, RecentSearches.add(list, "e").size)
    }

    @Test
    fun blankSearches_areIgnored() {
        assertEquals(listOf("a"), RecentSearches.add(listOf("a"), "   "))
    }

    @Test
    fun whitespaceIsTrimmedAndCollapsed() {
        assertEquals(listOf("ice cream"), RecentSearches.add(emptyList(), "  ice    cream "))
    }
}