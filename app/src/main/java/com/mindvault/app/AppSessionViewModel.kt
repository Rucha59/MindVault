package com.mindvault.app

import androidx.lifecycle.ViewModel
import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.usecase.ObserveDataOwnerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Activity-level holder of the active vault owner, read by the nav host to reset navigation on a change. */
@HiltViewModel
class AppSessionViewModel @Inject constructor(
    observeDataOwner: ObserveDataOwnerUseCase,
) : ViewModel() {
    val owner: StateFlow<DataOwner> = observeDataOwner()
}