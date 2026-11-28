package com.mindvault.domain.usecase

import com.mindvault.domain.model.QuickLookup
import com.mindvault.domain.model.QuickLookupParser
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds at most one pending quick lookup between MainActivity (which receives it) and the Search screen
 * (which runs it). A newer lookup replaces an unconsumed older one. In memory only: it is never stored,
 * so a lookup that was not run is gone when the process ends.
 */
@Singleton
class QuickLookupInbox @Inject constructor() {

    private val _pending = MutableStateFlow<QuickLookup?>(null)
    val pending: StateFlow<QuickLookup?> = _pending.asStateFlow()

    private val nextId = AtomicLong(1)

    /** Cleans [raw] and makes it the pending lookup. Returns false (and changes nothing) if there is nothing to look up. */
    fun submit(raw: CharSequence?): Boolean {
        val text = QuickLookupParser.parse(raw) ?: return false
        _pending.value = QuickLookup(id = nextId.getAndIncrement(), text = text)
        return true
    }

    /**
     * Takes the lookup with [id] out of the inbox. Returns true only for the caller that got it, so the
     * lookup runs once even if two screens saw it. A stale [id] returns false and leaves the inbox alone.
     */
    fun consume(id: Long): Boolean {
        val current = _pending.value ?: return false
        if (current.id != id) return false
        return _pending.compareAndSet(current, null)
    }
}