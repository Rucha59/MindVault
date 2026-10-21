package com.mindvault.domain.usecase

import com.mindvault.domain.model.Category
import com.mindvault.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> = repository.observeAll()
}