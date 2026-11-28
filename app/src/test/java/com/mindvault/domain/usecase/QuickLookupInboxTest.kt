package com.mindvault.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickLookupInboxTest {

    @Test
    fun startsEmpty() {
        assertNull(QuickLookupInbox().pending.value)
    }

    @Test
    fun submitStoresTheCleanedText() {
        val inbox = QuickLookupInbox()
        assertTrue(inbox.submit("  entropy \n"))
        assertEquals("entropy", inbox.pending.value?.text)
    }

    @Test
    fun blankTextIsRejectedAndKeepsTheExistingLookup() {
        val inbox = QuickLookupInbox()
        inbox.submit("entropy")
        assertFalse(inbox.submit("   "))
        assertFalse(inbox.submit(null))
        assertEquals("entropy", inbox.pending.value?.text)
    }

    @Test
    fun aNewerLookupReplacesAnOlderOne() {
        val inbox = QuickLookupInbox()
        inbox.submit("first")
        inbox.submit("second")
        assertEquals("second", inbox.pending.value?.text)
    }

    @Test
    fun theSameTextTwiceGetsADifferentId() {
        val inbox = QuickLookupInbox()
        inbox.submit("entropy")
        val first = inbox.pending.value!!.id
        inbox.submit("entropy")
        assertNotEquals(first, inbox.pending.value!!.id)
    }

    @Test
    fun consumeTakesTheLookupOnlyOnce() {
        val inbox = QuickLookupInbox()
        inbox.submit("entropy")
        val id = inbox.pending.value!!.id
        assertTrue(inbox.consume(id))
        assertNull(inbox.pending.value)
        assertFalse(inbox.consume(id))
    }

    @Test
    fun aStaleIdCannotConsumeANewerLookup() {
        val inbox = QuickLookupInbox()
        inbox.submit("first")
        val staleId = inbox.pending.value!!.id
        inbox.submit("second")
        assertFalse(inbox.consume(staleId))
        assertEquals("second", inbox.pending.value?.text)
    }
}