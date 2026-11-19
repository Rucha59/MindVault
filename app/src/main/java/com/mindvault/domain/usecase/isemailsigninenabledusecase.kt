package com.mindvault.domain.usecase

import com.mindvault.domain.repository.AuthRepository
import javax.inject.Inject

class IsEmailSignInEnabledUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Boolean = repository.isAvailable && repository.isEmailSignInEnabled
}