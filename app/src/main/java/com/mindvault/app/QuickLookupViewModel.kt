package com.mindvault.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.domain.usecase.QuickLookupInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Tells the navigation host that a quick lookup is waiting, so it can open Search. It only exposes the ID and
 * never consumes the lookup: the Search screen does that once it is on screen.
 */
@HiltViewModel
class QuickLookupViewModel @Inject constructor(
    inbox: QuickLookupInbox,
) : ViewModel() {

    /** Eagerly, with the current value, so a lookup that arrived before the first screen is not missed. */
    val pendingId: StateFlow<Long?> = inbox.pending
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, inbox.pending.value?.id)
}