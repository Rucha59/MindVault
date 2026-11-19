package com.mindvault.domain.usecase

import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

/** False when the build has no Supabase keys; the UI then explains that accounts are unavailable. */
class IsAuthAvailableUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Boolean = repository.isAvailable
}