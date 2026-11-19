package com.mindvault.domain.usecase

import com.mindvault.domain.model.DataOwner
import com.mindvault.domain.repository.AccountDataRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveDataOwnerUseCase @Inject constructor(private val repository: AccountDataRepository) {
    operator fun invoke(): StateFlow<DataOwner> = repository.owner
}