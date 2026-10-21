package com.mindvault.domain.usecase

import com.mindvault.domain.model.VaultEntry
import com.mindvault.domain.repository.CategoryRepository
import com.mindvault.domain.repository.KnowledgeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One saved item with its category name; emits null if the item doesn't exist (or is deleted). */
class ObserveKnowledgeDetailUseCase @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(itemId: String): Flow<VaultEntry?> =
        combine(knowledgeRepository.observeById(itemId), categoryRepository.observeAll()) { item, categories ->
            item?.let { found ->
                VaultEntry(found, categories.firstOrNull { it.id == found.categoryId }?.name)
            }
        }
}